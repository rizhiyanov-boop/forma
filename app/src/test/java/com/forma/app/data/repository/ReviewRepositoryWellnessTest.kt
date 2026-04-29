package com.forma.app.data.repository

import com.forma.app.domain.wellness.EnergyLevel
import com.forma.app.domain.wellness.WellnessEntry
import com.forma.app.domain.wellness.WellnessRepository
import com.forma.app.domain.wellness.WellnessTriggerType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ReviewRepositoryWellnessTest {

    @Test
    fun build_returnsNull_whenNoWellnessData() = runBlocking {
        val builder = WellnessContextBuilder(
            wellnessRepo = FakeWellnessRepository(),
            nowMillis = { NOW }
        )

        val context = builder.build(sessionId = "s1")

        assertThat(context).isNull()
    }

    @Test
    fun build_collectsPrePostAndAverageContext() = runBlocking {
        val repo = FakeWellnessRepository(
            entries = listOf(
                WellnessEntry(
                    sessionId = "s1",
                    timestamp = NOW - 10_000L,
                    type = WellnessTriggerType.PRE_WORKOUT,
                    energy = EnergyLevel.FATIGUED
                ),
                WellnessEntry(
                    sessionId = "s1",
                    timestamp = NOW - 1_000L,
                    type = WellnessTriggerType.POST_WORKOUT,
                    energy = EnergyLevel.NORMAL,
                    sleepQuality = 2,
                    stressLevel = 4,
                    mood = 3
                )
            )
        )
        val builder = WellnessContextBuilder(repo, nowMillis = { NOW })

        val context = builder.build(sessionId = "s1")

        assertThat(context).isNotNull()
        assertThat(context?.preWorkoutEnergy).isEqualTo("Уставший")
        assertThat(context?.postWorkoutEnergy).isEqualTo("Норма")
        assertThat(context?.postWorkoutSleep).isEqualTo(2)
        assertThat(context?.postWorkoutStress).isEqualTo(4)
        assertThat(context?.postWorkoutMood).isEqualTo(3)
        assertThat(context?.sevenDayAvgEnergy).isEqualTo(1.5)
        assertThat(context?.recentLowEnergyStreak).isEqualTo(0)
    }

    @Test
    fun computeLowEnergyStreak_countsConsecutiveFatiguedDays() = runBlocking {
        val repo = FakeWellnessRepository(
            entries = listOf(
                dayEntry(daysAgo = 0, energy = EnergyLevel.FATIGUED),
                dayEntry(daysAgo = 1, energy = EnergyLevel.FATIGUED),
                dayEntry(daysAgo = 2, energy = EnergyLevel.FATIGUED),
                dayEntry(daysAgo = 3, energy = EnergyLevel.NORMAL)
            )
        )
        val builder = WellnessContextBuilder(repo, nowMillis = { NOW })

        val streak = builder.computeLowEnergyStreak(daysBack = 7)

        assertThat(streak).isEqualTo(3)
    }

    private class FakeWellnessRepository(
        private val entries: List<WellnessEntry> = emptyList()
    ) : WellnessRepository {
        override suspend fun save(entry: WellnessEntry) = Unit

        override suspend fun getPreWorkoutFor(sessionId: String): WellnessEntry? =
            entries.firstOrNull {
                it.sessionId == sessionId && it.type == WellnessTriggerType.PRE_WORKOUT
            }

        override suspend fun getPostWorkoutFor(sessionId: String): WellnessEntry? =
            entries.firstOrNull {
                it.sessionId == sessionId && it.type == WellnessTriggerType.POST_WORKOUT
            }

        override suspend fun getBetweenExercisesFor(sessionId: String): List<WellnessEntry> =
            entries
                .filter {
                    it.sessionId == sessionId && it.type == WellnessTriggerType.BETWEEN_EXERCISES
                }
                .sortedByDescending { it.timestamp }

        override fun observeRecent(daysBack: Int): Flow<List<WellnessEntry>> =
            MutableStateFlow(entries.sortedByDescending { it.timestamp })

        override suspend fun avgEnergyLast(daysBack: Int): Double? {
            val values = entries.mapNotNull { entry ->
                when (entry.energy) {
                    EnergyLevel.FATIGUED -> 1.0
                    EnergyLevel.NORMAL -> 2.0
                    EnergyLevel.ENERGIZED -> 3.0
                    null -> null
                }
            }
            return values.takeIf { it.isNotEmpty() }?.average()
        }
    }

    private companion object {
        const val DAY_MS = 24L * 60L * 60L * 1000L
        const val NOW = 1_800_000_000_000L

        fun dayEntry(daysAgo: Int, energy: EnergyLevel): WellnessEntry {
            val timestamp = NOW - (daysAgo * DAY_MS) - 1_000L
            return WellnessEntry(
                sessionId = "s1",
                timestamp = timestamp,
                type = WellnessTriggerType.DAILY,
                energy = energy
            )
        }
    }
}
