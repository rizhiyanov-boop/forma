package com.forma.app.domain.coaching

import com.forma.app.domain.model.Exercise
import com.forma.app.domain.model.ExerciseLog
import com.forma.app.domain.model.ExperienceLevel
import com.forma.app.domain.model.Goal
import com.forma.app.domain.model.SetLog
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * Детерминированный движок прогрессии. Принимает упражнение, его историю
 * (последние N сессий) и параметры пользователя, выдаёт конкретную
 * рекомендацию что делать дальше.
 *
 * Базируется на:
 *   - Двойной прогрессии (Helms / Muscle and Strength Pyramid):
 *     сначала растим повторы внутри диапазона, потом вес.
 *   - 4%-правиле автoрегуляции (Helms 2018): на каждый репс отклонения
 *     от целевого RIR корректируем вес на ~4%.
 *   - Cap +5%/-15% за раз — защита от резких скачков (sanity).
 *
 * Движок **не делает API-вызовов**, не зависит от Hilt/Android — это чистый
 * Kotlin, легко юнит-тестируется. AI обогащение происходит в слое выше.
 */
class ProgressionEngine {

    /**
     * Главный вход.
     *
     * @param exercise текущее состояние упражнения в активной программе
     * @param history последние выполненные сессии этого упражнения,
     *                от старых к новым. Пустой список = нет данных.
     * @param goal цель пользователя (определяет целевой RIR и диапазон повторов
     *             если в самом упражнении не задано явно)
     * @param level уровень атлета (влияет на агрессивность шагов и DELOAD-логику)
     */
    fun analyze(
        exercise: Exercise,
        history: List<ExerciseHistoryPoint>,
        goal: Goal,
        level: ExperienceLevel
    ): EngineRecommendation {
        // Не имеет веса — отдельная упрощённая ветка по повторам.
        if (!exercise.usesWeight) {
            return analyzeBodyweight(exercise, history, goal)
        }

        if (history.isEmpty()) {
            return EngineRecommendation(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                action = ProgressionAction.INSUFFICIENT_DATA,
                confidence = Confidence.HIGH,
                rationale = "Нет завершённых тренировок этого упражнения — наблюдаем",
                signals = ProgressionSignals(
                    sessionsAnalyzed = 0,
                    avgRirLast = null,
                    avgRepsLast = 0.0,
                    workingWeightLast = exercise.startingWeightKg ?: 0.0,
                    targetRir = targetRirFor(goal),
                    rirOffset = null,
                    hitTopOfRange = false,
                    sessionsAtSameWeight = 0,
                    regressStreak = 0
                )
            )
        }

        val targetRir = targetRirFor(goal)
        val last = history.last()
        val prev = history.dropLast(1).lastOrNull()

        // Считаем сигналы по последней сессии
        val avgRir = last.avgRir
        val avgReps = last.avgReps
        val workingWeight = last.workingWeightKg
        val rirOffset = avgRir?.let { it - targetRir }
        val hitTop = avgReps >= exercise.targetRepsMax - 0.01    // флоат-трюк
        val sessionsAtSame = countSessionsAtSameWeight(history, workingWeight)
        val regressStreak = countRegressStreak(history)

        val signals = ProgressionSignals(
            sessionsAnalyzed = history.size,
            avgRirLast = avgRir,
            avgRepsLast = avgReps,
            workingWeightLast = workingWeight,
            targetRir = targetRir,
            rirOffset = rirOffset,
            hitTopOfRange = hitTop,
            sessionsAtSameWeight = sessionsAtSame,
            regressStreak = regressStreak
        )

        // ─── МАТРИЦА РЕШЕНИЙ ────────────────────────────────────────────────
        // Порядок проверок важен: от самых критичных (DELOAD/REGRESS)
        // к самым позитивным (INCREASE).

        // 1. Серия регрессов 2+ → DELOAD
        if (regressStreak >= 2) {
            val deloadWeight = roundToHalfKg(workingWeight * 0.85)
            return EngineRecommendation(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                action = ProgressionAction.DELOAD,
                confidence = Confidence.HIGH,
                newWeightKg = deloadWeight,
                newRepsMin = max(exercise.targetRepsMin - 2, 5),
                newRepsMax = max(exercise.targetRepsMax - 2, 8),
                rationale = "Регресс 2 тренировки подряд — снижаем вес на 15%, " +
                    "повторы тоже сократим. Нужна разгрузка.",
                signals = signals
            )
        }

        // 2. Регресс одной тренировки сильный (RIR << цели и/или повторы ниже мин)
        // - но только если есть с чем сравнивать
        if (prev != null && isConfirmedRegress(last, prev, exercise, targetRir)) {
            val step = stepFor(workingWeight, level)
            // Сбрасываем на 1 шаг, но защитно не больше 15%
            val drop = min(step, workingWeight * 0.15)
            val newWeight = roundToHalfKg(workingWeight - drop)
            return EngineRecommendation(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                action = ProgressionAction.DECREASE_WEIGHT,
                confidence = Confidence.MEDIUM,
                newWeightKg = newWeight,
                rationale = buildString {
                    append("Падение vs предыдущая: ")
                    append(formatWeight(workingWeight))
                    append("×")
                    append(avgReps.toInt())
                    if (avgRir != null) append(" RIR ").append(avgRir.toInt())
                    append(". Вернёмся на шаг назад.")
                },
                signals = signals
            )
        }

        // 3. Глубокий регресс — RIR=0 в большинстве подходов и повторы сильно ниже мин
        if (avgRir != null && avgRir <= 0.5 && avgReps < exercise.targetRepsMin - 1) {
            val newWeight = roundToHalfKg(workingWeight * 0.9)  // -10%
            return EngineRecommendation(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                action = ProgressionAction.DECREASE_WEIGHT,
                confidence = Confidence.HIGH,
                newWeightKg = newWeight,
                rationale = "Не добил подходы (RIR ~0, повторы ниже мин) — " +
                    "сбрасываем на 10% чтобы снова работать в диапазоне",
                signals = signals
            )
        }

        // 4. Стагнация: 3+ тренировки на том же весе без роста повторов
        if (sessionsAtSame >= 3 && !hitTop) {
            return EngineRecommendation(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                action = ProgressionAction.INCREASE_REPS,
                confidence = Confidence.MEDIUM,
                newRepsMin = exercise.targetRepsMin,
                newRepsMax = exercise.targetRepsMax + 2,
                rationale = "3 тренировки на ${formatWeight(workingWeight)} без роста " +
                    "повторов — поднимаем потолок диапазона ещё на 2 повтора, " +
                    "цель прорваться выше прежде чем добавить вес",
                signals = signals
            )
        }

        // 5. Идеальный прогресс — попал в верх повторов с правильным RIR
        if (hitTop && rirOffset != null && rirOffset in -0.5..1.5) {
            val step = stepFor(workingWeight, level)
            val newWeight = roundToHalfKg(workingWeight + step)
            return EngineRecommendation(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                action = ProgressionAction.INCREASE_WEIGHT,
                confidence = Confidence.HIGH,
                newWeightKg = newWeight,
                newRepsMin = exercise.targetRepsMin,
                newRepsMax = exercise.targetRepsMax,
                rationale = "Закрыл верх диапазона ${exercise.targetRepsMax} повторов " +
                    "при целевом RIR — готов к +${formatWeight(step)} кг",
                signals = signals
            )
        }

        // 6. Запас явный: RIR ≥ цель+2 на двух+ тренировках, в диапазоне
        if (rirOffset != null && rirOffset >= 2.0 && sessionsAtSame >= 2) {
            // Большой шаг — но не больше 2× стандартного и не больше 5% от веса
            val baseStep = stepFor(workingWeight, level)
            val multiplier = ceil(rirOffset / 2.0).toInt().coerceAtMost(2)
            val rawShift = baseStep * multiplier
            val shift = min(rawShift, workingWeight * 0.05)
            val newWeight = roundToHalfKg(workingWeight + shift)
            return EngineRecommendation(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                action = ProgressionAction.INCREASE_WEIGHT,
                confidence = Confidence.HIGH,
                newWeightKg = newWeight,
                rationale = "RIR ${avgRir?.toInt()} при цели $targetRir два раза подряд " +
                    "— большой запас, поднимаем на +${formatWeight(shift)} кг",
                signals = signals
            )
        }

        // 7. На цели и в верх диапазона (одна тренировка) — стандартный шаг
        if (hitTop && rirOffset != null && rirOffset >= -0.5) {
            val step = stepFor(workingWeight, level)
            val newWeight = roundToHalfKg(workingWeight + step)
            return EngineRecommendation(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                action = ProgressionAction.INCREASE_WEIGHT,
                confidence = Confidence.MEDIUM,
                newWeightKg = newWeight,
                rationale = "Закрыл диапазон без особых проблем, " +
                    "пора +${formatWeight(step)} кг",
                signals = signals
            )
        }

        // 8. Тяжело раз — KEEP, дать шанс
        if (rirOffset != null && rirOffset < -1.5) {
            return EngineRecommendation(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                action = ProgressionAction.KEEP,
                confidence = Confidence.MEDIUM,
                rationale = "RIR ниже целевого — даём ещё одну тренировку " +
                    "на ${formatWeight(workingWeight)} прежде чем что-то менять",
                signals = signals
            )
        }

        // 9. По умолчанию — KEEP с целью на +1 повтор
        return EngineRecommendation(
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            action = ProgressionAction.KEEP,
            confidence = Confidence.LOW,
            rationale = "В диапазоне, держим вес и целимся в +1 повтор " +
                "на следующей тренировке",
            signals = signals
        )
    }

