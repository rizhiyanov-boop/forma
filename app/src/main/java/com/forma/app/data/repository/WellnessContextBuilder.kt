package com.forma.app.data.repository

import com.forma.app.data.remote.WellnessContext
import com.forma.app.domain.wellness.EnergyLevel
import com.forma.app.domain.wellness.WellnessRepository
import kotlinx.coroutines.flow.first

internal class WellnessContextBuilder(
    private val wellnessRepo: WellnessRepository,
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun build(sessionId: String): WellnessContext? {
        val pre = wellnessRepo.getPreWorkoutFor(sessionId)
        val post = wellnessRepo.getPostWorkoutFor(sessionId)
        val avg7d = wellnessRepo.avgEnergyLast(7)
        val streak = computeLowEnergyStreak(daysBack = 7)

        if (pre == null && post == null && avg7d == null) return null

        return WellnessContext(
            preWorkoutEnergy = pre?.energy?.displayName,
            postWorkoutEnergy = post?.energy?.displayName,
            postWorkoutSleep = post?.sleepQuality,
            postWorkoutStress = post?.stressLevel,
            postWorkoutMood = post?.mood,
            sevenDayAvgEnergy = avg7d,
            recentLowEnergyStreak = streak
        )
    }

    suspend fun computeLowEnergyStreak(daysBack: Int): Int {
        val entries = wellnessRepo.observeRecent(daysBack).first()
            .filter { it.energy != null }
            .sortedByDescending { it.timestamp }
        var streak = 0
        val today = nowMillis()

        for (i in 0 until daysBack) {
            val dayStart = today - (i + 1) * DAY_MS
            val dayEnd = today - i * DAY_MS
            val entryForDay = entries.firstOrNull { it.timestamp in dayStart..dayEnd } ?: break
            if (entryForDay.energy == EnergyLevel.FATIGUED) {
                streak += 1
            } else {
                break
            }
        }
        return streak
    }

    private companion object {
        const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
