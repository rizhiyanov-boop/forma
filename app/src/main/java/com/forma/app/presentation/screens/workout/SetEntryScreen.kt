package com.forma.app.presentation.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.forma.app.presentation.components.FormaCard
import com.forma.app.presentation.components.HapticType
import com.forma.app.presentation.components.PrimaryButton
import com.forma.app.presentation.theme.AccentLime
import com.forma.app.presentation.theme.BgBlack
import com.forma.app.presentation.theme.SurfaceHighlight
import com.forma.app.presentation.theme.TextPrimary
import com.forma.app.presentation.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun SetEntryScreen(
    setId: String,
    exerciseId: String,
    restSeconds: Int,
    vm: WorkoutViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onSaved: (exerciseId: String, restSeconds: Int) -> Unit
) {
    val session by vm.session.collectAsState()
    val target = remember(session, setId, exerciseId) {
        val log = session?.exerciseLogs?.firstOrNull { it.exerciseId == exerciseId }
        val set = log?.sets?.firstOrNull { it.id == setId }
        log to set
    }

    val log = target.first
    val set = target.second

    var reps by remember(setId) { mutableIntStateOf(0) }
    var weight by remember(setId) { mutableDoubleStateOf(0.0) }
    var rir by remember(setId) { mutableIntStateOf(2) }
    var rirSkipped by remember(setId) { mutableStateOf(false) }
    var weightStepLabel by remember(setId) { mutableDoubleStateOf(1.0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(set?.id) {
        if (set != null) {
            reps = set.reps.takeIf { it > 0 } ?: (set.targetRepsMin ?: 0)
            weight = set.weightKg ?: set.targetWeightKg ?: 0.0
            val targetRir = vm.getTargetRirForSet(exerciseId, set.setNumber)
            rir = set.rir ?: targetRir ?: 2
            rirSkipped = set.isCompleted && set.rir == null
            weightStepLabel = vm.smartWeightStepFor(exerciseId, weight)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .systemBarsPadding()
    ) {
        if (log == null || set == null) return@Box

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = TextPrimary)
                }
                Column {
                    Text(
                        "Подход ${set.setNumber}",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        log.exerciseName,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            NumberAdjustCard(
                title = "Повторы",
                value = reps.toString(),
                onMinus = { if (reps > 0) reps -= 1 },
                onPlus = { reps += 1 }
            )

            if (set.weightKg != null || set.targetWeightKg != null) {
                NumberAdjustCard(
                    title = "Вес",
                    value = formatWeight(weight),
                    suffix = "кг",
                    helperText = "Шаг: ${formatWeight(weightStepLabel)} кг",
                    onMinus = {
                        scope.launch {
                            val step = vm.smartWeightStepFor(exerciseId, weight)
                            weightStepLabel = step
                            weight = vm.adjustWeightWithGrid(exerciseId, weight, increase = false)
                        }
                    },
                    onPlus = {
                        scope.launch {
                            val step = vm.smartWeightStepFor(exerciseId, weight)
                            weightStepLabel = step
                            weight = vm.adjustWeightWithGrid(exerciseId, weight, increase = true)
                        }
                    }
                )
            }

            NumberAdjustCard(
                title = "RIR",
                value = rir.toString(),
                onMinus = { if (rir > 0) rir -= 1 },
                onPlus = { if (rir < 10) rir += 1 }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (rirSkipped) "RIR не указан" else "Не помню RIR",
                    color = if (rirSkipped) AccentLime else TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clickable { rirSkipped = !rirSkipped }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            Spacer(Modifier.weight(1f))
            PrimaryButton(
                text = "Записать подход",
                hapticOnClick = HapticType.Strong,
                onClick = {
                    vm.completeSetAndAnalyze(
                        exerciseId = exerciseId,
                        setId = set.id,
                        reps = reps,
                        weight = if (set.weightKg != null || set.targetWeightKg != null) weight else null,
                        rir = if (rirSkipped) null else rir,
                        onAnalyzed = { reaction ->
                            onSaved(exerciseId, restSeconds)
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun NumberAdjustCard(
    title: String,
    value: String,
    suffix: String = "",
    helperText: String? = null,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    FormaCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                title,
                color = TextSecondary,
                style = MaterialTheme.typography.labelMedium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundActionButton(text = "−", onClick = onMinus)
                Spacer(Modifier.width(14.dp))
                Text(
                    if (suffix.isBlank()) value else "$value $suffix",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(14.dp))
                RoundActionButton(text = "+", onClick = onPlus)
            }
            if (helperText != null) {
                Text(
                    helperText,
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun RoundActionButton(text: String, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(SurfaceHighlight, RoundedCornerShape(22.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

private fun formatWeight(value: Double): String {
    val rounded = kotlin.math.round(value * 10.0) / 10.0
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        "%.1f".format(rounded)
    }
}
