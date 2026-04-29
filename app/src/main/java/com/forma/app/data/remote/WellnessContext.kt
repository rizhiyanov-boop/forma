package com.forma.app.data.remote

/**
 * Контекст самочувствия для AI-разбора после тренировки.
 * Все поля опциональны, потому что пользователь может пропустить часть анкет.
 */
data class WellnessContext(
    val preWorkoutEnergy: String?,
    val postWorkoutEnergy: String?,
    val postWorkoutSleep: Int?,
    val postWorkoutStress: Int?,
    val postWorkoutMood: Int?,
    val sevenDayAvgEnergy: Double?,
    val recentLowEnergyStreak: Int
)
