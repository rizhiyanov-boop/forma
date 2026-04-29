package com.forma.app.data.repository

import com.forma.app.data.local.dao.ExerciseOverrideDao
import com.forma.app.data.local.dao.ProgramDao
import com.forma.app.data.local.dao.ProgramWithWorkouts
import com.forma.app.data.local.entity.ExerciseOverrideEntity
import com.forma.app.data.mapper.Mappers
import com.forma.app.data.remote.OpenAiService
import com.forma.app.data.remote.dto.ExerciseDto
import com.forma.app.domain.model.Equipment
import com.forma.app.domain.model.Exercise
import com.forma.app.domain.model.MuscleGroup
import com.forma.app.domain.model.Program
import com.forma.app.domain.model.UserProfile
import com.forma.app.domain.model.Workout
import com.forma.app.domain.repository.ProgramRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgramRepositoryImpl @Inject constructor(
    private val dao: ProgramDao,
    private val overrideDao: ExerciseOverrideDao,
    private val openAi: OpenAiService
) : ProgramRepository {

    /**
     * Активная программа со склеенными overrides — там где оригинал заменён,
     * подставляем переопределение.
     */
    override fun observeActiveProgram(): Flow<Program?> =
        dao.observeActive().map { raw ->
            raw?.let {
                val program = with(Mappers) { it.toDomain() }
                applyOverrides(program)
            }
        }

    private suspend fun applyOverrides(program: Program): Program {
        val workouts = program.workouts.map { w ->
            val overrides = overrideDao.getForWorkout(w.id)
            if (overrides.isEmpty()) w else applyOverridesTo(w, overrides)
        }
        return program.copy(workouts = workouts)
    }

    private fun applyOverridesTo(
        workout: Workout,
        overrides: List<ExerciseOverrideEntity>
    ): Workout {
        val byOrigin = overrides.associateBy { it.exerciseIdOrigin }
        val newExercises = workout.exercises.map { ex ->
            val ov = byOrigin[ex.id] ?: return@map ex
            Exercise(
                id = ex.id,                  // сохраняем тот же id для статистики
                name = ov.name,
                description = ov.description,
                primaryMuscle = ov.primaryMuscle,
                secondaryMuscles = ov.secondaryMuscles,
                equipment = ov.equipment,
                targetSets = ov.targetSets,
                targetRepsMin = ov.targetRepsMin,
                targetRepsMax = ov.targetRepsMax,
                restSeconds = ov.restSeconds,
                usesWeight = ov.usesWeight,
                notes = ov.notes,
                orderIndex = ex.orderIndex
            )
        }
        return workout.copy(exercises = newExercises)
    }

    override suspend fun getActiveProgram(): Program? {
        val raw = dao.observeActive().firstOrNull() ?: return null
        val program = with(Mappers) { raw.toDomain() }
        return applyOverrides(program)
    }

    override suspend fun saveAsActive(program: Program) {
        val (p, w, e) = with(Mappers) { program.toEntities() }
        dao.replaceActive(p, w, e)
    }

    override suspend fun generateAndSave(profile: UserProfile): Program {
        val plan = openAi.generateProgram(profile)
        val program = with(Mappers) { plan.toDomain(profile) }
        saveAsActive(program)
        return program
    }

    override suspend fun suggestReplacements(
        workoutId: String,
        exerciseId: String,
        reason: String?
    ): List<Exercise> {
        val active = dao.observeActive().firstOrNull()
            ?: error("Нет активной программы")

        val program = with(Mappers) { active.toDomain() }
        val workout = program.workouts.firstOrNull { it.id == workoutId }
            ?: error("Тренировка не найдена")
        val exercise = workout.exercises.firstOrNull { it.id == exerciseId }
            ?: error("Упражнение не найдено")

        val dto = openAi.suggestReplacement(
            profile = program.profileSnapshot,
            workoutFocus = workout.focus,
            toReplace = exercise,
            reason = reason
        )

        return dto.suggestions.map { it.toExercise(exercise.orderIndex) }
    }

    override suspend fun replaceExercisePermanently(
        workoutId: String,
        exerciseIdOrigin: String,
        replacement: Exercise
    ) {
        val entity = ExerciseOverrideEntity(
            id = UUID.randomUUID().toString(),
            workoutId = workoutId,
            exerciseIdOrigin = exerciseIdOrigin,
            name = replacement.name,
            description = replacement.description,
            primaryMuscle = replacement.primaryMuscle,
            secondaryMuscles = replacement.secondaryMuscles,
            equipment = replacement.equipment,
            targetSets = replacement.targetSets,
            targetRepsMin = replacement.targetRepsMin,
            targetRepsMax = replacement.targetRepsMax,
            restSeconds = replacement.restSeconds,
            usesWeight = replacement.usesWeight,
            notes = replacement.notes
        )
        overrideDao.upsert(entity)
    }

    override suspend fun clearOverride(workoutId: String, exerciseIdOrigin: String) {
        overrideDao.deleteOverride(workoutId, exerciseIdOrigin)
    }

    override suspend fun updateExerciseFields(
        exerciseId: String,
        newWeightKg: Double?,
        newRepsMin: Int?,
        newRepsMax: Int?,
        newRestSeconds: Int?
    ) {
        // Находим упражнение в активной программе со склеенными overrides
        val program = getActiveProgram() ?: return
        val (workout, exercise) = program.workouts.firstNotNullOfOrNull { w ->
            w.exercises.firstOrNull { it.id == exerciseId }?.let { w to it }
        } ?: return

        // Создаём (или обновляем) override с новыми полями. Не указанные — берём из текущего.
        val patched = Exercise(
            id = exercise.id,
            name = exercise.name,
            description = exercise.description,
            primaryMuscle = exercise.primaryMuscle,
            secondaryMuscles = exercise.secondaryMuscles,
            equipment = exercise.equipment,
            targetSets = exercise.targetSets,
            targetRepsMin = newRepsMin ?: exercise.targetRepsMin,
            targetRepsMax = newRepsMax ?: exercise.targetRepsMax,
            restSeconds = newRestSeconds ?: exercise.restSeconds,
            usesWeight = exercise.usesWeight,
            notes = exercise.notes,
            orderIndex = exercise.orderIndex,
            startingWeightKg = newWeightKg ?: exercise.startingWeightKg,
            // Если есть detailed-сценарий и меняем вес — применяем ко ВСЕМ подходам пропорционально
            // чтобы не сломать пятничный жим. Простейший вариант — оставляем как есть и трогаем
            // только startingWeightKg. Detailed подходы пропустят рекомендацию INCREASE_WEIGHT.
            targetSetsDetailed = exercise.targetSetsDetailed
        )

        replaceExercisePermanently(workout.id, exercise.id, patched)
    }

    override suspend fun loadPresetProgram(): Program {
        val program = com.forma.app.presets.MyProgramPreset.build()
        saveAsActive(program)
        return program
    }
}

/**
 * Преобразование DTO замены в доменное Exercise.
 * Не используем Mappers.toDomain — там логика для полной программы.
 */
private fun ExerciseDto.toExercise(orderIndex: Int): Exercise = Exercise(
    name = name,
    description = description,
    primaryMuscle = runCatching { MuscleGroup.valueOf(primaryMuscle) }
        .getOrElse { MuscleGroup.FULL_BODY },
    secondaryMuscles = secondaryMuscles.mapNotNull {
        runCatching { MuscleGroup.valueOf(it) }.getOrNull()
    },
    equipment = equipment.mapNotNull {
        runCatching { Equipment.valueOf(it) }.getOrNull()
    },
    targetSets = targetSets,
    targetRepsMin = targetRepsMin,
    targetRepsMax = targetRepsMax,
    restSeconds = restSeconds,
    usesWeight = usesWeight,
    notes = notes.ifBlank { null },
    orderIndex = orderIndex
)
