package com.forma.app.domain.usecase

import com.forma.app.domain.model.ExerciseFeedback

data class AdaptationResult(
    val reduceSetsBy: Int = 0,
    val weightMultiplier: Double = 1.0,
    val suggestReplacement: Boolean = false,
    val showTip: Boolean = false
)

class AdaptWorkoutUseCase {

    fun execute(feedback: ExerciseFeedback): AdaptationResult {
        return AdaptationResult(
            reduceSetsBy = if (feedback.fatigue >= 4) 1 else 0,
            weightMultiplier = if (feedback.fatigue == 5) 0.9 else 1.0,
            suggestReplacement = feedback.pain,
            showTip = feedback.pump <= 2
        )
    }
}