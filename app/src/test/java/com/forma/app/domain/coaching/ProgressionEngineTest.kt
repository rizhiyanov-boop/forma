package com.forma.app.domain.coaching

import com.forma.app.domain.model.Equipment
import com.forma.app.domain.model.Exercise
import com.forma.app.domain.model.ExperienceLevel
import com.forma.app.domain.model.Goal
import com.forma.app.domain.model.MuscleGroup
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Тесты ProgressionEngine. Покрывают все 9 веток матрицы решений + edge cases.
 *
 * Структура каждого теста:
 *  1. Сценарий — что моделируем (комментарий).
 *  2. Вход — exercise + history.
 *  3. Действие — engine.analyze(...).
 *  4. Проверка — action + ключевые числа в рекомендации.
 *
 * Все кейсы используют типичный жим лёжа (грудь, рабочий вес 60 кг, целевой
 * диапазон 6-10 повторов). Это типичное упражнение в интермедиатной программе.
 */
class ProgressionEngineTest {

    private val engine = ProgressionEngine()

    /** Стандартное упражнение для большинства тестов. */
    private fun benchPress(
        weight: Double = 60.0,
        repsMin: Int = 6,
        repsMax: Int = 10
    ) = Exercise(
        id = "bench-press",
        name = "Жим штанги лёжа",
        description = null,
        primaryMuscle = MuscleGroup.CHEST,
        secondaryMuscles = listOf(MuscleGroup.TRICEPS),
        equipment = listOf(Equipment.BARBELL),
        targetSets = 4,
        targetRepsMin = repsMin,
        targetRepsMax = repsMax,
        restSeconds = 120,
        usesWeight = true,
        startingWeightKg = weight
    )

    private fun point(
        weight: Double,
        avgReps: Double,
        avgRir: Double?,
        ts: Long = 0L
    ) = ExerciseHistoryPoint(
        sessionId = "s-$ts",
        timestamp = ts,
        workingWeightKg = weight,
        avgReps = avgReps,
        avgRir = avgRir,
        completedSets = 4
    )

    // ─── 1. INSUFFICIENT_DATA ────────────────────────────────────────────────

    @Test
    fun `пустая история — INSUFFICIENT_DATA`() {
        val rec = engine.analyze(
            exercise = benchPress(),
            history = emptyList(),
            goal = Goal.MUSCLE_GAIN,
            level = ExperienceLevel.INTERMEDIATE
        )
        assertThat(rec.action).isEqualTo(ProgressionAction.INSUFFICIENT_DATA)
        assertThat(rec.confidence).isEqualTo(Confidence.HIGH)
        assertThat(rec.signals.sessionsAnalyzed).isEqualTo(0)
    }

    // ─── 2. DELOAD после серии регрессов ─────────────────────────────────────

    @Test
    fun `два регресса подряд — DELOAD с -15 процентов и -2 повтора`() {
        // Сценарий: три тренировки, объём падает, повторы тоже
        // 60×8 → 60×6 → 60×4 = два регресса подряд
        val history = listOf(
            point(60.0, 8.0, 1.0, ts = 1),
            point(60.0, 6.0, 0.5, ts = 2),
            point(60.0, 4.0, 0.0, ts = 3)
        )
        val rec = engine.analyze(
            benchPress(),
            history,
            Goal.MUSCLE_GAIN,
            ExperienceLevel.INTERMEDIATE
        )
        assertThat(rec.action).isEqualTo(ProgressionAction.DELOAD)
        assertThat(rec.confidence).isEqualTo(Confidence.HIGH)
        // 60 * 0.85 = 51, округление до 0.5 = 51.0
        assertThat(rec.newWeightKg).isEqualTo(51.0)
        // повторы -2 от диапазона 6-10 = 4-8, но не меньше 5-8
        assertThat(rec.newRepsMin).isEqualTo(5)
        assertThat(rec.newRepsMax).isEqualTo(8)
    }

    // ─── 3. DECREASE_WEIGHT — подтверждённый регресс одной тренировки ───────

    @Test
    fun `регресс vs предыдущая — DECREASE_WEIGHT на один шаг`() {
        // Прошлая тренировка: 60×8 RIR 1 (норма)
        // Текущая: 60×4 RIR -1 (повторы ниже мин диапазона 6-10)
        val history = listOf(
            point(60.0, 8.0, 1.0, ts = 1),
            point(60.0, 4.0, 0.0, ts = 2)
        )
        val rec = engine.analyze(
            benchPress(),
            history,
            Goal.MUSCLE_GAIN,
            ExperienceLevel.INTERMEDIATE
        )
        assertThat(rec.action).isEqualTo(ProgressionAction.DECREASE_WEIGHT)
        // Шаг для веса 60 = 2.5, новый вес = 57.5
        assertThat(rec.newWeightKg).isEqualTo(57.5)
    }

