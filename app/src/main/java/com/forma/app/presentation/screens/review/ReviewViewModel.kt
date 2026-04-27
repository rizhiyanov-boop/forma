package com.forma.app.presentation.screens.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forma.app.domain.review.ReviewRepository
import com.forma.app.domain.review.WorkoutReview
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

data class ReviewUi(
    val isApplying: Set<String> = emptySet(),  // id рекомендаций которые сейчас применяются
    val errorMessage: String? = null
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val reviewRepo: ReviewRepository
) : ViewModel() {

    private val reviewId: String = savedState[Route.Review.ARG] ?: ""

    val review: StateFlow<WorkoutReview?> = reviewRepo.observeReview(reviewId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _ui = MutableStateFlow(ReviewUi())
    val ui: StateFlow<ReviewUi> = _ui.asStateFlow()

    fun applyRecommendation(recommendationId: String) {
        viewModelScope.launch {
            _ui.update { it.copy(isApplying = it.isApplying + recommendationId) }
            try {
                reviewRepo.applyRecommendation(reviewId, recommendationId)
            } catch (t: Throwable) {
                android.util.Log.e("Forma.Review", "Apply failed", t)
                _ui.update { it.copy(errorMessage = "Не удалось применить: ${t.message}") }
            } finally {
                _ui.update { it.copy(isApplying = it.isApplying - recommendationId) }
            }
        }
    }

    fun dismiss() {
        viewModelScope.launch {
            reviewRepo.dismissReview(reviewId)
        }
    }
}
