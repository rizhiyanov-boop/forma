package com.forma.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.forma.app.data.local.entity.ExerciseOverrideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseOverrideDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ExerciseOverrideEntity)

    @Query("SELECT * FROM exercise_overrides WHERE workoutId = :workoutId")
    fun observeForWorkout(workoutId: String): Flow<List<ExerciseOverrideEntity>>

    @Query("SELECT * FROM exercise_overrides WHERE workoutId = :workoutId")
    suspend fun getForWorkout(workoutId: String): List<ExerciseOverrideEntity>

    @Query("DELETE FROM exercise_overrides WHERE workoutId = :workoutId AND exerciseIdOrigin = :originId")
    suspend fun deleteOverride(workoutId: String, originId: String)

    @Query("SELECT * FROM exercise_overrides")
    suspend fun all(): List<ExerciseOverrideEntity>

    @Query("DELETE FROM exercise_overrides")
    suspend fun deleteAll()
}
