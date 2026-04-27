package com.forma.app.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.forma.app.data.local.entity.ExerciseEntity
import com.forma.app.data.local.entity.ProgramEntity
import com.forma.app.data.local.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow

data class WorkoutWithExercises(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutId"
    )
    val exercises: List<ExerciseEntity>
)

data class ProgramWithWorkouts(
    @Embedded val program: ProgramEntity,
    @Relation(
        entity = WorkoutEntity::class,
        parentColumn = "id",
        entityColumn = "programId"
    )
    val workouts: List<WorkoutWithExercises>
)

@Dao
interface ProgramDao {

    @Transaction
    @Query("SELECT * FROM programs WHERE isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    fun observeActive(): Flow<ProgramWithWorkouts?>

    @Transaction
    @Query("SELECT * FROM programs WHERE id = :id")
    suspend fun getById(id: String): ProgramWithWorkouts?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgram(program: ProgramEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkouts(workouts: List<WorkoutEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Query("UPDATE programs SET isActive = 0")
    suspend fun deactivateAll()

    @Transaction
    suspend fun replaceActive(
        program: ProgramEntity,
        workouts: List<WorkoutEntity>,
        exercises: List<ExerciseEntity>
    ) {
        deactivateAll()
        insertProgram(program.copy(isActive = true))
        insertWorkouts(workouts)
        insertExercises(exercises)
    }
}
