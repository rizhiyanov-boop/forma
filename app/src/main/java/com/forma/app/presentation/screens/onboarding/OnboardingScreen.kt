package com.forma.app.presentation.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.forma.app.presentation.components.PrimaryButton
import com.forma.app.presentation.components.SecondaryButton
import com.forma.app.presentation.components.StepDots
import com.forma.app.presentation.theme.AccentLime
import com.forma.app.presentation.theme.BgBlack
import com.forma.app.presentation.theme.ErrorRed
import com.forma.app.presentation.theme.TextPrimary
import com.forma.app.presentation.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    vm: OnboardingViewModel = hiltViewModel(),
    onFinished: () -> Unit
) {
    val s by vm.ui.collectAsState()

    Box(
        Modifier
            .fillMaxSize()
            .background(BgBlack)
            .systemBarsPadding()
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {

            // Топ-бар
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (s.step > 0) {
                    IconButton(onClick = vm::prev) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = TextPrimary)
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
                Spacer(Modifier.width(8.dp))
                StepDots(total = s.totalSteps, current = s.step)
            }

            Spacer(Modifier.height(16.dp))

            // Контент с анимированным переходом
            AnimatedContent(
                targetState = s.step,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally(tween(300)) { it / 4 } + fadeIn(tween(300)) togetherWith
                                slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(200))
                    } else {
                        slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) togetherWith
                                slideOutHorizontally(tween(300)) { it / 4 } + fadeOut(tween(200))
                    }
                },
                label = "onboard_step",
                modifier = Modifier.weight(1f)
            ) { step ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    when (step) {
                        0 -> StepGoal(s, vm)
                        1 -> StepSex(s, vm)
                        2 -> StepLevel(s, vm)
                        3 -> StepDays(s, vm)
                        4 -> StepEquipment(s, vm)
                        5 -> StepBody(s, vm)
                        6 -> StepSummary(s)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            // Ошибка
            if (s.errorMessage != null) {
                Text(
                    s.errorMessage!!,
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Нижние кнопки
            Column(Modifier.padding(bottom = 20.dp)) {
                if (s.isGenerating) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = AccentLime, strokeWidth = 2.dp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Составляю программу…",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    if (s.step < s.totalSteps - 1) {
                        PrimaryButton(
                            text = "Далее",
                            enabled = s.canProceed,
                            onClick = vm::next
                        )
                    } else {
                        PrimaryButton(
                            text = "Создать программу",
                            enabled = s.canProceed && !s.isGenerating,
                            onClick = { vm.finish(onFinished) }
                        )
                        Spacer(Modifier.height(8.dp))
                        SecondaryButton(text = "Назад", onClick = vm::prev)
                    }
                }
            }
        }
    }
}

// Заголовок шага
@Composable
internal fun StepHeader(title: String, subtitle: String? = null) {
    Text(
        title,
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.displaySmall
    )
    if (subtitle != null) {
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
    }
    Spacer(Modifier.height(24.dp))
}
