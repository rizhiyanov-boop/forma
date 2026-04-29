package com.forma.app.domain.wellness

import kotlinx.coroutines.flow.Flow

interface WellnessRepository {
    suspend fun save(entry: WellnessEntry)
    suspend fun getPreWorkoutFor(sessionId: String): WellnessEntry?
    suspend fun getPostWorkoutFor(sessionId: String): WellnessEntry?
    suspend fun getBetweenExercisesFor(sessionId: String): List<WellnessEntry>
    fun observeRecent(daysBack: Int = 14): Flow<List<WellnessEntry>>
    suspend fun avgEnergyLast(daysBack: Int = 7): Double?
}
