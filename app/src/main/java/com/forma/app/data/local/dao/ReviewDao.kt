package com.forma.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.forma.app.data.local.entity.ReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReviewEntity)

    @Update
    suspend fun update(entity: ReviewEntity)

    @Query("SELECT * FROM workout_reviews WHERE id = :id")
    fun observe(id: String): Flow<ReviewEntity?>

    @Query("SELECT * FROM workout_reviews WHERE id = :id")
    suspend fun get(id: String): ReviewEntity?

    @Query("SELECT * FROM workout_reviews WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getBySessionId(sessionId: String): ReviewEntity?

    @Query("SELECT * FROM workout_reviews ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM workout_reviews ORDER BY createdAt ASC")
    suspend fun all(): List<ReviewEntity>

    @Query("DELETE FROM workout_reviews")
    suspend fun deleteAll()
}