    /**
     * Упражнения без веса (бодвейт): анализ только по повторам и RIR.
     */
    private fun analyzeBodyweight(
        exercise: Exercise,
        history: List<ExerciseHistoryPoint>,
        goal: Goal
    ): EngineRecommendation {
        val targetRir = targetRirFor(goal)
        if (history.isEmpty()) {
            return EngineRecommendation(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                action = ProgressionAction.INSUFFICIENT_DATA,
                confidence = Confidence.HIGH,
                rationale = "Нет данных по этому упражнению",
                signals = ProgressionSignals(
                    sessionsAnalyzed = 0,
                    avgRirLast = null,
                    avgRepsLast = 0.0,
                    workingWeightLast = 0.0,
                    targetRir = targetRir,
                    rirOffset = null,
                    hitTopOfRange = false,
                    sessionsAtSameWeight = 0,
                    regressStreak = 0
                )
            )
        }
        val last = history.last()
        val signals = ProgressionSignals(
            sessionsAnalyzed = history.size,
            avgRirLast = last.avgRir,
            avgRepsLast = last.avgReps,
            workingWeightLast = 0.0,
            targetRir = targetRir,
            rirOffset = last.avgRir?.let { it - targetRir },
            hitTopOfRange = last.avgReps >= exercise.targetRepsMax - 0.01,
            sessionsAtSameWeight = history.size,
            regressStreak = 0
        )
        // Бодвейт: если RIR ≥ цель+2 → растим повторы. Иначе KEEP.
        val rirOffset = signals.rirOffset
        return if (rirOffset != null && rirOffset >= 2.0) {
            EngineRecommendation(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                action = ProgressionAction.INCREASE_REPS,
                confidence = Confidence.MEDIUM,
                newRepsMin = exercise.targetRepsMin + 2,
                newRepsMax = exercise.targetRepsMax + 2,
                rationale = "Бодвейт с большим запасом RIR — добавляем 2 повтора в диапазон",
                signals = signals
            )
        } else {
            EngineRecommendation(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                action = ProgressionAction.KEEP,
                confidence = Confidence.LOW,
                rationale = "Бодвейт в рабочей зоне — продолжаем",
                signals = signals
            )
        }
    }

