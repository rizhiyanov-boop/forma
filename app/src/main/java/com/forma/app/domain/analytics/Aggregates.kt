package com.forma.app.domain.analytics

import com.forma.app.domain.model.MuscleGroup

/**
 * Агрегат за одну календарную неделю.
 * weekStart — timestamp понедельника 00:00 локального времени.
 */
data class WeeklyAggregate(
    val weekStart: Long,
    val weekLabel: String,           // "21 апр", "14 апр" — для оси
    val totalVolumeKg: Double,
    val totalSetsCompleted: Int,
    val totalSetsPlanned: Int,
    val sessionsCount: Int,
    val avgRir: Double?,             // null если RIR не вводили
    val completionRatio: Double      // 0..1, доля выполненных подходов от запланированных
)

/**
 * Прогресс по конкретному упражнению — точка для графика весов.
 */
data class ExerciseProgressPoint(
    val timestamp: Long,             // дата сессии
    val workingWeightKg: Double,     // максимальный вес рабочих подходов
    val totalRepsAtMaxWeight: Int    // сколько повторов выжал на максимальном весе
)

/**
 * Прогресс по упражнению через все сессии.
 * exerciseName — нормализованное имя (одинаковое для всех его повторений).
 */
data class ExerciseProgressSeries(
    val exerciseName: String,
    val primaryMuscle: MuscleGroup,
    val points: List<ExerciseProgressPoint>
)

/**
 * Объём за период по конкретной группе мышц.
 */
data class MuscleGroupVolume(
    val muscle: MuscleGroup,
    val volumeKg: Double,
    val setsCount: Int
)

/**
 * Полный набор агрегатов для экрана графиков.
 */
data class ProgressData(
    val weeks: List<WeeklyAggregate>,                    // последние 8-12 недель
    val keyExercises: List<ExerciseProgressSeries>,      // топ-N по частоте
    val muscleVolume: List<MuscleGroupVolume>,           // распределение за выбранный период
    val isEmpty: Boolean = weeks.isEmpty()
)
