package com.forma.app.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.forma.app.data.local.entity.ExerciseLogEntity
import com.forma.app.data.local.entity.SetLogEntity
import com.forma.app.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

data class ExerciseLogWithSets(
    @Embedded val log: ExerciseLogEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseLogId"
    )
    val sets: List<SetLogEntity>
)

data class SessionWithLogs(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        entity = ExerciseLogEntity::class,
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val exerciseLogs: List<ExerciseLogWithSets>
)

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity)

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseLogs(logs: List<ExerciseLogEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetLogs(sets: List<SetLogEntity>)

    @Update
    suspend fun updateSet(set: SetLogEntity)

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun observeSession(id: String): Flow<SessionWithLogs?>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSession(id: String): SessionWithLogs?

    @Transaction
    @Query(
        """
        SELECT * FROM workout_sessions
        WHERE startedAt >= :fromTs
        ORDER BY startedAt DESC
        """
    )
    fun observeSessionsSince(fromTs: Long): Flow<List<SessionWithLogs>>

    @Transaction
    @Query(
        """
        SELECT * FROM workout_sessions
        WHERE workoutId = :workoutId
        ORDER BY startedAt DESC LIMIT 1
        """
    )
    suspend fun lastSessionForWorkout(workoutId: String): SessionWithLogs?

    @Transaction
    @Query(
        """
        SELECT * FROM workout_sessions
        WHERE workoutId = :workoutId AND id != :excludeId AND finishedAt IS NOT NULL
        ORDER BY startedAt DESC LIMIT :limit
        """
    )
    suspend fun recentSessionsForWorkout(
        workoutId: String,
        excludeId: String,
        limit: Int
    ): List<SessionWithLogs>

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY startedAt ASC")
    suspend fun allSessions(): List<SessionWithLogs>

    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAllSessions()
}
