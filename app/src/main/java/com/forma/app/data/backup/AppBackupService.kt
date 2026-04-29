package com.forma.app.data.backup

import android.content.Context
import com.forma.app.domain.model.Program
import com.forma.app.domain.model.UserProfile
import com.forma.app.domain.model.WorkoutSession
import com.forma.app.domain.repository.ProgramRepository
import com.forma.app.domain.repository.SessionRepository
import com.forma.app.domain.repository.UserProfileRepository
import com.forma.app.domain.review.ReviewRepository
import com.forma.app.domain.review.WorkoutReview
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Снимок пользовательских данных для восстановления после wipe БД.
 * Версия — для будущих миграций структуры самого снапшота.
 */
@Serializable
data class BackupSnapshot(
    val schemaVersion: Int = 1,
    val savedAt: Long,
    val profile: UserProfile?,
    val program: Program?,
    val recentSessions: List<WorkoutSession> = emptyList(),
    val recentReviews: List<WorkoutReview> = emptyList()
)

/**
 * Автобэкап. Сохраняет JSON-снимок в internal storage приложения.
 * Файл переживает обновления APK, но НЕ переживает "Очистить данные".
 */
@Singleton
class AppBackupService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepo: UserProfileRepository,
    private val programRepo: ProgramRepository,
    private val sessionRepo: SessionRepository,
    private val reviewRepo: ReviewRepository
) {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val backupFile: File
        get() = File(context.filesDir, BACKUP_FILENAME)

    /**
     * Сохранить текущее состояние в JSON.
     * Вызывается периодически: после старта, после завершения тренировки и т.п.
     */
    suspend fun save() {
        try {
            val profile = profileRepo.observeProfile().first()
            val program = programRepo.getActiveProgram()
            val sessions = sessionRepo.observeSessionsSince(0L).first()
                .sortedByDescending { it.startedAt }
                .take(100)  // храним до 100 последних сессий
            val reviews = reviewRepo.observeRecentReviews(50).first()

            val snapshot = BackupSnapshot(
                savedAt = System.currentTimeMillis(),
                profile = profile,
                program = program,
                recentSessions = sessions,
                recentReviews = reviews
            )

            val text = json.encodeToString(BackupSnapshot.serializer(), snapshot)
            backupFile.writeText(text)

            android.util.Log.d("Forma.Backup",
                "Saved: profile=${profile != null}, program=${program?.name}, " +
                "sessions=${sessions.size}, reviews=${reviews.size}, size=${text.length}b")
        } catch (t: Throwable) {
            android.util.Log.e("Forma.Backup", "Save failed", t)
        }
    }

    /**
     * Загрузить снапшот, если он есть. Не восстанавливает в БД сам — это делает
     * вызывающий код после проверки что БД пустая.
     */
    fun load(): BackupSnapshot? {
        val file = backupFile
        if (!file.exists() || file.length() == 0L) return null
        return try {
            val text = file.readText()
            json.decodeFromString(BackupSnapshot.serializer(), text)
        } catch (t: Throwable) {
            android.util.Log.e("Forma.Backup", "Load failed", t)
            null
        }
    }

    /**
     * Восстановить снапшот в БД. Делает только если БД пустая.
     * Возвращает true если восстановление произошло.
     */
    suspend fun restoreIfNeeded(): Boolean {
        val existing = profileRepo.observeProfile().first()
        if (existing != null) {
            android.util.Log.d("Forma.Backup", "Profile already exists — no restore needed")
            return false
        }

        val snapshot = load() ?: run {
            android.util.Log.d("Forma.Backup", "No backup file found")
            return false
        }

        try {
            android.util.Log.d("Forma.Backup",
                "Restoring snapshot from ${java.util.Date(snapshot.savedAt)}: " +
                "profile=${snapshot.profile != null}, program=${snapshot.program?.name}, " +
                "sessions=${snapshot.recentSessions.size}")

            snapshot.profile?.let { profileRepo.saveProfile(it) }
            snapshot.program?.let { programRepo.saveAsActive(it) }
            snapshot.recentSessions.forEach { sessionRepo.saveSession(it) }
            // Reviews пока не восстанавливаем — сохранение review требует знаний
            // о Recommendation deserialization, добавим позже если понадобится.

            return true
        } catch (t: Throwable) {
            android.util.Log.e("Forma.Backup", "Restore failed", t)
            return false
        }
    }

    companion object {
        private const val BACKUP_FILENAME = "forma_backup.json"
    }
}
