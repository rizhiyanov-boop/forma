package com.forma.app.domain.coaching

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SanityValidatorTest {

    private val validator = SanityValidator()

    private fun rec(
        action: ProgressionAction,
        newWeight: Double? = null
    ) = EngineRecommendation(
        exerciseId = "ex-1",
        exerciseName = "Жим",
        action = action,
        confidence = Confidence.HIGH,
        newWeightKg = newWeight,
        rationale = "test",
        signals = ProgressionSignals(
            sessionsAnalyzed = 3,
            avgRirLast = 1.0,
            avgRepsLast = 8.0,
            workingWeightLast = 60.0,
            targetRir = 1,
            rirOffset = 0.0,
            hitTopOfRange = false,
            sessionsAtSameWeight = 1,
            regressStreak = 0
        )
    )

    @Test
    fun `INCREASE_WEIGHT в пределах 5процентов — не disputed`() {
        // 60 → 62.5 = +4.17%, в норме
        val r = validator.validate(
            rec(ProgressionAction.INCREASE_WEIGHT, newWeight = 62.5),
            currentWeight = 60.0,
            daysSinceLastDeload = null
        )
        assertThat(r.isDisputed).isFalse()
        assertThat(r.rec.newWeightKg).isEqualTo(62.5)
    }

    @Test
    fun `INCREASE_WEIGHT более 5процентов — урезаем и помечаем disputed`() {
        // 60 → 70 = +16.7%, должно урезаться до 60*1.05 = 63
        val r = validator.validate(
            rec(ProgressionAction.INCREASE_WEIGHT, newWeight = 70.0),
            currentWeight = 60.0,
            daysSinceLastDeload = null
        )
        assertThat(r.isDisputed).isTrue()
        assertThat(r.rec.newWeightKg).isEqualTo(63.0)
        assertThat(r.rec.confidence).isEqualTo(Confidence.LOW)
    }

    @Test
    fun `DECREASE_WEIGHT в пределах 15процентов — не disputed`() {
        // 60 → 54 = -10%, в норме
        val r = validator.validate(
            rec(ProgressionAction.DECREASE_WEIGHT, newWeight = 54.0),
            currentWeight = 60.0,
            daysSinceLastDeload = null
        )
        assertThat(r.isDisputed).isFalse()
    }

    @Test
    fun `DECREASE_WEIGHT более 15процентов — урезаем`() {
        // 60 → 40 = -33%, должно урезаться до 60*0.85 = 51
        val r = validator.validate(
            rec(ProgressionAction.DECREASE_WEIGHT, newWeight = 40.0),
            currentWeight = 60.0,
            daysSinceLastDeload = null
        )
        assertThat(r.isDisputed).isTrue()
        assertThat(r.rec.newWeightKg).isEqualTo(51.0)
    }

    @Test
    fun `DELOAD недавно был — конвертируем в KEEP`() {
        val r = validator.validate(
            rec(ProgressionAction.DELOAD, newWeight = 51.0),
            currentWeight = 60.0,
            daysSinceLastDeload = 14   // 2 недели назад
        )
        assertThat(r.isDisputed).isTrue()
        assertThat(r.rec.action).isEqualTo(ProgressionAction.KEEP)
        assertThat(r.rec.newWeightKg).isNull()
    }

    @Test
    fun `DELOAD после 4 недель — пропускаем`() {
        val r = validator.validate(
            rec(ProgressionAction.DELOAD, newWeight = 51.0),
            currentWeight = 60.0,
            daysSinceLastDeload = 35
        )
        assertThat(r.isDisputed).isFalse()
        assertThat(r.rec.action).isEqualTo(ProgressionAction.DELOAD)
    }

    @Test
    fun `KEEP всегда проходит без проверки`() {
        val r = validator.validate(
            rec(ProgressionAction.KEEP),
            currentWeight = 60.0,
            daysSinceLastDeload = 5
        )
        assertThat(r.isDisputed).isFalse()
    }
}
