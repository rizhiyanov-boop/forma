package com.forma.app.data.wellness

import com.forma.app.data.local.dao.WellnessDao
import com.forma.app.data.local.entity.WellnessEntity
import com.forma.app.domain.wellness.EnergyLevel
import com.forma.app.domain.wellness.WellnessEntry
import com.forma.app.domain.wellness.WellnessRepository
import com.forma.app.domain.wellness.WellnessTriggerType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class WellnessRepositoryImpl @Inject constructor(
    private val dao: WellnessDao
) : WellnessRepository {

    override suspend fun save(entry: WellnessEntry) {
        dao.upsert(entry.toEntity())
    }

    override suspend fun getPreWorkoutFor(sessionId: String): WellnessEntry? {
        return dao.findBySessionAndType(sessionId, WellnessTriggerType.PRE_WORKOUT.name)?.toDomain()
    }

    override suspend fun getPostWorkoutFor(sessionId: String): WellnessEntry? {
        return dao.findBySessionAndType(sessionId, WellnessTriggerType.POST_WORKOUT.name)?.toDomain()
    }

    override suspend fun getBetweenExercisesFor(sessionId: String): List<WellnessEntry> {
        return dao.listBySessionAndType(sessionId, WellnessTriggerType.BETWEEN_EXERCISES.name)
            .map { it.toDomain() }
    }

    override fun observeRecent(daysBack: Int): Flow<List<WellnessEntry>> {
        val since = System.currentTimeMillis() - daysBack * DAY_MS
        return dao.observeRecent(since).map { rows -> rows.map { it.toDomain() } }
    }

    override suspend fun avgEnergyLast(daysBack: Int): Double? {
        val since = System.currentTimeMillis() - daysBack * DAY_MS
        val entries = dao.listSince(since).filter { it.energy != null }
        if (entries.isEmpty()) return null
        return entries.map { row ->
            when (EnergyLevel.valueOf(row.energy!!)) {
                EnergyLevel.FATIGUED -> 1.0
                EnergyLevel.NORMAL -> 2.0
                EnergyLevel.ENERGIZED -> 3.0
            }
        }.average()
    }

    private fun WellnessEntry.toEntity(): WellnessEntity = WellnessEntity(
        id = id,
        sessionId = sessionId,
        timestamp = timestamp,
        type = type.name,
        energy = energy?.name,
        sleepQuality = sleepQuality,
        stressLevel = stressLevel,
        mood = mood,
        notes = notes
    )

    private fun WellnessEntity.toDomain(): WellnessEntry = WellnessEntry(
        id = id,
        sessionId = sessionId,
        timestamp = timestamp,
        type = WellnessTriggerType.valueOf(type),
        energy = energy?.let { EnergyLevel.valueOf(it) },
        sleepQuality = sleepQuality,
        stressLevel = stressLevel,
        mood = mood,
        notes = notes
    )

    private companion object {
        const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
