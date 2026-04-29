package com.forma.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.forma.app.data.local.entity.WellnessEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WellnessDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WellnessEntity)

    @Query("SELECT * FROM wellness_log WHERE sessionId = :sessionId AND type = :type LIMIT 1")
    suspend fun findBySessionAndType(sessionId: String, type: String): WellnessEntity?

    @Query("""
        SELECT * FROM wellness_log
        WHERE sessionId = :sessionId AND type = :type
        ORDER BY timestamp DESC
    """)
    suspend fun listBySessionAndType(sessionId: String, type: String): List<WellnessEntity>

    @Query("SELECT * FROM wellness_log WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun observeRecent(since: Long): Flow<List<WellnessEntity>>

    @Query("SELECT * FROM wellness_log WHERE timestamp >= :since")
    suspend fun listSince(since: Long): List<WellnessEntity>

    @Query("SELECT * FROM wellness_log")
    suspend fun all(): List<WellnessEntity>

    @Query("DELETE FROM wellness_log")
    suspend fun deleteAll()
}
