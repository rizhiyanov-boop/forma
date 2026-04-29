package com.forma.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.forma.app.domain.model.DayOfWeek
import com.forma.app.domain.model.Equipment
import com.forma.app.domain.model.MuscleGroup

@Entity(tableName = "programs")
data class ProgramEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val weekNumber: Int,
    val profileSnapshotJson: String,    // сериализованный UserProfile
    val createdAt: Long,
    val isActive: Boolean = true,
    // Sync-поля для будущего бэкенда
    val isDirty: Boolean = true,
    val syncedAt: Long? = null
)

@Entity(
    tableName = "workouts",
    foreignKeys = [ForeignKey(
        entity = ProgramEntity::class,
        parentColumns = ["id"],
        childColumns = ["programId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("programId")]
)
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val programId: String,
    val dayOfWeek: DayOfWeek,
    val title: String,
    val focus: String,
    val estimatedMinutes: Int,
    val orderIndex: Int = 0
)

@Entity(
    tableName = "exercises",
    foreignKeys = [ForeignKey(
        entity = WorkoutEntity::class,
        parentColumns = ["id"],
        childColumns = ["workoutId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("workoutId")]
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val contentId: String? = null,
    val name: String,
    val description: String?,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: List<MuscleGroup>,
    val equipment: List<Equipment>,
    val targetSets: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val restSeconds: Int,
    val usesWeight: Boolean,
    val notes: String?,
    val orderIndex: Int,
    val startingWeightKg: Double? = null,
    /** JSON-сериализованный List<TargetSet> или null. */
    val targetSetsDetailedJson: String? = null
)
