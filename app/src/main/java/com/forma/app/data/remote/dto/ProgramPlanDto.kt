package com.forma.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Формат ответа AI для генерации недельной программы тренировок.
 * Соответствует JSON-схеме из ProgramSchemas.kt.
 */
@Serializable
data class ProgramPlanDto(
    val programName: String,
    val programDescription: String,
    val workouts: List<WorkoutDto>
)

@Serializable
data class WorkoutDto(
    val dayOfWeek: String,        // "MON"|"TUE"|...|"SUN"
    val title: String,
    val focus: String,
    val estimatedMinutes: Int,
    val exercises: List<ExerciseDto>
)

@Serializable
data class ExerciseDto(
    val name: String,
    val description: String,
    val primaryMuscle: String,    // MuscleGroup.name
    val secondaryMuscles: List<String>,
    val equipment: List<String>,  // Equipment.name
    val targetSets: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val restSeconds: Int,
    val usesWeight: Boolean,
    val notes: String
)