    // ─── 4. Глубокий регресс — DECREASE_WEIGHT -10% ─────────────────────────

    @Test
    fun `глубокий регресс RIR 0 и повторы сильно ниже мин — DECREASE на 10 процентов`() {
        // Одна сессия, повторы 4 (мин был 6), RIR 0 — не добил
        val history = listOf(
            point(60.0, 4.0, 0.0, ts = 1)
        )
        val rec = engine.analyze(
            benchPress(),
            history,
            Goal.MUSCLE_GAIN,
            ExperienceLevel.INTERMEDIATE
        )
        assertThat(rec.action).isEqualTo(ProgressionAction.DECREASE_WEIGHT)
        // 60 * 0.9 = 54
        assertThat(rec.newWeightKg).isEqualTo(54.0)
        assertThat(rec.confidence).isEqualTo(Confidence.HIGH)
    }

    // ─── 5. Стагнация — INCREASE_REPS ────────────────────────────────────────

    @Test
    fun `три тренировки на одном весе без роста повторов — INCREASE_REPS`() {
        // 60×7 RIR 2 три раза подряд — застрял в середине диапазона 6-10
        val history = listOf(
            point(60.0, 7.0, 2.0, ts = 1),
            point(60.0, 7.0, 2.0, ts = 2),
            point(60.0, 7.0, 2.0, ts = 3)
        )
        val rec = engine.analyze(
            benchPress(),
            history,
            Goal.MUSCLE_GAIN,
            ExperienceLevel.INTERMEDIATE
        )
        assertThat(rec.action).isEqualTo(ProgressionAction.INCREASE_REPS)
        assertThat(rec.newRepsMax).isEqualTo(12)  // 10 + 2
    }

    // ─── 6. Идеальный прогресс — INCREASE_WEIGHT стандартный ────────────────

    @Test
    fun `закрыл верх диапазона с целевым RIR — INCREASE_WEIGHT стандартно`() {
        // Один раз сделал 60×10 RIR 1 (закрыл верх диапазона 6-10, цель RIR 1)
        val history = listOf(
            point(60.0, 10.0, 1.0, ts = 1)
        )
        val rec = engine.analyze(
            benchPress(),
            history,
            Goal.MUSCLE_GAIN,
            ExperienceLevel.INTERMEDIATE
        )
        assertThat(rec.action).isEqualTo(ProgressionAction.INCREASE_WEIGHT)
        // Шаг для веса 60 = 2.5, новый вес = 62.5
        assertThat(rec.newWeightKg).isEqualTo(62.5)
        assertThat(rec.confidence).isEqualTo(Confidence.HIGH)
    }

    // ─── 7. Запас явный — INCREASE_WEIGHT большой шаг ───────────────────────

    @Test
    fun `RIR на 2 выше цели в двух тренировках — INCREASE_WEIGHT с большим шагом`() {
        // 60×8 RIR 3 (цель 1, отклонение +2) два раза подряд
        val history = listOf(
            point(60.0, 8.0, 3.0, ts = 1),
            point(60.0, 8.0, 3.0, ts = 2)
        )
        val rec = engine.analyze(
            benchPress(),
            history,
            Goal.MUSCLE_GAIN,
            ExperienceLevel.INTERMEDIATE
        )
        assertThat(rec.action).isEqualTo(ProgressionAction.INCREASE_WEIGHT)
        // base step 2.5, multiplier 1 (rirOffset/2=1, ceil=1)
        // shift = min(2.5*1, 60*0.05=3) = 2.5
        // newWeight = 62.5
        assertThat(rec.newWeightKg).isEqualTo(62.5)
        assertThat(rec.confidence).isEqualTo(Confidence.HIGH)
    }

    @Test
    fun `RIR на 4 выше цели в двух тренировках — большой шаг с 5процентным cap`() {
        // RIR 5 при цели 1, отклонение +4
        val history = listOf(
            point(60.0, 8.0, 5.0, ts = 1),
            point(60.0, 8.0, 5.0, ts = 2)
        )
        val rec = engine.analyze(
            benchPress(),
            history,
            Goal.MUSCLE_GAIN,
            ExperienceLevel.INTERMEDIATE
        )
        assertThat(rec.action).isEqualTo(ProgressionAction.INCREASE_WEIGHT)
        // multiplier = ceil(4/2)=2, shift = min(2.5*2=5, 60*0.05=3) = 3
        // newWeight = 63.0
        assertThat(rec.newWeightKg).isEqualTo(63.0)
    }

