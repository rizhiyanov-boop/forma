package com.forma.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.forma.app.data.local.entity.CoachContentHistoryEntity

@Dao
interface CoachContentHistoryDao {
    @Insert
    suspend fun insert(entity: CoachContentHistoryEntity)

    @Query("SELECT contentItemId FROM coach_content_history WHERE shownAt >= :since")
    suspend fun shownSince(since: Long): List<String>

    @Query("DELETE FROM coach_content_history")
    suspend fun deleteAll()
}
