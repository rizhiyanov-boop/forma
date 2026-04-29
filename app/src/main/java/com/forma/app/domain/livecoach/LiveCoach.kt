package com.forma.app.domain.livecoach

import com.forma.app.domain.model.SetLog
import kotlin.math.abs

data class WellnessSnapshot(
    val preWorkoutEnergy: com.forma.app.domain.wellness.EnergyLevel? = null,
    val betweenExerciseEnergy: com.forma.app.domain.wellness.EnergyLevel? = null,
    val sevenDayAvgEnergy: Double? = null,
    val sevenDayAvgSleep: Double? = null
)

data class SetContext(
    val setNumber: Int,
    val totalSets: Int,
    val targetReps: IntRange,
    val targetRir: Int,
    val plannedWeight: Double?,
    val actualReps: Int,
    val actualRir: Int?,
    val actualWeight: Double?,
    val previousSetsThisExercise: List<SetLog>,
    val currentWellness: WellnessSnapshot?
)

enum class SetVerdict {
    ON_TARGET,
    EASY,
    HEAVY,
    TOO_EASY,
    TOO_HEAVY,
    UNUSUAL,
    INSUFFICIENT
}

enum class Confidence {
    LOW, MEDIUM, HIGH
}

enum class WeightAdjustment {
    INCREASE, DECREASE, KEEP, MORE_REST
}

data class NextSetTip(
    val type: WeightAdjustment,
    val newWeight: Double?,
    val newRestSeconds: Int?,
    val rationale: String,
    val requiresConfirmation: Boolean = true
)

data class SetReaction(
    val verdict: SetVerdict,
    val confidence: Confidence,
    val shortMessage: String,
    val nextSetSuggestion: NextSetTip?
)

class LiveCoach {
    fun analyze(context: SetContext, suggestedStep: Double): SetReaction {
        val rir = context.actualRir ?: return SetReaction(
            verdict = SetVerdict.INSUFFICIENT,
            confidence = Confidence.LOW,
            shortMessage = "RIR не указан, продолжаем по плану",
            nextSetSuggestion = null
        )

        val inRepsRange = context.actualReps in context.targetReps
        val rirDelta = rir - context.targetRir
        val afterSecondWorkingSet = context.setNumber >= 2
        val plannedWeight = context.plannedWeight ?: context.actualWeight

        val verdict = when {
            inRepsRange && abs(rirDelta) <= 1 -> SetVerdict.ON_TARGET
            rirDelta >= 3 && context.actualReps > context.targetReps.last -> SetVerdict.TOO_EASY
            rirDelta >= 2 -> SetVerdict.EASY
            rir == 0 && context.actualReps < context.targetReps.first -> SetVerdict.TOO_HEAVY
            rirDelta <= -2 && context.actualReps < context.targetReps.first -> SetVerdict.HEAVY
            !inRepsRange && abs(rirDelta) <= 1 -> SetVerdict.UNUSUAL
            else -> SetVerdict.HEAVY
        }

        if (verdict == SetVerdict.ON_TARGET) {
            return SetReaction(verdict, Confidence.HIGH, "В плане, RIR в норме", null)
        }

        val wellness = context.currentWellness
        val currentEnergy = wellness?.betweenExerciseEnergy ?: wellness?.preWorkoutEnergy
        val fatiguedContext = currentEnergy == com.forma.app.domain.wellness.EnergyLevel.FATIGUED ||
            ((wellness?.sevenDayAvgEnergy ?: 2.0) < 1.8) ||
            ((wellness?.sevenDayAvgSleep ?: 3.0) < 2.5)

        val suggestion = when {
            !afterSecondWorkingSet -> null
            plannedWeight == null -> null
            verdict == SetVerdict.TOO_EASY || verdict == SetVerdict.EASY -> NextSetTip(
                type = WeightAdjustment.INCREASE,
                newWeight = plannedWeight + suggestedStep,
                newRestSeconds = null,
                rationale = "Есть запас, попробуй добавить вес в следующем подходе"
            )
            (verdict == SetVerdict.TOO_HEAVY || verdict == SetVerdict.HEAVY) && !fatiguedContext -> NextSetTip(
                type = WeightAdjustment.DECREASE,
                newWeight = (plannedWeight - suggestedStep).coerceAtLeast(0.0),
                newRestSeconds = null,
                rationale = "Слишком тяжело, лучше немного снизить вес"
            )
            (verdict == SetVerdict.TOO_HEAVY || verdict == SetVerdict.HEAVY) && fatiguedContext -> NextSetTip(
                type = WeightAdjustment.KEEP,
                newWeight = plannedWeight,
                newRestSeconds = 30,
                rationale = "На фоне усталости лучше оставить вес и добавить отдых"
            )
            else -> null
        }

        val message = when (verdict) {
            SetVerdict.EASY -> "Легко прошло, есть запас"
            SetVerdict.TOO_EASY -> "Слишком легко для целевого стимула"
            SetVerdict.HEAVY -> "Тяжелее цели, но контролируемо"
            SetVerdict.TOO_HEAVY -> "Слишком тяжело, стоит скорректировать"
            SetVerdict.UNUSUAL -> "Необычный паттерн, лучше идти аккуратно"
            SetVerdict.INSUFFICIENT -> "Недостаточно данных"
            SetVerdict.ON_TARGET -> "В плане, RIR в норме"
        }
        val confidence = when (verdict) {
            SetVerdict.TOO_EASY, SetVerdict.TOO_HEAVY -> Confidence.HIGH
            SetVerdict.EASY, SetVerdict.HEAVY -> Confidence.MEDIUM
            else -> Confidence.LOW
        }

        return SetReaction(
            verdict = verdict,
            confidence = confidence,
            shortMessage = message,
            nextSetSuggestion = suggestion
        )
    }
}
