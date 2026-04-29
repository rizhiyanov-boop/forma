package com.forma.app.presentation.screens.workoutdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forma.app.domain.model.Exercise
import com.forma.app.domain.model.Program
import com.forma.app.domain.model.Workout
import com.forma.app.domain.repository.ProgramRepository
import com.forma.app.domain.repository.SessionRepository
import com.forma.app.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutDetailUi(
    val isLoading: Boolean = true,
    val program: Program? = null,
    val workout: Workout? = null,

    // Состояние диалога замены
    val replaceDialogFor: Exercise? = null,    // какое упражнение заменяем (null = диалог скрыт)
    val replaceReason: String = "",
    val replaceLoading: Boolean = false,
    val replaceSuggestions: List<Exercise> = emptyList(),
    val replaceReasoning: String? = null,
    val replaceError: String? = null,

    // Подтверждение «разово или навсегда» после выбора варианта
    val pendingReplacement: Exercise? = null   // выбранный вариант, ждём решения юзера
)

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val programRepo: ProgramRepository,
    private val sessionRepo: SessionRepository
) : ViewModel() {

    private val workoutId: String = savedState[Route.WorkoutDetail.ARG] ?: ""

    private val _ui = MutableStateFlow(WorkoutDetailUi())
    val ui: StateFlow<WorkoutDetailUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            programRepo.observeActiveProgram()
                .map { program ->
                    val workout = program?.workouts?.firstOrNull { it.id == workoutId }
                    WorkoutDetailUi(
                        isLoading = false,
                        program = program,
                        workout = workout
                    )
                }
                .collect { newState ->
                    // Сохраняем диалоговое состояние, обновляем только данные
                    _ui.update { current ->
                        newState.copy(
                            replaceDialogFor = current.replaceDialogFor,
                            replaceReason = current.replaceReason,
                            replaceLoading = current.replaceLoading,
                            replaceSuggestions = current.replaceSuggestions,
                            replaceReasoning = current.replaceReasoning,
                            replaceError = current.replaceError,
                            pendingReplacement = current.pendingReplacement
                        )
                    }
                }
        }
    }

    fun startSession(onReady: (sessionId: String) -> Unit) {
        val w = _ui.value.workout ?: return
        val p = _ui.value.program ?: return
        viewModelScope.launch {
            val session = sessionRepo.createSession(w.id, p.id)
            onReady(session.id)
        }
    }

    // ─── Замена упражнения ───────────────────────────────────────────

    fun openReplaceDialog(exercise: Exercise) {
        _ui.update {
            it.copy(
                replaceDialogFor = exercise,
                replaceReason = "",
                replaceSuggestions = emptyList(),
                replaceReasoning = null,
                replaceError = null,
                pendingReplacement = null
            )
        }
    }

    fun closeReplaceDialog() {
        _ui.update {
            it.copy(
                replaceDialogFor = null,
                replaceReason = "",
                replaceSuggestions = emptyList(),
                replaceReasoning = null,
                replaceError = null,
                replaceLoading = false,
                pendingReplacement = null
            )
        }
    }

    fun setReplaceReason(reason: String) {
        _ui.update { it.copy(replaceReason = reason) }
    }

    fun fetchReplacements() {
        val ex = _ui.value.replaceDialogFor ?: return
        val reason = _ui.value.replaceReason.takeIf { it.isNotBlank() }
        viewModelScope.launch {
            _ui.update { it.copy(replaceLoading = true, replaceError = null) }
            try {
                val list = programRepo.suggestReplacements(workoutId, ex.id, reason)
                _ui.update { it.copy(
                    replaceLoading = false,
                    replaceSuggestions = list,
                    replaceError = null
                ) }
            } catch (t: Throwable) {
                android.util.Log.e("Forma", "Replacement fetch failed", t)
                _ui.update { it.copy(
                    replaceLoading = false,
                    replaceError = t.message ?: "Не удалось получить варианты"
                ) }
            }
        }
    }

    /**
     * Пользователь выбрал вариант — теперь нужно спросить «разово или навсегда».
     * Запоминаем выбор в pendingReplacement.
     */
    fun selectReplacement(replacement: Exercise) {
        _ui.update { it.copy(pendingReplacement = replacement) }
    }

    fun cancelPending() {
        _ui.update { it.copy(pendingReplacement = null) }
    }

    /**
     * Применить перманентную замену — обновляем программу, override остаётся в БД.
     */
    fun applyPermanent(onDone: () -> Unit) {
        val origin = _ui.value.replaceDialogFor ?: return
        val replacement = _ui.value.pendingReplacement ?: return
        viewModelScope.launch {
            programRepo.replaceExercisePermanently(workoutId, origin.id, replacement)
            closeReplaceDialog()
            onDone()
        }
    }

    /**
     * Разовая замена — стартуем сессию и подменяем в ней упражнение.
     */
    fun applyOnceAndStart(onSessionReady: (sessionId: String) -> Unit) {
        val origin = _ui.value.replaceDialogFor ?: return
        val replacement = _ui.value.pendingReplacement ?: return
        val program = _ui.value.program ?: return
        val workout = _ui.value.workout ?: return
        viewModelScope.launch {
            val session = sessionRepo.createSession(workout.id, program.id)
            // Найти ExerciseLog с подставленным упражнением и заменить его
            val logForOrigin = session.exerciseLogs
                .firstOrNull { it.exerciseId == origin.id }
            if (logForOrigin != null) {
                sessionRepo.replaceExerciseInSession(session.id, logForOrigin.id, replacement)
            }
            closeReplaceDialog()
            onSessionReady(session.id)
        }
    }
}
