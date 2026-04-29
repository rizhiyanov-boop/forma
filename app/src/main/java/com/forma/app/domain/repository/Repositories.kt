package com.forma.app.domain.repository

import com.forma.app.domain.model.Exercise
import com.forma.app.domain.model.Program
import com.forma.app.domain.model.UserProfile
import com.forma.app.domain.model.WorkoutSession
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun observeProfile(): Flow<UserProfile?>
    suspend fun getProfile(): UserProfile?
    suspend fun saveProfile(profile: UserProfile)
    suspend fun clear()
}

interface ProgramRepository {
    fun observeActiveProgram(): Flow<Program?>
    suspend fun getActiveProgram(): Program?
    suspend fun saveAsActive(program: Program)
    /**
     * Генерирует программу через AI и сохраняет как активную.
     */
    suspend fun generateAndSave(profile: UserProfile): Program

    /**
     * Запросить у AI 2-3 варианта замены упражнения.
     */
    suspend fun suggestReplacements(
        workoutId: String,
        exerciseId: String,
        reason: String?
    ): List<Exercise>

    /**
     * Перманентная замена упражнения в активной программе.
     * Меняет упражнение во ВСЕХ будущих повторениях этой тренировки.
     */
    suspend fun replaceExercisePermanently(
        workoutId: String,
        exerciseIdOrigin: String,
        replacement: Exercise
    )

    /**
     * Снять перманентную замену — вернуть оригинальное упражнение.
     */
    suspend fun clearOverride(workoutId: String, exerciseIdOrigin: String)

    /**
     * Обновить параметры упражнения (вес/повторы/отдых) — для применения рекомендаций AI.
     * Внутри использует механизм override: сохраняет оригинал, патчит указанные поля.
     */
    suspend fun updateExerciseFields(
        exerciseId: String,
        newWeightKg: Double? = null,
        newRepsMin: Int? = null,
        newRepsMax: Int? = null,
        newRestSeconds: Int? = null
    )

    /**
     * Загрузить готовую (preset) программу — без вызова AI.
     */
    suspend fun loadPresetProgram(): Program
}

interface SessionRepository {
    suspend fun createSession(workoutId: String, programId: String): WorkoutSession
    suspend fun saveSession(session: WorkoutSession)
    suspend fun updateSet(
        sessionId: String,
        setId: String,
        reps: Int,
        weightKg: Double?,
        rir: Int?,
        completed: Boolean
    )
    fun observeSession(id: String): Flow<WorkoutSession?>
    suspend fun getSession(id: String): WorkoutSession?
    suspend fun finishSession(id: String)
    fun observeSessionsSince(fromTs: Long): Flow<List<WorkoutSession>>
    suspend fun lastSessionForWorkout(workoutId: String): WorkoutSession?

    /**
     * Последние N завершённых сессий этого же workout, кроме указанной.
     * Используется для AI-разбора как контекст сравнения.
     */
    suspend fun recentSessionsForWorkout(
        workoutId: String,
        excludeId: String,
        limit: Int = 4
    ): List<WorkoutSession>

    /**
     * Разовая замена упражнения в АКТИВНОЙ сессии.
     * Не трогает программу, не создаёт override — меняет только ExerciseLog в этой сессии.
     */
    suspend fun replaceExerciseInSession(
        sessionId: String,
        exerciseLogId: String,
        replacement: Exercise
    )
}
