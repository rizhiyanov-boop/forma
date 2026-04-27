package com.forma.app.presentation.screens.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forma.app.domain.model.WorkoutSession
import com.forma.app.domain.repository.SessionRepository
import com.forma.app.domain.review.ReviewRepository
import com.forma.app.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutUi(
    val restCountdown: Int? = null,      // секунды обратного отсчёта после подхода
    val isFinishing: Boolean = false
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val sessionRepo: SessionRepository,
    private val reviewRepo: ReviewRepository,
    private val backup: com.forma.app.data.backup.AppBackupService
) : ViewModel() {

    val sessionId: String = savedState[Route.Workout.ARG] ?: ""

    val session: StateFlow<WorkoutSession?> = sessionRepo.observeSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _ui = MutableStateFlow(WorkoutUi())
    val ui: StateFlow<WorkoutUi> = _ui.asStateFlow()

    fun updateSet(setId: String, reps: Int, weight: Double?, rir: Int?, completed: Boolean) {
        viewModelScope.launch {
            sessionRepo.updateSet(sessionId, setId, reps, weight, rir, completed)
        }
    }

    /**
     * Запускает обратный отсчёт на N секунд. Используется после завершения подхода.
     */
    fun startRest(seconds: Int) {
        viewModelScope.launch {
            _ui.update { it.copy(restCountdown = seconds) }
            var remaining = seconds
            while (remaining > 0) {
                kotlinx.coroutines.delay(1000)
                remaining -= 1
                _ui.update { it.copy(restCountdown = remaining.takeIf { v -> v > 0 }) }
            }
        }
    }

    fun skipRest() {
        _ui.update { it.copy(restCountdown = null) }
    }

    /**
     * Завершает сессию и запускает AI-разбор в фоне.
     * onDone получит reviewId или null если разбор упал — навигация решит куда вести.
     */
    fun finish(onDone: (reviewId: String?) -> Unit) {
        viewModelScope.launch {
            _ui.update { it.copy(isFinishing = true) }
            sessionRepo.finishSession(sessionId)

            // Сразу же делаем бэкап — самый ценный момент, нельзя терять
            backup.save()

            val reviewId = try {
                reviewRepo.reviewAfterWorkout(sessionId)
            } catch (t: Throwable) {
                android.util.Log.e("Forma.Review", "Auto-review failed", t)
                null
            }

            _ui.update { it.copy(isFinishing = false) }
            onDone(reviewId)
        }
    }
}
