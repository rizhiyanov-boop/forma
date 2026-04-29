package com.forma.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.forma.app.domain.model.Equipment
import com.forma.app.domain.model.MuscleGroup

/**
 * Перманентная замена упражнения внутри тренировки.
 * Строка существует — значит на этом workoutId оригинальное упражнение
 * exerciseIdOrigin заменено на параметры из этого override-а.
 *
 * Для разовых замен это не используется — там просто меняется ExerciseLog
 * в конкретной WorkoutSession.
 */
@Entity(
    tableName = "exercise_overrides",
    foreignKeys = [ForeignKey(
        entity = WorkoutEntity::class,
        parentColumns = ["id"],
        childColumns = ["workoutId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("workoutId"), Index("exerciseIdOrigin")]
)
data class ExerciseOverrideEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val exerciseIdOrigin: String,
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
    val startingWeightKg: Double? = null,
    val targetSetsDetailedJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = true,
    val syncedAt: Long? = null
)
