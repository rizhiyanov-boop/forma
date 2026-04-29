package com.forma.app.presentation.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forma.app.domain.analytics.AnalyticsRepository
import com.forma.app.domain.analytics.ProgressData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProgressUi(
    val isLoading: Boolean = true,
    val data: ProgressData? = null
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    analyticsRepo: AnalyticsRepository
) : ViewModel() {

    val ui: StateFlow<ProgressUi> = flow {
        emit(ProgressUi(isLoading = true))
        analyticsRepo.observeProgress(weeksBack = 8).collect { data ->
            emit(ProgressUi(isLoading = false, data = data))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUi())

    private val _selectedExerciseIdx = kotlinx.coroutines.flow.MutableStateFlow(0)
    val selectedExerciseIdx: StateFlow<Int> = _selectedExerciseIdx

    fun selectExercise(index: Int) {
        _selectedExerciseIdx.value = index
    }
}
