package com.forma.app.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Запись одного подхода, заполняется пользователем.
 */
@Serializable
data class SetLog(
    val id: String = UUID.randomUUID().toString(),
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double?,            // null для упражнений без веса
    val rpe: Int? = null,             // rate of perceived exertion, опционально 1-10
    val rir: Int? = null,             // reps in reserve — сколько ещё мог бы сделать
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val isOptional: Boolean = false,  // помечен ли как опциональный (можно пропустить)
    val targetWeightKg: Double? = null,    // целевой вес из программы (для подсказки)
    val targetRepsMin: Int? = null,        // целевые повторы — нижняя граница
    val targetRepsMax: Int? = null         // целевые повторы — верхняя граница
)

/**
 * Лог одного упражнения в рамках сессии.
 */
@Serializable
data class ExerciseLog(
    val id: String = UUID.randomUUID().toString(),
    val exerciseId: String,
    val exerciseName: String,
    val sets: List<SetLog>,
    val notes: String? = null
)

/**
 * Сессия — одна фактически проведённая тренировка.
 */
@Serializable
data class WorkoutSession(
    val id: String = UUID.randomUUID().toString(),
    val workoutId: String,
    val programId: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val dayOfWeek: DayOfWeek,
    val exerciseLogs: List<ExerciseLog>,
    val totalVolumeKg: Double = 0.0,
    val completedSetsCount: Int = 0,
    val totalSetsCount: Int = 0
) {
    val isFinished: Boolean get() = finishedAt != null
    val completionRatio: Float
        get() = if (totalSetsCount == 0) 0f else completedSetsCount.toFloat() / totalSetsCount
}
