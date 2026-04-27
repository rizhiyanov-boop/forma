package com.forma.app.domain.backup

import com.forma.app.domain.model.Program
import com.forma.app.domain.model.UserProfile
import com.forma.app.domain.model.WorkoutSession
import com.forma.app.domain.review.WorkoutReview
import kotlinx.serialization.Serializable

/**
 * Полный снимок состояния пользователя для экспорта/импорта.
 *
 * version — версия формата снапшота (НЕ версия БД). Если поменяется структура
 * полей — поднимаем version и в импорте делаем форк по версии.
 *
 * Это доменные модели, а не Room entities — так что снапшот не зависит от
 * изменений Room-схемы.
 */
@Serializable
data class BackupSnapshot(
    val version: Int = CURRENT_VERSION,
    val createdAt: Long,
    val appVersion: String? = null,
    val profile: UserProfile?,
    val program: Program?,
    val sessions: List<WorkoutSession>,
    val reviews: List<WorkoutReview>,
    val overrides: List<OverrideSnapshot>
) {
    companion object {
        /**
         * v1 — изначальный формат
         * v2 — добавлен `sex` в UserProfile. Старые v1 снапшоты импортируются нормально:
         *      Json игнорирует отсутствующее поле и берёт default `Sex.UNSPECIFIED`.
         */
        const val CURRENT_VERSION = 2
    }
}

/**
 * Override (перманентная замена упражнения) — сериализуемое представление.
 * Не используем доменное Exercise напрямую, потому что override привязан к workoutId
 * и хранит ровно те поля что лежат в Room ExerciseOverrideEntity.
 */
@Serializable
data class OverrideSnapshot(
    val id: String,
    val workoutId: String,
    val exerciseIdOrigin: String,
    val name: String,
    val description: String?,
    val primaryMuscle: String,
    val secondaryMuscles: List<String>,
    val equipment: List<String>,
    val targetSets: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val restSeconds: Int,
    val usesWeight: Boolean,
    val notes: String?,
    val startingWeightKg: Double?,
    val targetSetsDetailedJson: String?,
    val createdAt: Long
)
