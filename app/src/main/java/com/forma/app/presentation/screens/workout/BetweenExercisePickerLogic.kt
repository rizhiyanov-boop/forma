package com.forma.app.presentation.screens.workout

import com.forma.app.data.local.dao.SetReactionDao
import com.forma.app.domain.livecoach.SetVerdict
import com.forma.app.domain.wellness.WellnessRepository

internal class BetweenExercisePickerLogic(
    private val wellnessRepo: WellnessRepository,
    private val setReactionDao: SetReactionDao
) {
    suspend fun shouldShowBetweenPicker(
        sessionId: String,
        completedExerciseIndex: Int,
        totalExercises: Int,
        lastExerciseHadStrongSignal: Boolean
    ): Boolean {
        if (completedExerciseIndex == 0) return false

        val betweens = wellnessRepo.getBetweenExercisesFor(sessionId)
        val lastBetweenAfterExerciseIndex = betweens.firstOrNull()?.notes?.toIntOrNull()
        if (lastBetweenAfterExerciseIndex == completedExerciseIndex) return false

        if (lastExerciseHadStrongSignal) return true

        val sinceLast = lastBetweenAfterExerciseIndex
            ?.let { completedExerciseIndex - it }
            ?: completedExerciseIndex
        if (sinceLast >= 3) return true

        val midpoint = totalExercises / 2
        if (completedExerciseIndex == midpoint) return true

        return false
    }

    suspend fun lastExerciseHadStrongSignal(sessionId: String, exerciseId: String): Boolean {
        return setReactionDao.findByExerciseInSession(sessionId, exerciseId).any {
            it.verdict == SetVerdict.TOO_HEAVY.name || it.verdict == SetVerdict.TOO_EASY.name
        }
    }
}
