package com.forma.app.presentation.screens.exercisedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forma.app.domain.model.Exercise
import com.forma.app.domain.model.Workout
import com.forma.app.domain.repository.ProgramRepository
import com.forma.app.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseDetailUi(
    val isLoading: Boolean = true,
    val workout: Workout? = null,
    val exercise: Exercise? = null
)

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val programRepo: ProgramRepository
) : ViewModel() {

    private val workoutId: String = savedState[Route.ExerciseDetail.ARG_WORKOUT] ?: ""
    private val exerciseId: String = savedState[Route.ExerciseDetail.ARG_EXERCISE] ?: ""

    private val _ui = MutableStateFlow(ExerciseDetailUi())
    val ui: StateFlow<ExerciseDetailUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            programRepo.observeActiveProgram().collect { program ->
                val workout = program?.workouts?.firstOrNull { it.id == workoutId }
                val exercise = workout?.exercises?.firstOrNull { it.id == exerciseId }
                _ui.update {
                    it.copy(isLoading = false, workout = workout, exercise = exercise)
                }
            }
        }
    }
}
