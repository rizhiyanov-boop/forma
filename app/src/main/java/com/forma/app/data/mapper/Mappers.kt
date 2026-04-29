package com.forma.app.data.mapper

import com.forma.app.data.local.dao.ExerciseLogWithSets
import com.forma.app.data.local.dao.ProgramWithWorkouts
import com.forma.app.data.local.dao.SessionWithLogs
import com.forma.app.data.local.dao.WorkoutWithExercises
import com.forma.app.data.local.entity.ExerciseEntity
import com.forma.app.data.local.entity.ExerciseLogEntity
import com.forma.app.data.local.entity.ProgramEntity
import com.forma.app.data.local.entity.SetLogEntity
import com.forma.app.data.local.entity.WorkoutEntity
import com.forma.app.data.local.entity.WorkoutSessionEntity
import com.forma.app.data.remote.dto.ExerciseDto
import com.forma.app.data.remote.dto.ProgramPlanDto
import com.forma.app.data.remote.dto.WorkoutDto
import com.forma.app.domain.model.DayOfWeek
import com.forma.app.domain.model.Equipment
import com.forma.app.domain.model.Exercise
import com.forma.app.domain.model.ExerciseLog
import com.forma.app.domain.model.MuscleGroup
import com.forma.app.domain.model.Program
import com.forma.app.domain.model.SetLog
import com.forma.app.domain.model.TargetSet
import com.forma.app.domain.model.UserProfile
import com.forma.app.domain.model.Workout
import com.forma.app.domain.model.WorkoutSession
import kotlinx.serialization.json.Json
import java.util.UUID

object Mappers {

    private val json = Json { ignoreUnknownKeys = true }

    // --- DTO -> Domain (программа от AI) ---

    fun ProgramPlanDto.toDomain(profile: UserProfile): Program {
        val programId = UUID.randomUUID().toString()
        return Program(
            id = programId,
            name = programName,
            description = programDescription,
            profileSnapshot = profile,
            workouts = workouts.map { it.toDomain(programId) }
        )
    }

    private fun WorkoutDto.toDomain(programId: String): Workout {
        val workoutId = UUID.randomUUID().toString()
        return Workout(
            id = workoutId,
            programId = programId,
            dayOfWeek = runCatching { DayOfWeek.valueOf(dayOfWeek) }.getOrElse { DayOfWeek.MON },
            title = title,
            focus = focus,
            estimatedMinutes = estimatedMinutes,
            exercises = exercises.mapIndexed { i, e -> e.toDomain(i) }
        )
    }