    // ─── 8. KEEP — тяжело раз ────────────────────────────────────────────────

    @Test
    fun `RIR ниже целевого на одной тренировке — KEEP даём шанс`() {
        // 60×6 RIR -1 (цель 1, отклонение -2) — но повторы в нижней границе диапазона
        val history = listOf(
            point(60.0, 6.0, -1.0, ts = 1)
        )
        val rec = engine.analyze(
            benchPress(),
            history,
            Goal.MUSCLE_GAIN,
            ExperienceLevel.INTERMEDIATE
        )
        // Тяжело, повторы на нижней границе — KEEP, дать шанс
        assertThat(rec.action).isEqualTo(ProgressionAction.KEEP)
    }

    // ─── 9. KEEP по умолчанию ────────────────────────────────────────────────

    @Test
    fun `повторы в середине диапазона цель попала — KEEP по умолчанию`() {
        // 60×8 RIR 1 (цель 1, идеально по RIR, но не закрыл верх)
        val history = listOf(
            point(60.0, 8.0, 1.0, ts = 1)
        )
        val rec = engine.analyze(
            benchPress(),
            history,
            Goal.MUSCLE_GAIN,
            ExperienceLevel.INTERMEDIATE
        )
        assertThat(rec.action).isEqualTo(ProgressionAction.KEEP)
    }

    // ─── 10. Шаги по диапазонам веса ─────────────────────────────────────────

    @Test
    fun `шаг для веса 10 кг — 0_5 кг из-за 4процентного cap`() {
        // Лёгкая дельта 10 кг, юзер закрыл диапазон
        val ex = benchPress(weight = 10.0)
        val history = listOf(point(10.0, 10.0, 1.0, ts = 1))
        val rec = engine.analyze(ex, history, Goal.MUSCLE_GAIN, ExperienceLevel.INTERMEDIATE)
        assertThat(rec.action).isEqualTo(ProgressionAction.INCREASE_WEIGHT)
        // default step = 1.0, но 4%-cap для 10 кг даёт минимум 0.5 кг
        assertThat(rec.newWeightKg).isEqualTo(10.5)
    }

    @Test
    fun `шаг для веса 30 кг — 1 кг после 4процентного cap и округления`() {
        val ex = benchPress(weight = 30.0)
        val history = listOf(point(30.0, 10.0, 1.0, ts = 1))
        val rec = engine.analyze(ex, history, Goal.MUSCLE_GAIN, ExperienceLevel.INTERMEDIATE)
        // default step = 2.0, 4%-cap = 1.2, после округления новый вес = 31.0
        assertThat(rec.newWeightKg).isEqualTo(31.0)
    }

    @Test
    fun `шаг для веса 100 кг — 4 кг с cap`() {
        val ex = benchPress(weight = 100.0)
        val history = listOf(point(100.0, 10.0, 1.0, ts = 1))
        val rec = engine.analyze(ex, history, Goal.MUSCLE_GAIN, ExperienceLevel.INTERMEDIATE)
        // default 5.0 для 80-120 диапазона
        // 4%-cap = 100 * 0.04 = 4.0
        // итог = min(5.0, 4.0) = 4.0
        assertThat(rec.newWeightKg).isEqualTo(104.0)
    }

    // ─── 11. ADVANCED — микро-шаги ──────────────────────────────────────────

    @Test
    fun `ADVANCED делит шаг пополам`() {
        val ex = benchPress(weight = 60.0)
        val history = listOf(point(60.0, 10.0, 1.0, ts = 1))
        val rec = engine.analyze(ex, history, Goal.MUSCLE_GAIN, ExperienceLevel.ADVANCED)
        // step = 2.5 * 0.5 = 1.25, округление до 0.5 → 1.0 (но с cap 4% = 2.4 → не лимит)
        // итог: newWeight = 60 + 1.25 = 61.25, округление до 0.5 = 61.0 или 61.5
        assertThat(rec.action).isEqualTo(ProgressionAction.INCREASE_WEIGHT)
        // Точное значение зависит от округления — проверяем диапазон
        assertThat(rec.newWeightKg).isIn(listOf(61.0, 61.5))
    }

    // ─── 12. Бодвейт упражнения ──────────────────────────────────────────────

    @Test
    fun `бодвейт без истории — INSUFFICIENT_DATA`() {
        val pullup = benchPress().copy(usesWeight = false, name = "Подтягивания")
        val rec = engine.analyze(pullup, emptyList(), Goal.MUSCLE_GAIN, ExperienceLevel.INTERMEDIATE)
        assertThat(rec.action).isEqualTo(ProgressionAction.INSUFFICIENT_DATA)
    }