    // ─── Хелперы ────────────────────────────────────────────────────────────

    /**
     * Шаг прибавки веса по диапазону текущего веса.
     * Защита: не больше 4% от рабочего веса (автоматическое масштабирование
     * для лёгких изоляций, где 2.5 кг = 20%).
     */
    fun stepFor(weight: Double, level: ExperienceLevel): Double {
        val defaultStep = when {
            weight <= 15.0 -> 1.0
            weight <= 40.0 -> 2.0
            weight <= 80.0 -> 2.5
            weight <= 120.0 -> 5.0
            else -> 5.0
        }
        // ADVANCED — микропрогрессия, шаги меньше
        val levelMultiplier = when (level) {
            ExperienceLevel.BEGINNER -> 1.0
            ExperienceLevel.INTERMEDIATE -> 1.0
            ExperienceLevel.ADVANCED -> 0.5    // делим шаг пополам
        }
        val candidate = defaultStep * levelMultiplier
        // 4%-cap — главная защита от перепрыгиваний
        val capped = min(candidate, weight * 0.04)
        // Не меньше 0.5 кг (микро-блины)
        return max(capped, 0.5)
    }

    /**
     * Целевой RIR по цели программы.
     */
    fun targetRirFor(goal: Goal): Int = when (goal) {
        Goal.STRENGTH -> 2
        Goal.MUSCLE_GAIN -> 1
        Goal.ENDURANCE -> 2
        Goal.FAT_LOSS -> 2
        Goal.GENERAL_FITNESS -> 2
    }

    /**
     * Округляем до 0.5 кг — стандартная гранулярность блинов в зале.
     */
    private fun roundToHalfKg(weight: Double): Double = round(weight * 2.0) / 2.0

    private fun formatWeight(w: Double): String =
        if (w == w.toInt().toDouble()) "${w.toInt()}" else "%.1f".format(w)

    private fun countSessionsAtSameWeight(
        history: List<ExerciseHistoryPoint>,
        weight: Double
    ): Int {
        var count = 0
        for (point in history.reversed()) {
            if (kotlin.math.abs(point.workingWeightKg - weight) < 0.5) count++
            else break
        }
        return count
    }

