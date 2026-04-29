package com.forma.app.domain.wellness

import com.forma.app.data.local.dao.WellnessDao
import com.forma.app.data.local.entity.WellnessEntity
import com.forma.app.data.wellness.WellnessRepositoryImpl
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

class WellnessRepositoryTest {

    @Test
    fun save_andGetPreWorkout_returnsEntry() = runBlocking {
        val dao = FakeWellnessDao()
        val repo = WellnessRepositoryImpl(dao)
        val entry = WellnessEntry(
            sessionId = "s1",
            timestamp = System.currentTimeMillis(),
            type = WellnessTriggerType.PRE_WORKOUT,
            energy = EnergyLevel.NORMAL
        )

        repo.save(entry)
        val loaded = repo.getPreWorkoutFor("s1")

        assertThat(loaded).isNotNull()
        assertThat(loaded?.energy).isEqualTo(EnergyLevel.NORMAL)
    }

    @Test
    fun getPreWorkout_returnsNull_whenNoEntry() = runBlocking {
        val repo = WellnessRepositoryImpl(FakeWellnessDao())
        assertThat(repo.getPreWorkoutFor("missing")).isNull()
    }

    @Test
    fun avgEnergyLast_returnsAverage() = runBlocking {
        val dao = FakeWellnessDao()
        val repo = WellnessRepositoryImpl(dao)
        val now = System.currentTimeMillis()
        repo.save(WellnessEntry(sessionId = "s1", timestamp = now, type = WellnessTriggerType.PRE_WORKOUT, energy = EnergyLevel.FATIGUED))
        repo.save(WellnessEntry(sessionId = "s2", timestamp = now, type = WellnessTriggerType.PRE_WORKOUT, energy = EnergyLevel.NORMAL))
        repo.save(WellnessEntry(sessionId = "s3", timestamp = now, type = WellnessTriggerType.PRE_WORKOUT, energy = EnergyLevel.ENERGIZED))

        val avg = repo.avgEnergyLast(7)

        assertThat(avg).isEqualTo(2.0)
    }

    @Test
    fun observeRecent_filtersByDate() = runBlocking {
        val dao = FakeWellnessDao()
        val repo = WellnessRepositoryImpl(dao)
        val now = System.currentTimeMillis()
        val old = now - 20L * 24L * 60L * 60L * 1000L

        dao.upsert(
            WellnessEntity(
                id = "old",
                sessionId = "x",
                timestamp = old,
                type = WellnessTriggerType.PRE_WORKOUT.name,
                energy = EnergyLevel.NORMAL.name,
                sleepQuality = null,
                stressLevel = null,
                mood = null,
                notes = null
            )
        )
        dao.upsert(
            WellnessEntity(
                id = "new",
                sessionId = "y",
                timestamp = now,
                type = WellnessTriggerType.PRE_WORKOUT.name,
                energy = EnergyLevel.NORMAL.name,
                sleepQuality = null,
                stressLevel = null,
                mood = null,
                notes = null
            )
        )

        val recent = repo.observeRecent(14).first()

        assertThat(recent.map { it.id }).containsExactly("new")
        Unit
    }

    private class FakeWellnessDao : WellnessDao {
        private val items = mutableListOf<WellnessEntity>()
        private val flow = MutableStateFlow<List<WellnessEntity>>(emptyList())

        override suspend fun upsert(entity: WellnessEntity) {
            items.removeAll { it.id == entity.id }
            items.add(entity)
            flow.value = items.sortedByDescending { it.timestamp }
        }

        override suspend fun findBySessionAndType(sessionId: String, type: String): WellnessEntity? {
            return items.firstOrNull { it.sessionId == sessionId && it.type == type }
        }

        override suspend fun listBySessionAndType(sessionId: String, type: String): List<WellnessEntity> {
            return items
                .filter { it.sessionId == sessionId && it.type == type }
                .sortedByDescending { it.timestamp }
        }

        override fun observeRecent(since: Long): Flow<List<WellnessEntity>> {
            return MutableStateFlow(
                items.filter { it.timestamp >= since }.sortedByDescending { it.timestamp }
            )
        }

        override suspend fun listSince(since: Long): List<WellnessEntity> {
            return items.filter { it.timestamp >= since }
        }

        override suspend fun all(): List<WellnessEntity> = items.toList()

        override suspend fun deleteAll() {
            items.clear()
            flow.value = emptyList()
        }
    }
}
