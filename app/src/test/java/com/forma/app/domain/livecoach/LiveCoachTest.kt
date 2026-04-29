package com.forma.app.domain.livecoach

import com.forma.app.domain.model.SetLog
import com.forma.app.domain.wellness.EnergyLevel
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LiveCoachTest {

    private val coach = LiveCoach()

    @Test
    fun analyze_returnsInsufficient_whenRirIsNull() {
        val result = coach.analyze(
            ctx(actualReps = 8, actualRir = null),
            suggestedStep = 2.5
        )

        assertThat(result.verdict).isEqualTo(SetVerdict.INSUFFICIENT)
        assertThat(result.confidence).isEqualTo(Confidence.LOW)
        assertThat(result.nextSetSuggestion).isNull()
    }

    @Test
    fun analyze_returnsOnTarget_whenRepsAndRirAreInPlan() {
        val result = coach.analyze(
            ctx(actualReps = 8, actualRir = 1),
            suggestedStep = 2.5
        )

        assertThat(result.verdict).isEqualTo(SetVerdict.ON_TARGET)
        assertThat(result.shortMessage).isEqualTo("В плане, RIR в норме")
        assertThat(result.nextSetSuggestion).isNull()
    }

    @Test
    fun analyze_returnsEasyAndIncrease_onSecondSet_whenRirIsHigh() {
        val result = coach.analyze(
            ctx(setNumber = 2, actualReps = 10, actualRir = 3),
            suggestedStep = 2.5
        )

        assertThat(result.verdict).isEqualTo(SetVerdict.EASY)
        assertThat(result.nextSetSuggestion?.type).isEqualTo(WeightAdjustment.INCREASE)
        assertThat(result.nextSetSuggestion?.newWeight).isEqualTo(62.5)
    }

    @Test
    fun analyze_returnsEasyWithoutSuggestion_onFirstSet_whenRirIsHigh() {
        val result = coach.analyze(
            ctx(setNumber = 1, actualReps = 10, actualRir = 3),
            suggestedStep = 2.5
        )

        assertThat(result.verdict).isEqualTo(SetVerdict.EASY)
        assertThat(result.nextSetSuggestion).isNull()
    }

    @Test
    fun analyze_returnsTooEasyAndIncrease_whenRirHighAndRepsAbovePlan() {
        val result = coach.analyze(
            ctx(actualReps = 12, actualRir = 4),
            suggestedStep = 2.5
        )

        assertThat(result.verdict).isEqualTo(SetVerdict.TOO_EASY)
        assertThat(result.nextSetSuggestion?.type).isEqualTo(WeightAdjustment.INCREASE)
        assertThat(result.nextSetSuggestion?.newWeight).isEqualTo(62.5)
    }

    @Test
    fun analyze_returnsHeavyAndDecrease_whenRirLowAndRepsBelowPlan() {
        val result = coach.analyze(
            ctx(actualReps = 5, actualRir = -1),
            suggestedStep = 2.5
        )

        assertThat(result.verdict).isEqualTo(SetVerdict.HEAVY)
        assertThat(result.nextSetSuggestion?.type).isEqualTo(WeightAdjustment.DECREASE)
        assertThat(result.nextSetSuggestion?.newWeight).isEqualTo(57.5)
    }

    @Test
    fun analyze_returnsTooHeavyAndDecrease_whenRirZeroAndRepsBelowPlan() {
        val result = coach.analyze(
            ctx(actualReps = 4, actualRir = 0),
            suggestedStep = 2.5
        )

        assertThat(result.verdict).isEqualTo(SetVerdict.TOO_HEAVY)
        assertThat(result.nextSetSuggestion?.type).isEqualTo(WeightAdjustment.DECREASE)
    }

    @Test
    fun analyze_returnsUnusual_whenRepsOutsidePlanButRirInTarget() {
        val result = coach.analyze(
            ctx(actualReps = 12, actualRir = 1),
            suggestedStep = 2.5
        )

        assertThat(result.verdict).isEqualTo(SetVerdict.UNUSUAL)
        assertThat(result.confidence).isEqualTo(Confidence.LOW)
        assertThat(result.nextSetSuggestion).isNull()
    }

    @Test
    fun analyze_blocksSuggestion_onFirstSet_evenWhenHeavy() {
        val result = coach.analyze(
            ctx(setNumber = 1, actualReps = 5, actualRir = -1),
            suggestedStep = 2.5
        )

        assertThat(result.verdict).isEqualTo(SetVerdict.HEAVY)
        assertThat(result.nextSetSuggestion).isNull()
    }

    @Test
    fun analyze_keepsWeightAndAddsRest_whenFatiguedAndHeavy() {
        val result = coach.analyze(
            ctx(
                actualReps = 5,
                actualRir = -1,
                wellness = WellnessSnapshot(preWorkoutEnergy = EnergyLevel.FATIGUED)
            ),
            suggestedStep = 2.5
        )

        assertThat(result.verdict).isEqualTo(SetVerdict.HEAVY)
        assertThat(result.nextSetSuggestion?.type).isEqualTo(WeightAdjustment.KEEP)
        assertThat(result.nextSetSuggestion?.newRestSeconds).isEqualTo(30)
        assertThat(result.nextSetSuggestion?.rationale).contains("усталости")
    }

    @Test
    fun analyze_usesFatigueContext_whenSevenDaySleepIsLow() {
        val result = coach.analyze(
            ctx(
                actualReps = 5,
                actualRir = -1,
                wellness = WellnessSnapshot(sevenDayAvgSleep = 2.0)
            ),
            suggestedStep = 2.5
        )

        assertThat(result.verdict).isEqualTo(SetVerdict.HEAVY)
        assertThat(result.nextSetSuggestion?.type).isEqualTo(WeightAdjustment.KEEP)
    }

    @Test
    fun analyze_usesFatigueContext_whenSevenDayEnergyIsLow() {
        val result = coach.analyze(
            ctx(
                actualReps = 5,
                actualRir = -1,
                wellness = WellnessSnapshot(sevenDayAvgEnergy = 1.5)
            ),
            suggestedStep = 2.5
        )

        assertThat(result.verdict).isEqualTo(SetVerdict.HEAVY)
        assertThat(result.nextSetSuggestion?.type).isEqualTo(WeightAdjustment.KEEP)
    }

    @Test
    fun analyze_decreasesWeight_whenEnergizedAndHeavy() {
        val result = coach.analyze(
            ctx(
                actualReps = 5,
                actualRir = -1,
                wellness = WellnessSnapshot(preWorkoutEnergy = EnergyLevel.ENERGIZED)
            ),
            suggestedStep = 2.5
        )

        assertThat(result.verdict).isEqualTo(SetVerdict.HEAVY)
        assertThat(result.nextSetSuggestion?.type).isEqualTo(WeightAdjustment.DECREASE)
    }

    @Test
    fun analyze_doesNotSuggestWeightChange_whenPlannedWeightIsNull() {
        val result = coach.analyze(
            ctx(
                plannedWeight = null,
                actualWeight = null,
                actualReps = 10,
                actualRir = 3
            ),
            suggestedStep = 2.5
        )

        assertThat(result.verdict).isEqualTo(SetVerdict.EASY)
        assertThat(result.nextSetSuggestion).isNull()
    }

    @Test
    fun analyze_increaseUsesSuggestedStep() {
        val result = coach.analyze(
            ctx(actualReps = 10, actualRir = 3),
            suggestedStep = 4.0
        )

        assertThat(result.nextSetSuggestion?.type).isEqualTo(WeightAdjustment.INCREASE)
        assertThat(result.nextSetSuggestion?.newWeight).isEqualTo(64.0)
    }

    @Test
    fun analyze_decreaseDoesNotGoBelowZero() {
        val result = coach.analyze(
            ctx(plannedWeight = 1.0, actualReps = 5, actualRir = -1),
            suggestedStep = 2.5
        )

        assertThat(result.verdict).isEqualTo(SetVerdict.HEAVY)
        assertThat(result.nextSetSuggestion?.type).isEqualTo(WeightAdjustment.DECREASE)
        assertThat(result.nextSetSuggestion?.newWeight).isEqualTo(0.0)
    }

    @Test
    fun analyze_usesBetweenExerciseEnergyBeforePreWorkoutEnergy() {
        val result = coach.analyze(
            ctx(
                actualReps = 5,
                actualRir = -1,
                wellness = WellnessSnapshot(
                    preWorkoutEnergy = EnergyLevel.ENERGIZED,
                    betweenExerciseEnergy = EnergyLevel.FATIGUED
                )
            ),
            suggestedStep = 2.5
        )

        assertThat(result.verdict).isEqualTo(SetVerdict.HEAVY)
        assertThat(result.nextSetSuggestion?.type).isEqualTo(WeightAdjustment.KEEP)
    }

    @Test
    fun analyze_fallsBackToPreWorkoutEnergy_whenBetweenExerciseEnergyIsNull() {
        val result = coach.analyze(
            ctx(
                actualReps = 5,
                actualRir = -1,
                wellness = WellnessSnapshot(
                    preWorkoutEnergy = EnergyLevel.FATIGUED,
                    betweenExerciseEnergy = null
                )
            ),
            suggestedStep = 2.5
        )

        assertThat(result.verdict).isEqualTo(SetVerdict.HEAVY)
        assertThat(result.nextSetSuggestion?.type).isEqualTo(WeightAdjustment.KEEP)
    }

    private fun ctx(
        setNumber: Int = 2,
        targetReps: IntRange = 6..10,
        targetRir: Int = 1,
        plannedWeight: Double? = 60.0,
        actualReps: Int,
        actualRir: Int?,
        actualWeight: Double? = plannedWeight,
        previousSets: List<SetLog> = emptyList(),
        wellness: WellnessSnapshot? = null
    ) = SetContext(
        setNumber = setNumber,
        totalSets = 3,
        targetReps = targetReps,
        targetRir = targetRir,
        plannedWeight = plannedWeight,
        actualReps = actualReps,
        actualRir = actualRir,
        actualWeight = actualWeight,
        previousSetsThisExercise = previousSets,
        currentWellness = wellness
    )
}
