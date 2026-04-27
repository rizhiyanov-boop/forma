package com.forma.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.forma.app.domain.model.DayOfWeek

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val programId: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val dayOfWeek: DayOfWeek,
    val totalVolumeKg: Double = 0.0,
    val completedSetsCount: Int = 0,
    val totalSetsCount: Int = 0,
    val isDirty: Boolean = true,
    val syncedAt: Long? = null
)

@Entity(
    tableName = "exercise_logs",
    foreignKeys = [ForeignKey(
        entity = WorkoutSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class ExerciseLogEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val exerciseId: String,
    val exerciseName: String,
    val notes: String?,
    val orderIndex: Int = 0
)

@Entity(
    tableName = "set_logs",
    foreignKeys = [ForeignKey(
        entity = ExerciseLogEntity::class,
        parentColumns = ["id"],
        childColumns = ["exerciseLogId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("exerciseLogId")]
)
data class SetLogEntity(
    @PrimaryKey val id: String,
    val exerciseLogId: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double?,
    val rpe: Int?,
    val rir: Int? = null,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val isOptional: Boolean = false,
    val targetWeightKg: Double? = null,
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null
)
