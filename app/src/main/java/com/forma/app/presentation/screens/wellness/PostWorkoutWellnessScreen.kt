package com.forma.app.presentation.screens.wellness

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forma.app.domain.review.ReviewRepository
import com.forma.app.domain.wellness.EnergyLevel
import com.forma.app.domain.wellness.WellnessEntry
import com.forma.app.domain.wellness.WellnessRepository
import com.forma.app.domain.wellness.WellnessTriggerType
import com.forma.app.presentation.components.FormaCard
import com.forma.app.presentation.components.HapticType
import com.forma.app.presentation.components.PrimaryButton
import com.forma.app.presentation.icons.iconRes
import com.forma.app.presentation.theme.AccentLime
import com.forma.app.presentation.theme.BgBlack
import com.forma.app.presentation.theme.SurfaceHighlight
import com.forma.app.presentation.theme.TextPrimary
import com.forma.app.presentation.theme.TextSecondary
import com.forma.app.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class PostWorkoutUi(
    val energy: EnergyLevel? = null,
    val sleep: Int = 3,
    val stress: Int = 3,
    val mood: Int = 3,
    val isSubmitting: Boolean = false
) {
    val canSubmit: Boolean
        get() = energy != null
}

@HiltViewModel
class PostWorkoutWellnessViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val wellnessRepo: WellnessRepository,
    private val reviewRepo: ReviewRepository
) : ViewModel() {
    val sessionId: String = savedState[Route.ARG_SESSION_ID] ?: ""

    private val _ui = MutableStateFlow(PostWorkoutUi())
    val ui: StateFlow<PostWorkoutUi> = _ui.asStateFlow()

    fun setEnergy(level: EnergyLevel) = _ui.update { it.copy(energy = level) }
    fun setSleep(value: Int) = _ui.update { it.copy(sleep = value) }
    fun setStress(value: Int) = _ui.update { it.copy(stress = value) }
    fun setMood(value: Int) = _ui.update { it.copy(mood = value) }

    fun submit(onDone: (reviewId: String?) -> Unit) {
        viewModelScope.launch {
            _ui.update { it.copy(isSubmitting = true) }

            wellnessRepo.save(
                WellnessEntry(
                    sessionId = sessionId,
                    timestamp = System.currentTimeMillis(),
                    type = WellnessTriggerType.POST_WORKOUT,
                    energy = _ui.value.energy,
                    sleepQuality = _ui.value.sleep,
                    stressLevel = _ui.value.stress,
                    mood = _ui.value.mood
                )
            )

            val reviewId = try {
                reviewRepo.reviewAfterWorkout(sessionId)
            } catch (t: Throwable) {
                android.util.Log.e("Forma.Wellness", "Review failed", t)
                null
            }

            _ui.update { it.copy(isSubmitting = false) }
            onDone(reviewId)
        }
    }
}

@Composable
fun PostWorkoutWellnessScreen(
    vm: PostWorkoutWellnessViewModel = hiltViewModel(),
    onSubmit: (reviewId: String?) -> Unit
) {
    val ui by vm.ui.collectAsState()
    val haptic = LocalHapticFeedback.current

    Column(
        Modifier
            .fillMaxSize()
            .background(BgBlack)
            .systemBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Как прошло?",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineMedium
        )

        FormaCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Энергия",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    EnergyLevel.entries.forEach { level ->
                        val selected = ui.energy == level
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (selected) SurfaceHighlight else BgBlack,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    vm.setEnergy(level)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                androidx.compose.material3.Icon(
                                    painter = painterResource(level.iconRes()),
                                    contentDescription = null,
                                    tint = Color.Unspecified
                                )
                                Text(
                                    level.displayName,
                                    color = if (selected) AccentLime else TextSecondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }

        SliderSection(
            title = "Сон прошлой ночью",
            value = ui.sleep,
            left = "Плохой",
            right = "Отличный",
            onChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                vm.setSleep(it)
            }
        )
        SliderSection(
            title = "Уровень стресса",
            value = ui.stress,
            left = "Низкий",
            right = "Высокий",
            onChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                vm.setStress(it)
            }
        )
        SliderSection(
            title = "Настроение",
            value = ui.mood,
            left = "Плохое",
            right = "Отличное",
            onChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                vm.setMood(it)
            }
        )

        Spacer(Modifier.weight(1f))

        PrimaryButton(
            text = if (ui.isSubmitting) "Анализ тренировки…" else "Сохранить и посмотреть разбор",
            enabled = ui.canSubmit && !ui.isSubmitting,
            hapticOnClick = HapticType.Strong,
            onClick = { vm.submit(onSubmit) }
        )
    }
}

@Composable
private fun SliderSection(
    title: String,
    value: Int,
    left: String,
    right: String,
    onChange: (Int) -> Unit
) {
    FormaCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                title,
                color = TextSecondary,
                style = MaterialTheme.typography.labelMedium
            )
            Slider(
                value = value.toFloat(),
                onValueChange = { onChange(it.roundToInt().coerceIn(1, 5)) },
                valueRange = 1f..5f,
                steps = 3
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(left, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.weight(1f))
                Text("$value/5", color = AccentLime, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                Text(right, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
