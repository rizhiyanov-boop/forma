package com.forma.app.presentation.screens.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forma.app.data.local.dao.SetReactionDao
import com.forma.app.data.local.entity.SetReactionEntity
import com.forma.app.domain.coaching.ProgressionEngine
import com.forma.app.domain.livecoach.CoachContentItem
import com.forma.app.domain.livecoach.CoachContentRepository
import com.forma.app.domain.livecoach.CoachContentType
import com.forma.app.domain.livecoach.LiveCoach
import com.forma.app.domain.livecoach.NextSetTip
import com.forma.app.domain.livecoach.SetContext
import com.forma.app.domain.livecoach.SetReaction
import com.forma.app.domain.livecoach.SetVerdict
import com.forma.app.domain.livecoach.WellnessSnapshot
import com.forma.app.domain.model.Equipment
import com.forma.app.domain.model.ExperienceLevel
import com.forma.app.domain.model.SetLog
import com.forma.app.domain.model.WorkoutSession
import com.forma.app.domain.repository.ProgramRepository
import com.forma.app.domain.repository.SessionRepository
import com.forma.app.domain.repository.UserProfileRepository
import com.forma.app.domain.review.ReviewRepository
import com.forma.app.domain.wellness.EnergyLevel
import com.forma.app.domain.wellness.WellnessEntry
import com.forma.app.domain.wellness.WellnessRepository
import com.forma.app.domain.wellness.WellnessTriggerType
import com.forma.app.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutUi(
    val restCountdown: Int? = null,
    val isFinishing: Boolean = false,
    val showPreWorkoutPicker: Boolean = false,
    val pendingSetReaction: SetReaction? = null,
    val pendingReactionSetId: String? = null,
    val pendingReactionExerciseId: String? = null,
    val pendingReactionSetNumber: Int? = null,
    val pendingReactionStatus: String? = null, // PENDING | APPLIED | DECLINED
    val showBetweenExercisePicker: Boolean = false,
    val factItems: List<CoachContentItem> = emptyList(),
    val techniqueItems: List<CoachContentItem> = emptyList(),
    val motivationItems: List<CoachContentItem> = emptyList(),
    val factIndex: Int = 0,
    val techniqueIndex: Int = 0,
    val motivationIndex: Int = 0,
    val restCardRotationOffset: Int = 0
) {
    val currentFact: CoachContentItem?
        get() = factItems.getOrNull(factIndex)

    val currentTechnique: CoachContentItem?
        get() = techniqueItems.getOrNull(techniqueIndex)

    val currentMotivation: CoachContentItem?
        get() = motivationItems.getOrNull(motivationIndex)
}

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val sessionRepo: SessionRepository,
    private val programRepo: ProgramRepository,
    private val profileRepo: UserProfileRepository,
    private val reviewRepo: ReviewRepository,
    private val coachRepo: CoachContentRepository,
    private val wellnessRepo: WellnessRepository,
    private val setReactionDao: SetReactionDao,
    private val backup: com.forma.app.data.backup.AppBackupService
) : ViewModel() {

    val sessionId: String = savedState[Route.ARG_SESSION_ID] ?: ""
    private val progressionEngine = ProgressionEngine()
    private val liveCoach = LiveCoach()
    private val betweenPickerLogic = BetweenExercisePickerLogic(wellnessRepo, setReactionDao)
    private val shownInThisSession = mutableSetOf<String>()
    private var restTimerJob: Job? = null
    private val profile = profileRepo.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val session: StateFlow<WorkoutSession?> = sessionRepo.observeSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _ui = MutableStateFlow(WorkoutUi())
    val ui: StateFlow<WorkoutUi> = _ui.asStateFlow()

    init {
        checkPreWorkoutPicker()
    }

    fun updateSet(setId: String, reps: Int, weight: Double?, rir: Int?, completed: Boolean) {
        viewModelScope.launch {
            sessionRepo.updateSet(sessionId, setId, reps, weight, rir, completed)
        }
    }

    fun completeSetAndAnalyze(
        exerciseId: String,
        setId: String,
        reps: Int,
        weight: Double?,
        rir: Int?,
        onAnalyzed: (SetReaction) -> Unit
    ) {
        viewModelScope.launch {
            sessionRepo.updateSet(
                sessionId = sessionId,
                setId = setId,
                reps = reps,
                weightKg = weight,
                rir = rir,
                completed = true
            )
            val reaction = analyzeSetAndPrepareReactionInternal(exerciseId, setId) ?: return@launch
            onAnalyzed(reaction)
        }
    }

    fun clearPendingSetReaction() {
        _ui.update {
            it.copy(
                pendingSetReaction = null,
                pendingReactionSetId = null,
                pendingReactionExerciseId = null,
                pendingReactionSetNumber = null,
                pendingReactionStatus = null
            )
        }
    }

    fun analyzeSetAndPrepareReaction(
        exerciseId: String,
        setId: String
    ) {
        viewModelScope.launch {
            analyzeSetAndPrepareReactionInternal(exerciseId, setId)
        }
    }

    private suspend fun analyzeSetAndPrepareReactionInternal(
        exerciseId: String,
        setId: String
    ): SetReaction? {
            val s = sessionRepo.getSession(sessionId) ?: return null
            val exLog = s.exerciseLogs.firstOrNull { it.exerciseId == exerciseId } ?: return null
            val currentSet = exLog.sets.firstOrNull { it.id == setId } ?: return null
            val setNumber = currentSet.setNumber
            val targetRir = getTargetRirForSet(exerciseId, setNumber) ?: 2
            val targetRange = ((currentSet.targetRepsMin ?: 0)..(currentSet.targetRepsMax ?: Int.MAX_VALUE))

            val pre = wellnessRepo.getPreWorkoutFor(sessionId)?.energy
            val latestBetween = wellnessRepo.getBetweenExercisesFor(sessionId).firstOrNull()?.energy
            val avgEnergy = wellnessRepo.avgEnergyLast(7)
            val avgSleep = wellnessRepo.observeRecent(7).first()
                .mapNotNull { it.sleepQuality?.toDouble() }
                .let { if (it.isEmpty()) null else it.average() }

            val suggestedStep = smartWeightStepFor(exerciseId, currentSet.weightKg ?: 0.0)
            val reaction = liveCoach.analyze(
                context = SetContext(
                    setNumber = setNumber,
                    totalSets = exLog.sets.size,
                    targetReps = targetRange,
                    targetRir = targetRir,
                    plannedWeight = currentSet.targetWeightKg ?: currentSet.weightKg,
                    actualReps = currentSet.reps,
                    actualRir = currentSet.rir,
                    actualWeight = currentSet.weightKg,
                    previousSetsThisExercise = exLog.sets.filter { it.setNumber < setNumber && it.isCompleted },
                    currentWellness = WellnessSnapshot(
                        preWorkoutEnergy = pre,
                        betweenExerciseEnergy = latestBetween,
                        sevenDayAvgEnergy = avgEnergy,
                        sevenDayAvgSleep = avgSleep
                    )
                ),
                suggestedStep = suggestedStep
            )

            if (reaction.verdict == com.forma.app.domain.livecoach.SetVerdict.ON_TARGET) {
                clearPendingSetReaction()
                return reaction
            } else {
                setReactionDao.upsert(
                    SetReactionEntity(
                        setLogId = setId,
                        sessionId = sessionId,
                        exerciseId = exerciseId,
                        verdict = reaction.verdict.name,
                        nextSuggestionType = reaction.nextSetSuggestion?.type?.name,
                        nextSuggestedWeight = reaction.nextSetSuggestion?.newWeight,
                        userAcceptedSuggestion = null,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            val declinedEarlier = setReactionDao.declinedCountForExercise(sessionId, exerciseId) > 0
            val visibleReaction = if (declinedEarlier) reaction.copy(nextSetSuggestion = null) else reaction

            _ui.update {
                it.copy(
                    pendingSetReaction = visibleReaction,
                    pendingReactionSetId = setId,
                    pendingReactionExerciseId = exerciseId,
                    pendingReactionSetNumber = setNumber,
                    pendingReactionStatus = "PENDING"
                )
            }
            return reaction
    }

    fun acceptPendingSuggestion(onDone: () -> Unit) {
        viewModelScope.launch {
            val state = _ui.value
            val suggestion = state.pendingSetReaction?.nextSetSuggestion
            val exerciseId = state.pendingReactionExerciseId
            val setNumber = state.pendingReactionSetNumber
            val setId = state.pendingReactionSetId
            if (suggestion == null || exerciseId == null || setNumber == null || setId == null) {
                onDone()
                return@launch
            }
            applySuggestionToNextSet(exerciseId, setNumber, suggestion)
            setReactionDao.markAccepted(setId, true)
            _ui.update {
                it.copy(
                    pendingSetReaction = it.pendingSetReaction?.copy(
                        shortMessage = "Применено. Следующий подход: ${formatWeight(suggestion.newWeight)} кг",
                        nextSetSuggestion = null
                    ),
                    pendingReactionStatus = "APPLIED"
                )
            }
            onDone()
        }
    }

    fun declinePendingSuggestion(onDone: () -> Unit) {
        viewModelScope.launch {
            _ui.value.pendingReactionSetId?.let { setReactionDao.markAccepted(it, false) }
            val currentWeight = sessionRepo.getSession(sessionId)
                ?.exerciseLogs
                ?.firstOrNull { it.exerciseId == _ui.value.pendingReactionExerciseId }
                ?.sets
                ?.firstOrNull { it.setNumber == ((_ui.value.pendingReactionSetNumber ?: 1) + 1) }
                ?.weightKg
            _ui.update {
                it.copy(
                    pendingSetReaction = it.pendingSetReaction?.copy(
                        shortMessage = "Оставили текущий вес: ${formatWeight(currentWeight)} кг",
                        nextSetSuggestion = null
                    ),
                    pendingReactionStatus = "DECLINED"
                )
            }
            onDone()
        }
    }

    fun startRest(seconds: Int, exerciseId: String?) {
        restTimerJob?.cancel()
        restTimerJob = viewModelScope.launch {
            val decks = exerciseId
                ?.let { resolveContentIdForExercise(it) }
                ?.let { contentId -> buildRestDecks(contentId) }
                ?: RestDecks()

            val initiallyShown = listOfNotNull(
                decks.fact.firstOrNull(),
                decks.technique.firstOrNull(),
                decks.motivation.firstOrNull()
            ).distinctBy { it.id }
            initiallyShown.forEach { markShownOnce(it.id) }

            _ui.update {
                it.copy(
                    restCountdown = seconds,
                    factItems = decks.fact,
                    techniqueItems = decks.technique,
                    motivationItems = decks.motivation,
                    factIndex = 0,
                    techniqueIndex = 0,
                    motivationIndex = 0,
                    restCardRotationOffset = 0
                )
            }

            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining -= 1
                _ui.update { state ->
                    state.copy(restCountdown = remaining.takeIf { v -> v > 0 })
                }
            }
        }
    }

    fun nextCoachCard(type: CoachContentType) {
        viewModelScope.launch {
            var shown: CoachContentItem? = null

            _ui.update { state ->
                when (type) {
                    CoachContentType.FACT -> {
                        if (state.factItems.isEmpty()) return@update state
                        val next = (state.factIndex + 1) % state.factItems.size
                        shown = state.factItems[next]
                        state.copy(
                            factIndex = next,
                            restCardRotationOffset = state.restCardRotationOffset + 1
                        )
                    }
                    CoachContentType.TECHNIQUE -> {
                        if (state.techniqueItems.isEmpty()) return@update state
                        val next = (state.techniqueIndex + 1) % state.techniqueItems.size
                        shown = state.techniqueItems[next]
                        state.copy(
                            techniqueIndex = next,
                            restCardRotationOffset = state.restCardRotationOffset + 1
                        )
                    }
                    CoachContentType.MOTIVATION -> {
                        if (state.motivationItems.isEmpty()) return@update state
                        val next = (state.motivationIndex + 1) % state.motivationItems.size
                        shown = state.motivationItems[next]
                        state.copy(
                            motivationIndex = next,
                            restCardRotationOffset = state.restCardRotationOffset + 1
                        )
                    }
                    else -> state
                }
            }

            shown?.let { markShownOnce(it.id) }
        }
    }

    fun savePreWorkoutEnergy(level: EnergyLevel) {
        viewModelScope.launch {
            wellnessRepo.save(
                WellnessEntry(
                    sessionId = sessionId,
                    timestamp = System.currentTimeMillis(),
                    type = WellnessTriggerType.PRE_WORKOUT,
                    energy = level
                )
            )
            _ui.update { it.copy(showPreWorkoutPicker = false) }
        }
    }

    fun skipPreWorkoutPicker() {
        viewModelScope.launch {
            wellnessRepo.save(
                WellnessEntry(
                    sessionId = sessionId,
                    timestamp = System.currentTimeMillis(),
                    type = WellnessTriggerType.PRE_WORKOUT,
                    energy = null
                )
            )
            _ui.update { it.copy(showPreWorkoutPicker = false) }
        }
    }

    fun checkBetweenExercisePicker(
        completedExerciseId: String,
        nextExerciseId: String
    ) {
        if (completedExerciseId == nextExerciseId) return
        viewModelScope.launch {
            val session = sessionRepo.getSession(sessionId) ?: return@launch
            val completedIndex = session.exerciseLogs.indexOfFirst {
                it.exerciseId == completedExerciseId
            }
            if (completedIndex < 0) return@launch

            val hadStrongSignal = betweenPickerLogic.lastExerciseHadStrongSignal(
                sessionId = sessionId,
                exerciseId = completedExerciseId
            )
            val shouldShow = betweenPickerLogic.shouldShowBetweenPicker(
                sessionId = sessionId,
                completedExerciseIndex = completedIndex,
                totalExercises = session.exerciseLogs.size,
                lastExerciseHadStrongSignal = hadStrongSignal
            )
            if (shouldShow) {
                _ui.update { it.copy(showBetweenExercisePicker = true) }
            }
        }
    }

    fun saveBetweenExerciseEnergy(level: EnergyLevel, completedExerciseId: String) {
        saveBetweenExerciseEntry(level = level, completedExerciseId = completedExerciseId)
    }

    fun skipBetweenExercisePicker(completedExerciseId: String) {
        saveBetweenExerciseEntry(level = null, completedExerciseId = completedExerciseId)
    }

    private fun saveBetweenExerciseEntry(level: EnergyLevel?, completedExerciseId: String) {
        viewModelScope.launch {
            val session = sessionRepo.getSession(sessionId)
            val completedIndex = session?.exerciseLogs?.indexOfFirst {
                it.exerciseId == completedExerciseId
            } ?: -1
            wellnessRepo.save(
                WellnessEntry(
                    sessionId = sessionId,
                    timestamp = System.currentTimeMillis(),
                    type = WellnessTriggerType.BETWEEN_EXERCISES,
                    energy = level,
                    notes = completedIndex.takeIf { it >= 0 }?.toString()
                )
            )
            _ui.update { it.copy(showBetweenExercisePicker = false) }
        }
    }

    fun weightStepFor(currentWeight: Double): Double {
        val level = profile.value?.level ?: ExperienceLevel.INTERMEDIATE
        return progressionEngine.stepFor(currentWeight.coerceAtLeast(1.0), level)
    }

    suspend fun weightGridForExercise(exerciseId: String): Double {
        val equipment = resolveEquipmentForExercise(exerciseId)
        return when {
            Equipment.MACHINES in equipment -> 5.0
            Equipment.CABLES in equipment -> 2.5
            Equipment.BARBELL in equipment -> 2.5
            Equipment.DUMBBELLS in equipment -> 1.0
            else -> 1.0
        }
    }

    suspend fun smartWeightStepFor(exerciseId: String, currentWeight: Double): Double {
        val percentStep = weightStepFor(currentWeight)
        val grid = weightGridForExercise(exerciseId)
        return maxOf(percentStep, grid)
    }

    suspend fun adjustWeightWithGrid(
        exerciseId: String,
        currentWeight: Double,
        increase: Boolean
    ): Double {
        val step = smartWeightStepFor(exerciseId, currentWeight)
        val grid = weightGridForExercise(exerciseId)
        val candidate = if (increase) currentWeight + step else (currentWeight - step).coerceAtLeast(0.0)
        return snapToGrid(candidate, grid)
    }

    suspend fun getTargetRirForSet(exerciseId: String, setNumber: Int): Int? {
        val program = programRepo.getActiveProgram() ?: return null
        val exercise = program.workouts
            .asSequence()
            .flatMap { it.exercises.asSequence() }
            .firstOrNull { it.id == exerciseId }
            ?: return null
        return exercise.targetSetsDetailed
            ?.firstOrNull { it.setNumber == setNumber }
            ?.rirTarget
    }

    fun getRestSecondsForExercise(exerciseId: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val activeProgram = programRepo.getActiveProgram()
            val rest = activeProgram?.workouts
                ?.asSequence()
                ?.flatMap { it.exercises.asSequence() }
                ?.firstOrNull { it.id == exerciseId }
                ?.restSeconds
                ?: 90
            onResult(rest)
        }
    }

    fun skipRest() {
        restTimerJob?.cancel()
        restTimerJob = null
        _ui.update {
            it.copy(
                restCountdown = null,
                factItems = emptyList(),
                techniqueItems = emptyList(),
                motivationItems = emptyList(),
                factIndex = 0,
                techniqueIndex = 0,
                motivationIndex = 0
            )
        }
    }

    override fun onCleared() {
        restTimerJob?.cancel()
        restTimerJob = null
        super.onCleared()
    }

    fun finish(onDone: (sessionId: String) -> Unit) {
        viewModelScope.launch {
            _ui.update { it.copy(isFinishing = true) }
            sessionRepo.finishSession(sessionId)
            backup.save()
            _ui.update { it.copy(isFinishing = false) }
            onDone(sessionId)
        }
    }

    private suspend fun resolveContentIdForExercise(exerciseId: String): String? {
        val activeProgram = programRepo.getActiveProgram() ?: return null
        return activeProgram.workouts
            .asSequence()
            .flatMap { it.exercises.asSequence() }
            .firstOrNull { it.id == exerciseId }
            ?.contentId
    }

    private suspend fun resolveEquipmentForExercise(exerciseId: String): List<Equipment> {
        val activeProgram = programRepo.getActiveProgram() ?: return emptyList()
        return activeProgram.workouts
            .asSequence()
            .flatMap { it.exercises.asSequence() }
            .firstOrNull { it.id == exerciseId }
            ?.equipment
            ?: emptyList()
    }

    private suspend fun buildRestDecks(contentId: String): RestDecks {
        val usedIds = mutableSetOf<String>()
        val fact = buildDeck(contentId, CoachContentType.FACT, usedIds)
        usedIds += fact.map { it.id }

        val technique = buildDeck(contentId, CoachContentType.TECHNIQUE, usedIds)
        usedIds += technique.map { it.id }

        val motivation = buildDeck(contentId, CoachContentType.MOTIVATION, usedIds)
        return RestDecks(fact = fact, technique = technique, motivation = motivation)
    }

    private suspend fun buildDeck(
        contentId: String,
        type: CoachContentType,
        excludedGlobalIds: Set<String>
    ): List<CoachContentItem> {
        val picked = mutableListOf<CoachContentItem>()
        var attempts = 0

        while (picked.size < DECK_SIZE && attempts < MAX_PICK_ATTEMPTS) {
            attempts += 1
            val excluded = excludedGlobalIds + picked.map { it.id }.toSet()
            val item = coachRepo.pickContentForExercise(
                contentId = contentId,
                preferredType = type,
                excludedIds = excluded
            ) ?: break
            if (picked.any { it.id == item.id }) break
            picked += item
        }

        if (picked.isNotEmpty()) return picked

        val fallback = coachRepo.pickContentForExercise(
            contentId = contentId,
            preferredType = null,
            excludedIds = excludedGlobalIds
        )
        return listOfNotNull(fallback)
    }

    private data class RestDecks(
        val fact: List<CoachContentItem> = emptyList(),
        val technique: List<CoachContentItem> = emptyList(),
        val motivation: List<CoachContentItem> = emptyList()
    )

    private companion object {
        const val DECK_SIZE = 6
        const val MAX_PICK_ATTEMPTS = 20
    }

    private fun snapToGrid(value: Double, grid: Double): Double {
        if (grid <= 0.0) return value
        return (kotlin.math.round(value / grid) * grid).coerceAtLeast(0.0)
    }

    private fun formatWeight(value: Double?): String {
        if (value == null) return "—"
        val rounded = kotlin.math.round(value * 10.0) / 10.0
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            "%.1f".format(rounded)
        }
    }

    private fun checkPreWorkoutPicker() {
        viewModelScope.launch {
            val existing = wellnessRepo.getPreWorkoutFor(sessionId)
            if (existing == null) {
                _ui.update { it.copy(showPreWorkoutPicker = true) }
            }
        }
    }

    private suspend fun markShownOnce(id: String) {
        if (id in shownInThisSession) return
        shownInThisSession += id
        coachRepo.markShown(id)
    }

    private suspend fun applySuggestionToNextSet(
        exerciseId: String,
        currentSetNumber: Int,
        tip: NextSetTip
    ) {
        val s = sessionRepo.getSession(sessionId) ?: return
        val updated = s.copy(
            exerciseLogs = s.exerciseLogs.map { log ->
                if (log.exerciseId != exerciseId) return@map log
                log.copy(
                    sets = log.sets.map { set ->
                        if (set.setNumber != currentSetNumber + 1) return@map set
                        when (tip.type) {
                            com.forma.app.domain.livecoach.WeightAdjustment.INCREASE,
                            com.forma.app.domain.livecoach.WeightAdjustment.DECREASE,
                            com.forma.app.domain.livecoach.WeightAdjustment.KEEP -> set.copy(
                                targetWeightKg = tip.newWeight ?: set.targetWeightKg,
                                weightKg = tip.newWeight ?: set.weightKg
                            )
                            com.forma.app.domain.livecoach.WeightAdjustment.MORE_REST -> set
                        }
                    }
                )
            }
        )
        sessionRepo.saveSession(updated)
    }
}