    private fun ExerciseDto.toDomain(orderIndex: Int): Exercise = Exercise(
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

    // --- Domain -> Entity ---

    fun Program.toEntities(): Triple<ProgramEntity, List<WorkoutEntity>, List<ExerciseEntity>> {
        val programEntity = ProgramEntity(
            id = id,
            name = name,
            description = description,
            weekNumber = weekNumber,
            profileSnapshotJson = json.encodeToString(UserProfile.serializer(), profileSnapshot),
            createdAt = createdAt
        )
        val workoutEntities = workouts.mapIndexed { i, w ->
            WorkoutEntity(
                id = w.id,
                programId = id,
                dayOfWeek = w.dayOfWeek,
                title = w.title,
                focus = w.focus,
                estimatedMinutes = w.estimatedMinutes,
                orderIndex = i
            )
        }
        val exerciseEntities = workouts.flatMap { w ->
            w.exercises.mapIndexed { i, e ->
                ExerciseEntity(
                    id = e.id,
                    workoutId = w.id,
                    contentId = e.contentId,
                    name = e.name,
                    description = e.description,
                    primaryMuscle = e.primaryMuscle,
                    secondaryMuscles = e.secondaryMuscles,
                    equipment = e.equipment,
                    targetSets = e.targetSets,
                    targetRepsMin = e.targetRepsMin,
                    targetRepsMax = e.targetRepsMax,
                    restSeconds = e.restSeconds,
                    usesWeight = e.usesWeight,
                    notes = e.notes,
                    orderIndex = i,
                    startingWeightKg = e.startingWeightKg,
                    targetSetsDetailedJson = e.targetSetsDetailed?.let {
                        json.encodeToString(kotlinx.serialization.builtins.ListSerializer(TargetSet.serializer()), it)
                    }
                )
            }
        }
        return Triple(programEntity, workoutEntities, exerciseEntities)
    }

    // --- Entity -> Domain ---

    fun ProgramWithWorkouts.toDomain(): Program {
        val profile = runCatching {
            json.decodeFromString(UserProfile.serializer(), program.profileSnapshotJson)
        }.getOrNull() ?: defaultProfileStub()

        return Program(
            id = program.id,
            name = program.name,
            description = program.description,
            createdAt = program.createdAt,
            weekNumber = program.weekNumber,
            profileSnapshot = profile,
            workouts = workouts
                .sortedBy { it.workout.orderIndex }
                .map { it.toDomain() }
        )
    }

    private fun WorkoutWithExercises.toDomain(): Workout = Workout(
        id = workout.id,
        programId = workout.programId,
        dayOfWeek = workout.dayOfWeek,
        title = workout.title,
        focus = workout.focus,
        estimatedMinutes = workout.estimatedMinutes,
        exercises = exercises.sortedBy { it.orderIndex }.map { it.toDomain() }
    )

    private fun ExerciseEntity.toDomain(): Exercise = Exercise(
        id = id,
        contentId = contentId,
        name = name,
        description = description,
        primaryMuscle = primaryMuscle,
        secondaryMuscles = secondaryMuscles,
        equipment = equipment,
        targetSets = targetSets,
        targetRepsMin = targetRepsMin,
        targetRepsMax = targetRepsMax,
        restSeconds = restSeconds,
        usesWeight = usesWeight,
        notes = notes,
        orderIndex = orderIndex,
        startingWeightKg = startingWeightKg,
        targetSetsDetailed = targetSetsDetailedJson?.let {
            runCatching {
                json.decodeFromString(
                    kotlinx.serialization.builtins.ListSerializer(TargetSet.serializer()),
                    it
                )
            }.getOrNull()
        }
    )

    // --- Session entity <-> domain ---

    fun WorkoutSession.toEntities(): Triple<
            WorkoutSessionEntity, List<ExerciseLogEntity>, List<SetLogEntity>> {
        val sessionEntity = WorkoutSessionEntity(
            id = id,
            workoutId = workoutId,
            programId = programId,
            startedAt = startedAt,
            finishedAt = finishedAt,
            dayOfWeek = dayOfWeek,
            totalVolumeKg = totalVolumeKg,
            completedSetsCount = completedSetsCount,
            totalSetsCount = totalSetsCount
        )
        val sessionId = id
        val logEntities = exerciseLogs.mapIndexed { i, log ->
            ExerciseLogEntity(
                id = log.id,
                sessionId = sessionId,
                exerciseId = log.exerciseId,
                exerciseName = log.exerciseName,
                notes = log.notes,
                orderIndex = i
            )
        }
        val setEntities = exerciseLogs.flatMap { log ->
            log.sets.map { set ->
                SetLogEntity(
                    id = set.id,
                    exerciseLogId = log.id,
                    setNumber = set.setNumber,
                    reps = set.reps,
                    weightKg = set.weightKg,
                    rpe = set.rpe,
                    rir = set.rir,
                    isCompleted = set.isCompleted,
                    completedAt = set.completedAt,
                    isOptional = set.isOptional,
                    targetWeightKg = set.targetWeightKg,
                    targetRepsMin = set.targetRepsMin,
                    targetRepsMax = set.targetRepsMax
                )
            }
        }
        return Triple(sessionEntity, logEntities, setEntities)
    }

    fun SessionWithLogs.toDomain(): WorkoutSession = WorkoutSession(
        id = session.id,
        workoutId = session.workoutId,
        programId = session.programId,
        startedAt = session.startedAt,
        finishedAt = session.finishedAt,
        dayOfWeek = session.dayOfWeek,
        exerciseLogs = exerciseLogs
            .sortedBy { it.log.orderIndex }
            .map { it.toDomain() },
        totalVolumeKg = session.totalVolumeKg,
        completedSetsCount = session.completedSetsCount,
        totalSetsCount = session.totalSetsCount
    )

    private fun ExerciseLogWithSets.toDomain(): ExerciseLog = ExerciseLog(
        id = log.id,
        exerciseId = log.exerciseId,
        exerciseName = log.exerciseName,
        sets = sets.sortedBy { it.setNumber }.map {
            SetLog(
                id = it.id,
                setNumber = it.setNumber,
                reps = it.reps,
                weightKg = it.weightKg,
                rpe = it.rpe,
                rir = it.rir,
                isCompleted = it.isCompleted,
                completedAt = it.completedAt,
                isOptional = it.isOptional,
                targetWeightKg = it.targetWeightKg,
                targetRepsMin = it.targetRepsMin,
                targetRepsMax = it.targetRepsMax
            )
        },
        notes = log.notes
    )

    private fun defaultProfileStub(): UserProfile = UserProfile(
        goal = com.forma.app.domain.model.Goal.GENERAL_FITNESS,
        level = com.forma.app.domain.model.ExperienceLevel.BEGINNER,
        sex = com.forma.app.domain.model.Sex.UNSPECIFIED,
        daysPerWeek = 3,
        preferredDays = emptyList(),
        equipment = emptyList(),
        heightCm = null,
        weightKg = null,
        age = null,
        limitations = null
    )
}