    private fun countRegressStreak(history: List<ExerciseHistoryPoint>): Int {
        if (history.size < 2) return 0
        var streak = 0
        for (i in history.size - 1 downTo 1) {
            val curr = history[i]
            val prev = history[i - 1]
            // Регресс: либо вес меньше, либо тот же вес, но повторов меньше на 2+
            val isRegress = curr.workingWeightKg < prev.workingWeightKg - 0.5 ||
                (kotlin.math.abs(curr.workingWeightKg - prev.workingWeightKg) < 0.5 &&
                    curr.avgReps < prev.avgReps - 1.5)
            if (isRegress) streak++ else break
        }
        return streak
    }

    private fun isConfirmedRegress(
        last: ExerciseHistoryPoint,
        prev: ExerciseHistoryPoint,
        exercise: Exercise,
        targetRir: Int
    ): Boolean {
        // Регресс если:
        // - такой же или меньший вес И повторы упали ниже минимума диапазона
        // - ИЛИ RIR ниже цели на 2+ при пониженных повторах
        val sameOrLessWeight = last.workingWeightKg <= prev.workingWeightKg + 0.5
        val repsDroppedBelowMin = last.avgReps < exercise.targetRepsMin - 0.5
        val rirCollapsed = last.avgRir != null && last.avgRir < targetRir - 1.5
        return sameOrLessWeight && (repsDroppedBelowMin || rirCollapsed)
    }
}

/**
 * Агрегированная точка истории по упражнению — что мы взяли из ExerciseLog
 * (или из топ-сета `targetSetsDetailed`-сценария).
 *
 * Это "сжатый" вход для движка — он не работает с сырыми SetLog'ами,
 * а с уже подсчитанным workingWeight/avgReps/avgRir per-сессия.
 */
data class ExerciseHistoryPoint(
    val sessionId: String,
    val timestamp: Long,
    /** Модальный/максимальный рабочий вес сессии (топ-сет для detailed-сценария). */
    val workingWeightKg: Double,
    /** Средние повторы по рабочим (не-разогревочным, не-опциональным) подходам. */
    val avgReps: Double,
    /**
     * Средний RIR по рабочим подходам где RIR заполнен.
     * null если ни один подход не имеет RIR (юзер просто не вводил).
     */
    val avgRir: Double?,
    /** Сколько подходов выполнено. Информационное поле. */
    val completedSets: Int
)

/**
 * Билдер ExerciseHistoryPoint из ExerciseLog. Изолирует сложную логику
 * "что считать рабочим подходом" в одном месте.
 *
 * Правила:
 * - Опциональные подходы (isOptional=true) исключаются из RIR/reps анализа,
 *   но учитываются в completedSets для статистики.
 * - Если у упражнения есть `targetSetsDetailed` (рамп-ап + топ-сет, как
 *   пятничный жим) — берём только подходы с максимальным targetWeightKg.
 * - Если нет detailed — берём все рабочие подходы как однородные.
 */
object HistoryPointBuilder {

    fun fromLog(
        sessionId: String,
        sessionTimestamp: Long,
        log: ExerciseLog
    ): ExerciseHistoryPoint? {
        val completed = log.sets.filter { it.isCompleted }
        if (completed.isEmpty()) return null

        // Шаг 1. Определить рабочие подходы.
        val workingSets = selectWorkingSets(completed)
        if (workingSets.isEmpty()) return null

        // Шаг 2. Считаем агрегаты ТОЛЬКО по рабочим подходам.
        val maxWeight = workingSets.mapNotNull { it.weightKg }.maxOrNull() ?: 0.0
        val avgReps = workingSets.map { it.reps }.average()

        // RIR — только из рабочих подходов где он заполнен
        val rirValues = workingSets.mapNotNull { it.rir }
        val avgRir = if (rirValues.isEmpty()) null
                     else rirValues.average()

        return ExerciseHistoryPoint(
            sessionId = sessionId,
            timestamp = sessionTimestamp,
            workingWeightKg = maxWeight,
            avgReps = avgReps,
            avgRir = avgRir,
            completedSets = completed.size
        )
    }

    /**
     * Выбирает рабочие подходы:
     *  1. Исключаем опциональные.
     *  2. Если у подходов есть targetWeightKg (= в программе был targetSetsDetailed),
     *     берём только подходы с максимальным целевым весом — это "топ-сет",
     *     остальное игнорируем как разогрев/закрепляющие.
     *  3. Иначе все non-optional завершённые подходы.
     */
    private fun selectWorkingSets(completed: List<SetLog>): List<SetLog> {
        val nonOptional = completed.filter { !it.isOptional }
        if (nonOptional.isEmpty()) return emptyList()

        // Detailed-сценарий: есть targetWeightKg на хотя бы одном подходе
        val targetWeights = nonOptional.mapNotNull { it.targetWeightKg }
        return if (targetWeights.isNotEmpty()) {
            val topTarget = targetWeights.max()
            nonOptional.filter { (it.targetWeightKg ?: 0.0) >= topTarget - 0.01 }
        } else {
            nonOptional
        }
    }
}
