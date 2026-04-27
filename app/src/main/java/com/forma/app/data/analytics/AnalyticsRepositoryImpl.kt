package com.forma.app.data.analytics

import com.forma.app.domain.analytics.AnalyticsRepository
import com.forma.app.domain.analytics.ExerciseProgressPoint
import com.forma.app.domain.analytics.ExerciseProgressSeries
import com.forma.app.domain.analytics.MuscleGroupVolume
import com.forma.app.domain.analytics.ProgressData
import com.forma.app.domain.analytics.WeeklyAggregate
import com.forma.app.domain.model.Exercise
import com.forma.app.domain.model.MuscleGroup
import com.forma.app.domain.model.Program
import com.forma.app.domain.model.WorkoutSession
import com.forma.app.domain.repository.ProgramRepository
import com.forma.app.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val programRepo: ProgramRepository
) : AnalyticsRepository {

    private val weekLabelFormat = SimpleDateFormat("d MMM", Locale("ru"))

    override fun observeProgress(weeksBack: Int): Flow<ProgressData> {
        val fromTs = startOfWeek(System.currentTimeMillis()) - weeksBack * WEEK_MS

        return combine(
            sessionRepo.observeSessionsSince(fromTs),
            programRepo.observeActiveProgram()
        ) { sessions, program ->
            buildProgressData(sessions.filter { it.isFinished }, program, weeksBack)
        }
    }

    private fun buildProgressData(
        sessions: List<WorkoutSession>,
        program: Program?,
        weeksBack: Int
    ): ProgressData {
        if (sessions.isEmpty()) return ProgressData(emptyList(), emptyList(), emptyList())

        val weeks = aggregateByWeek(sessions, weeksBack)
        val exerciseLookup = buildExerciseLookup(program)
        val keyExercises = aggregateExerciseSeries(sessions, exerciseLookup)
        val muscleVolume = aggregateMuscleVolume(sessions, exerciseLookup)

        return ProgressData(
            weeks = weeks,
            keyExercises = keyExercises,
            muscleVolume = muscleVolume
        )
    }

    // ─── Объём по неделям ───────────────────────────────────────────

    private fun aggregateByWeek(
        sessions: List<WorkoutSession>,
        weeksBack: Int
    ): List<WeeklyAggregate> {
        val nowWeekStart = startOfWeek(System.currentTimeMillis())
        // Создаём слоты для всех weeksBack недель — даже пустые, чтобы график не "проседал"
        val slots = (0 until weeksBack).map { i ->
            val ws = nowWeekStart - i * WEEK_MS
            ws
        }.reversed()  // от старых к новым

        val grouped = sessions.groupBy { startOfWeek(it.startedAt) }

        return slots.map { weekStart ->
            val ws = grouped[weekStart].orEmpty()
            val volume = ws.sumOf { it.totalVolumeKg }
            val completed = ws.sumOf { it.completedSetsCount }
            val planned = ws.sumOf { it.totalSetsCount }
            val rirValues = ws.flatMap { session ->
                session.exerciseLogs.flatMap { log ->
                    log.sets.mapNotNull { set ->
                        if (set.isCompleted) set.rir?.toDouble() else null
                    }
                }
            }
            val avgRir = if (rirValues.isEmpty()) null else rirValues.average()

            WeeklyAggregate(
                weekStart = weekStart,
                weekLabel = weekLabelFormat.format(java.util.Date(weekStart)),
                totalVolumeKg = volume,
                totalSetsCompleted = completed,
                totalSetsPlanned = planned,
                sessionsCount = ws.size,
                avgRir = avgRir,
                completionRatio = if (planned == 0) 0.0 else completed.toDouble() / planned
            )
        }
    }

    // ─── Серии прогресса по упражнениям ─────────────────────────────

    /**
     * Лукап exerciseId -> доменное Exercise — для получения primaryMuscle.
     * Берём из активной программы. Для старых сессий другой программы — fallback.
     */
    private fun buildExerciseLookup(program: Program?): Map<String, Exercise> {
        if (program == null) return emptyMap()
        return program.workouts.flatMap { it.exercises }.associateBy { it.id }
    }

    /**
     * Группируем все ExerciseLog по нормализованному имени.
     * Для каждой сессии считаем максимальный рабочий вес (самый тяжёлый завершённый подход).
     */
    private fun aggregateExerciseSeries(
        sessions: List<WorkoutSession>,
        exerciseLookup: Map<String, Exercise>
    ): List<ExerciseProgressSeries> {
        // Сначала собираем ВСЕ точки по всем упражнениям
        data class RawPoint(
            val name: String,
            val muscle: MuscleGroup,
            val ts: Long,
            val maxWeight: Double,
            val repsAtMax: Int
        )

        val rawPoints = sessions.flatMap { session ->
            session.exerciseLogs.mapNotNull { log ->
                val completedSets = log.sets.filter { it.isCompleted && (it.weightKg ?: 0.0) > 0 }
                if (completedSets.isEmpty()) return@mapNotNull null
                val topSet = completedSets.maxByOrNull { it.weightKg ?: 0.0 } ?: return@mapNotNull null
                val muscle = exerciseLookup[log.exerciseId]?.primaryMuscle ?: MuscleGroup.FULL_BODY
                RawPoint(
                    name = normalizeName(log.exerciseName),
                    muscle = muscle,
                    ts = session.startedAt,
                    maxWeight = topSet.weightKg ?: 0.0,
                    repsAtMax = topSet.reps
                )
            }
        }

        // Группируем по нормализованному имени, оставляем те где >= 2 точки
        return rawPoints
            .groupBy { it.name }
            .filter { (_, points) -> points.size >= 2 }
            .map { (name, points) ->
                val sorted = points.sortedBy { it.ts }
                ExerciseProgressSeries(
                    exerciseName = name,
                    primaryMuscle = sorted.first().muscle,
                    points = sorted.map {
                        ExerciseProgressPoint(it.ts, it.maxWeight, it.repsAtMax)
                    }
                )
            }
            .sortedByDescending { it.points.size }   // самые часто-повторяемые первыми
            .take(8)
    }

    private fun normalizeName(raw: String): String =
        raw.trim().lowercase()
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }

    // ─── Объём по группам мышц ──────────────────────────────────────

    private fun aggregateMuscleVolume(
        sessions: List<WorkoutSession>,
        exerciseLookup: Map<String, Exercise>
    ): List<MuscleGroupVolume> {
        data class Acc(var volume: Double = 0.0, var sets: Int = 0)
        val byMuscle = mutableMapOf<MuscleGroup, Acc>()

        sessions.forEach { session ->
            session.exerciseLogs.forEach { log ->
                val muscle = exerciseLookup[log.exerciseId]?.primaryMuscle ?: return@forEach
                val acc = byMuscle.getOrPut(muscle) { Acc() }
                log.sets.filter { it.isCompleted }.forEach { set ->
                    acc.volume += (set.weightKg ?: 0.0) * set.reps
                    acc.sets += 1
                }
            }
        }

        return byMuscle
            .map { (m, acc) -> MuscleGroupVolume(m, acc.volume, acc.sets) }
            .filter { it.setsCount > 0 }
            .sortedByDescending { it.volumeKg }
    }

    // ─── Утилиты времени ────────────────────────────────────────────

    /**
     * Возвращает timestamp понедельника 00:00 локального времени для данного ts.
     */
    private fun startOfWeek(ts: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Ставим понедельник как первый день
            firstDayOfWeek = Calendar.MONDAY
            // Если сегодня воскресенье — отмотать на 6 дней назад, иначе на (DAY_OF_WEEK - MONDAY)
            val shift = (get(Calendar.DAY_OF_WEEK) + 5) % 7
            add(Calendar.DAY_OF_MONTH, -shift)
        }
        return cal.timeInMillis
    }

    companion object {
        private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
    }
}