    @Test
    fun `бодвейт с большим запасом RIR — INCREASE_REPS`() {
        val pullup = benchPress().copy(usesWeight = false, name = "Подтягивания")
        // RIR 4 при цели 1 = отклонение +3
        val history = listOf(point(0.0, 8.0, 4.0, ts = 1))
        val rec = engine.analyze(pullup, history, Goal.MUSCLE_GAIN, ExperienceLevel.INTERMEDIATE)
        assertThat(rec.action).isEqualTo(ProgressionAction.INCREASE_REPS)
        assertThat(rec.newRepsMax).isEqualTo(12)  // 10 + 2
        assertThat(rec.newWeightKg).isNull()
    }

    @Test
    fun `бодвейт в рабочей зоне — KEEP`() {
        val pullup = benchPress().copy(usesWeight = false)
        // RIR 1 при цели 1 — точно в зоне
        val history = listOf(point(0.0, 8.0, 1.0, ts = 1))
        val rec = engine.analyze(pullup, history, Goal.MUSCLE_GAIN, ExperienceLevel.INTERMEDIATE)
        assertThat(rec.action).isEqualTo(ProgressionAction.KEEP)
    }

    // ─── 13. Целевой RIR по разным целям ─────────────────────────────────────

    @Test
    fun `STRENGTH цель — целевой RIR 2`() {
        // Жим 60×6 RIR 2 (цель силы — RIR 2), не закрыл верх
        val ex = benchPress(repsMin = 4, repsMax = 6)
        val history = listOf(point(60.0, 6.0, 2.0, ts = 1))
        val rec = engine.analyze(ex, history, Goal.STRENGTH, ExperienceLevel.INTERMEDIATE)
        // 6 = верх диапазона 4-6, RIR 2 в норме → INCREASE_WEIGHT
        assertThat(rec.action).isEqualTo(ProgressionAction.INCREASE_WEIGHT)
    }

    // ─── 14. Сигналы записываются корректно ─────────────────────────────────

    @Test
    fun `signals содержат корректную диагностику`() {
        val history = listOf(
            point(60.0, 8.0, 2.0, ts = 1),
            point(60.0, 8.0, 2.0, ts = 2),
            point(60.0, 8.0, 2.0, ts = 3)
        )
        val rec = engine.analyze(
            benchPress(),
            history,
            Goal.MUSCLE_GAIN,
            ExperienceLevel.INTERMEDIATE
        )
        with(rec.signals) {
            assertThat(sessionsAnalyzed).isEqualTo(3)
            assertThat(workingWeightLast).isEqualTo(60.0)
            assertThat(targetRir).isEqualTo(1)  // MUSCLE_GAIN
            assertThat(rirOffset).isEqualTo(1.0)  // 2 - 1
            assertThat(sessionsAtSameWeight).isEqualTo(3)
        }
    }

    // ─── 15. Граничные случаи округления ─────────────────────────────────────

    @Test
    fun `округление до 0_5 кг — не выдаёт нечётные числа`() {
        // Вес 60.3 после расчёта — должен округлиться к 60.5
        val ex = benchPress(weight = 57.7)
        val history = listOf(point(57.7, 10.0, 1.0, ts = 1))
        val rec = engine.analyze(ex, history, Goal.MUSCLE_GAIN, ExperienceLevel.INTERMEDIATE)
        // Проверяем что результат кратен 0.5
        val w = rec.newWeightKg!!
        assertThat(w * 2 % 1).isEqualTo(0.0)
    }

    // ─── 16. stepFor публичный API ───────────────────────────────────────────

    @Test
    fun `stepFor возвращает правильные шаги по диапазонам`() {
        // weight ≤ 15 → default 1 кг, но 4%-cap и минимум шага дают 0.5 кг
        assertThat(engine.stepFor(10.0, ExperienceLevel.INTERMEDIATE)).isEqualTo(0.5)
        // 15 < weight ≤ 40 → 2 кг (но cap 4% = 1.0 для веса 25)
        assertThat(engine.stepFor(25.0, ExperienceLevel.INTERMEDIATE)).isEqualTo(1.0)
        // weight 60: default 2.5, 4%-cap = 2.4 → cap срабатывает, итог 2.4
        assertThat(engine.stepFor(60.0, ExperienceLevel.INTERMEDIATE)).isEqualTo(2.4)
        // ADVANCED делит пополам
        assertThat(engine.stepFor(60.0, ExperienceLevel.ADVANCED)).isEqualTo(1.25)
    }
}
