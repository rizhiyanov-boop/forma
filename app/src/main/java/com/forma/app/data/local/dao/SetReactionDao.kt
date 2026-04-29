package com.forma.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.forma.app.data.local.entity.SetReactionEntity

@Dao
interface SetReactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SetReactionEntity)

    @Query("SELECT * FROM set_reactions WHERE setLogId = :setLogId LIMIT 1")
    suspend fun findBySetId(setLogId: String): SetReactionEntity?

    @Query("SELECT COUNT(*) FROM set_reactions WHERE sessionId = :sessionId AND exerciseId = :exerciseId AND userAcceptedSuggestion = 0")
    suspend fun declinedCountForExercise(sessionId: String, exerciseId: String): Int

    @Query("SELECT * FROM set_reactions WHERE sessionId = :sessionId AND exerciseId = :exerciseId")
    suspend fun findByExerciseInSession(sessionId: String, exerciseId: String): List<SetReactionEntity>

    @Query("UPDATE set_reactions SET userAcceptedSuggestion = :accepted WHERE setLogId = :setLogId")
    suspend fun markAccepted(setLogId: String, accepted: Boolean)
}
