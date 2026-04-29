package com.forma.app.presentation.screens.workout

import com.forma.app.data.local.dao.SetReactionDao
import com.forma.app.data.local.entity.SetReactionEntity
import com.forma.app.domain.livecoach.SetVerdict
import com.forma.app.domain.wellness.WellnessEntry
import com.forma.app.domain.wellness.WellnessRepository
import com.forma.app.domain.wellness.WellnessTriggerType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test

class BetweenExercisePickerLogicTest {

    @Test
    fun shouldShow_returnsFalse_afterFirstExercise() = runBlocking {
        val logic = logic()

        val result = logic.shouldShowBetweenPicker(
            sessionId = "s1",
            completedExerciseIndex = 0,
            totalExercises = 5,
            lastExerciseHadStrongSignal = false
        )

        assertThat(result).isFalse()
    }

    @Test
    fun shouldShow_returnsTrue_onStrongSignal() = runBlocking {
        val logic = logic()

        val result = logic.shouldShowBetweenPicker(
            sessionId = "s1",
            completedExerciseIndex = 1,
            totalExercises = 5,
            lastExerciseHadStrongSignal = true
        )

        assertThat(result).isTrue()
    }

    @Test
    fun shouldShow_returnsTrue_whenThreeOrMoreExercisesSinceLastPicker() = runBlocking {
        val logic = logic(
            betweenEntries = listOf(betweenEntry(afterIndex = 0, timestamp = 100L))
        )

        val result = logic.shouldShowBetweenPicker(
            sessionId = "s1",
            completedExerciseIndex = 4,
            totalExercises = 6,
            lastExerciseHadStrongSignal = false
        )

        assertThat(result).isTrue()
    }

    @Test
    fun shouldShow_returnsFalse_whenPickerWasRecentAndNoStrongSignal() = runBlocking {
        val logic = logic(
            betweenEntries = listOf(betweenEntry(afterIndex = 2, timestamp = 100L))
        )

        val result = logic.shouldShowBetweenPicker(
            sessionId = "s1",
            completedExerciseIndex = 3,
            totalExercises = 8,
            lastExerciseHadStrongSignal = false
        )

        assertThat(result).isFalse()
    }

    @Test
    fun shouldShow_returnsTrue_atWorkoutMidpoint() = runBlocking {
        val logic = logic()

        val result = logic.shouldShowBetweenPicker(
            sessionId = "s1",
            completedExerciseIndex = 2,
            totalExercises = 5,
            lastExerciseHadStrongSignal = false
        )

        assertThat(result).isTrue()
    }

    @Test
    fun shouldShow_returnsFalse_whenAlreadyAskedForSameTransition() = runBlocking {
        val logic = logic(
            betweenEntries = listOf(betweenEntry(afterIndex = 3, timestamp = 100L))
        )

        val result = logic.shouldShowBetweenPicker(
            sessionId = "s1",
            completedExerciseIndex = 3,
            totalExercises = 6,
            lastExerciseHadStrongSignal = true
        )

        assertThat(result).isFalse()
    }

    @Test
    fun lastExerciseHadStrongSignal_returnsTrue_forTooHeavy() = runBlocking {
        val logic = logic(
            reactions = listOf(reaction(verdict = SetVerdict.TOO_HEAVY))
        )

        val result = logic.lastExerciseHadStrongSignal(sessionId = "s1", exerciseId = "e1")

        assertThat(result).isTrue()
    }

    @Test
    fun lastExerciseHadStrongSignal_returnsFalse_forOnTargetOnly() = runBlocking {
        val logic = logic(
            reactions = listOf(reaction(verdict = SetVerdict.ON_TARGET))
        )

        val result = logic.lastExerciseHadStrongSignal(sessionId = "s1", exerciseId = "e1")

        assertThat(result).isFalse()
    }

    private fun logic(
        betweenEntries: List<WellnessEntry> = emptyList(),
        reactions: List<SetReactionEntity> = emptyList()
    ) = BetweenExercisePickerLogic(
        wellnessRepo = FakeWellnessRepository(betweenEntries),
        setReactionDao = FakeSetReactionDao(reactions)
    )

    private class FakeWellnessRepository(
        private val betweenEntries: List<WellnessEntry>
    ) : WellnessRepository {
        override suspend fun save(entry: WellnessEntry) = Unit
        override suspend fun getPreWorkoutFor(sessionId: String): WellnessEntry? = null
        override suspend fun getPostWorkoutFor(sessionId: String): WellnessEntry? = null
        override suspend fun getBetweenExercisesFor(sessionId: String): List<WellnessEntry> =
            betweenEntries
                .filter { it.sessionId == sessionId }
                .sortedByDescending { it.timestamp }
        override fun observeRecent(daysBack: Int): Flow<List<WellnessEntry>> =
            MutableStateFlow(emptyList())
        override suspend fun avgEnergyLast(daysBack: Int): Double? = null
    }

    private class FakeSetReactionDao(
        private val reactions: List<SetReactionEntity>
    ) : SetReactionDao {
        override suspend fun upsert(entity: SetReactionEntity) = Unit
        override suspend fun findBySetId(setLogId: String): SetReactionEntity? = null
        override suspend fun declinedCountForExercise(sessionId: String, exerciseId: String): Int = 0
        override suspend fun findByExerciseInSession(
            sessionId: String,
            exerciseId: String
        ): List<SetReactionEntity> = reactions.filter {
            it.sessionId == sessionId && it.exerciseId == exerciseId
        }
        override suspend fun markAccepted(setLogId: String, accepted: Boolean) = Unit
    }

    private companion object {
        fun betweenEntry(afterIndex: Int, timestamp: Long) = WellnessEntry(
            sessionId = "s1",
            timestamp = timestamp,
            type = WellnessTriggerType.BETWEEN_EXERCISES,
            energy = null,
            notes = afterIndex.toString()
        )

        fun reaction(verdict: SetVerdict) = SetReactionEntity(
            setLogId = verdict.name,
            sessionId = "s1",
            exerciseId = "e1",
            verdict = verdict.name,
            nextSuggestionType = null,
            nextSuggestedWeight = null,
            userAcceptedSuggestion = null,
            createdAt = 0L
        )
    }
}
