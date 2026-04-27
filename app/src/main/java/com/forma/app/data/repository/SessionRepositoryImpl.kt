package com.forma.app.data.repository

import com.forma.app.data.local.dao.ProgramDao
import com.forma.app.data.local.dao.SessionDao
import com.forma.app.data.mapper.Mappers
import com.forma.app.domain.model.DayOfWeek
import com.forma.app.domain.model.ExerciseLog
import com.forma.app.domain.model.SetLog
import com.forma.app.domain.model.WorkoutSession
import com.forma.app.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val programDao: ProgramDao
) : SessionRepository {

    override suspend fun createSession(workoutId: String, programId: String): WorkoutSession {
        // Находим Workout в активной программе через polный domain-маппинг.
        val activeRaw = programDao.observeActive().first()
            ?: error("Нет активной программы")
        val activeProgram = with(Mappers) { activeRaw.toDomain() }
        val workout = activeProgram.workouts.firstOrNull { it.id == workoutId }
            ?: error("Тренировка не найдена")
        val exercises = workout.exercises.sortedBy { it.orderIndex }

        val exerciseLogs = exercises.map { ex ->
            ExerciseLog(
                id = UUID.randomUUID().toString(),
                exerciseId = ex.id,
                exerciseName = ex.name,
                sets = buildSetsForExercise(ex)
            )
        }

        val totalSets = exerciseLogs.sumOf { it.sets.size }

        val session = WorkoutSession(
            id = UUID.randomUUID().toString(),
            workoutId = workoutId,
            programId = programId,
            startedAt = System.currentTimeMillis(),
            finishedAt = null,
            dayOfWeek = currentDayOfWeek(),
            exerciseLogs = exerciseLogs,
            totalSetsCount = totalSets,
            completedSetsCount = 0,
            totalVolumeKg = 0.0
        )
        saveSession(session)
        return session
    }

    /**
     * Создаёт стартовый список подходов для упражнения:
     *  - если есть targetSetsDetailed — берём оттуда (разные веса/повторы по подходам)
     *  - иначе — равномерные подходы со startingWeightKg (либо 0)
     */
    private fun buildSetsForExercise(ex: com.forma.app.domain.model.Exercise): List<SetLog> {
        val detailed = ex.targetSetsDetailed
        if (detailed != null && detailed.isNotEmpty()) {
            return detailed.map { ts ->
                SetLog(
                    setNumber = ts.setNumber,
                    reps = 0,
                    // Для упражнений с весом всегда даём не-null значение,
                    // чтобы UI отрисовал поле ввода веса (0.0 если стартовый не задан).
                    weightKg = if (ex.usesWeight) (ts.weightKg ?: 0.0) else null,
                    isCompleted = false,
                    isOptional = ts.isOptional,
                    targetWeightKg = ts.weightKg,
                    targetRepsMin = ts.repsMin,
                    targetRepsMax = ts.repsMax
                )
            }
        }
        return (1..ex.targetSets).map { n ->
            SetLog(
                setNumber = n,
                reps = 0,
                weightKg = if (ex.usesWeight) (ex.startingWeightKg ?: 0.0) else null,
                isCompleted = false,
                isOptional = false,
                targetWeightKg = ex.startingWeightKg,
                targetRepsMin = ex.targetRepsMin,
                targetRepsMax = ex.targetRepsMax
            )
        }
    }

    override suspend fun saveSession(session: WorkoutSession) {
        val (s, logs, sets) = with(Mappers) { session.toEntities() }
        sessionDao.insertSession(s)
        sessionDao.insertExerciseLogs(logs)
        sessionDao.insertSetLogs(sets)
    }

    override suspend fun updateSet(
        sessionId: String,
        setId: String,
        reps: Int,
        weightKg: Double?,
        rir: Int?,
        completed: Boolean
    ) {
        val full = sessionDao.getSession(sessionId) ?: return
        val domain = with(Mappers) { full.toDomain() }
        var found = false
        val updatedLogs = domain.exerciseLogs.map { log ->
            log.copy(sets = log.sets.map { s ->
                if (s.id == setId) {
                    found = true
                    s.copy(
                        reps = reps,
                        weightKg = weightKg,
                        rir = rir,
                        isCompleted = completed,
                        completedAt = if (completed) System.currentTimeMillis() else null
                    )
                } else s
            })
        }
        if (!found) return

        val completedCount = updatedLogs.sumOf { it.sets.count { s -> s.isCompleted } }
        val volume = updatedLogs.sumOf { log ->
            log.sets.filter { it.isCompleted }
                .sumOf { (it.weightKg ?: 0.0) * it.reps }
        }

        val updated = domain.copy(
            exerciseLogs = updatedLogs,
            completedSetsCount = completedCount,
            totalVolumeKg = volume
        )
        saveSession(updated)
    }

    override fun observeSession(id: String): Flow<WorkoutSession?> =
        sessionDao.observeSession(id).map { raw -> raw?.let { with(Mappers) { it.toDomain() } } }

    override suspend fun getSession(id: String): WorkoutSession? =
        sessionDao.getSession(id)?.let { with(Mappers) { it.toDomain() } }

    override suspend fun finishSession(id: String) {
        val s = sessionDao.getSession(id)?.session ?: return
        sessionDao.updateSession(s.copy(finishedAt = System.currentTimeMillis()))
    }

    override fun observeSessionsSince(fromTs: Long): Flow<List<WorkoutSession>> =
        sessionDao.observeSessionsSince(fromTs).map { list ->
            list.map { with(Mappers) { it.toDomain() } }
        }

    override suspend fun lastSessionForWorkout(workoutId: String): WorkoutSession? =
        sessionDao.lastSessionForWorkout(workoutId)?.let { with(Mappers) { it.toDomain() } }

    override suspend fun recentSessionsForWorkout(
        workoutId: String,
        excludeId: String,
        limit: Int
    ): List<WorkoutSession> =
        sessionDao.recentSessionsForWorkout(workoutId, excludeId, limit)
            .map { with(Mappers) { it.toDomain() } }

    override suspend fun replaceExerciseInSession(
        sessionId: String,
        exerciseLogId: String,
        replacement: com.forma.app.domain.model.Exercise
    ) {
        val full = sessionDao.getSession(sessionId) ?: return
        val domain = with(Mappers) { full.toDomain() }

        val updatedLogs = domain.exerciseLogs.map { log ->
            if (log.id != exerciseLogId) return@map log
            val existing = log.sets
            val targetCount = replacement.targetSets

            // Обновляем targetWeight/Reps в существующих подходах + дополняем пустыми
            // если новое упражнение требует больше подходов
            val mappedExisting = existing.map { s ->
                s.copy(
                    targetWeightKg = replacement.startingWeightKg ?: s.targetWeightKg,
                    targetRepsMin = replacement.targetRepsMin,
                    targetRepsMax = replacement.targetRepsMax,
                    weightKg = if (s.weightKg == null && replacement.usesWeight)
                        replacement.startingWeightKg
                    else s.weightKg
                )
            }

            val newSets: List<com.forma.app.domain.model.SetLog> = when {
                mappedExisting.size == targetCount -> mappedExisting
                mappedExisting.size < targetCount -> {
                    mappedExisting + (mappedExisting.size + 1..targetCount).map { n ->
                        com.forma.app.domain.model.SetLog(
                            setNumber = n,
                            reps = 0,
                            weightKg = if (replacement.usesWeight) replacement.startingWeightKg else null,
                            isCompleted = false,
                            targetWeightKg = replacement.startingWeightKg,
                            targetRepsMin = replacement.targetRepsMin,
                            targetRepsMax = replacement.targetRepsMax
                        )
                    }
                }
                else -> {
                    val completedCount = mappedExisting.count { it.isCompleted }
                    val keep = maxOf(targetCount, completedCount)
                    mappedExisting.take(keep)
                }
            }
            log.copy(
                exerciseId = replacement.id,
                exerciseName = replacement.name,
                sets = newSets,
                notes = replacement.notes
            )
        }

        val totalSets = updatedLogs.sumOf { it.sets.size }
        val completedCount = updatedLogs.sumOf { it.sets.count { s -> s.isCompleted } }
        val volume = updatedLogs.sumOf { log ->
            log.sets.filter { it.isCompleted }.sumOf { (it.weightKg ?: 0.0) * it.reps }
        }
        saveSession(domain.copy(
            exerciseLogs = updatedLogs,
            totalSetsCount = totalSets,
            completedSetsCount = completedCount,
            totalVolumeKg = volume
        ))
    }

    private fun currentDayOfWeek(): DayOfWeek {
        val cal = Calendar.getInstance()
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> DayOfWeek.MON
            Calendar.TUESDAY -> DayOfWeek.TUE
            Calendar.WEDNESDAY -> DayOfWeek.WED
            Calendar.THURSDAY -> DayOfWeek.THU
            Calendar.FRIDAY -> DayOfWeek.FRI
            Calendar.SATURDAY -> DayOfWeek.SAT
            else -> DayOfWeek.SUN
        }
    }
}
