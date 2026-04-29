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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.forma.app.domain.model.ExerciseLog
import com.forma.app.domain.model.SetLog
import com.forma.app.domain.model.WorkoutSession
import com.forma.app.presentation.components.EnergyPickerOverlay
import com.forma.app.presentation.components.FormaCard
import com.forma.app.presentation.components.HapticType
import com.forma.app.presentation.components.PrimaryButton
import com.forma.app.presentation.theme.AccentLime
import com.forma.app.presentation.theme.BgBlack
import com.forma.app.presentation.theme.SurfaceHighlight
import com.forma.app.presentation.theme.TextPrimary
import com.forma.app.presentation.theme.TextSecondary

@Composable
fun WorkoutScreen(
    sessionId: String,
    vm: WorkoutViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onFinished: (sessionId: String) -> Unit,
    onOpenSetEntry: (setId: String, exerciseId: String, restSeconds: Int) -> Unit
) {
    val session by vm.session.collectAsState()
    val ui by vm.ui.collectAsState()

    Box(
        Modifier
            .fillMaxSize()
            .background(BgBlack)
            .systemBarsPadding()
    ) {
        val s = session
        if (s == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentLime, strokeWidth = 2.dp)
            }
            return@Box
        }

        val nextPending = s.findNextPendingSet()

        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = TextPrimary)
                }
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "ТРЕНИРОВКА",
                        color = AccentLime,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        "${s.completedSetsCount} из ${s.totalSetsCount} подходов",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            ProgressBar(ratio = s.completionRatio)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(s.exerciseLogs) { log ->
                    ExerciseFlowCard(
                        log = log,
                        activeSetId = nextPending?.takeIf { it.exerciseId == log.exerciseId }?.set?.id,
                        onOpenSet = { set ->
                            vm.getRestSecondsForExercise(log.exerciseId) { rest ->
                                onOpenSetEntry(set.id, log.exerciseId, rest)
                            }
                        }
                    )
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton(
                        text = if (ui.isFinishing) "Завершение тренировки…" else "Завершить тренировку",
                        enabled = !ui.isFinishing,
                        hapticOnClick = HapticType.Strong,
                        onClick = { vm.finish(onFinished) }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        if (ui.showPreWorkoutPicker) {
            EnergyPickerOverlay(
                title = "КАК ОЩУЩЕНИЯ?",
                onPick = vm::savePreWorkoutEnergy,
                onSkip = vm::skipPreWorkoutPicker
            )
        }
    }
}

@Composable
private fun ProgressBar(ratio: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(SurfaceHighlight)
    ) {
        Box(
            Modifier
                .fillMaxWidth(ratio.coerceIn(0f, 1f))
                .height(3.dp)
                .background(AccentLime)
        )
    }
}

@Composable
private fun ExerciseFlowCard(
    log: ExerciseLog,
    activeSetId: String?,
    onOpenSet: (SetLog) -> Unit
) {
    FormaCard(
        modifier = Modifier.fillMaxWidth(),
        elevated = activeSetId != null,
        padding = PaddingValues(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                log.exerciseName,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )

            log.sets.forEach { set ->
                val done = set.isCompleted
                val active = set.id == activeSetId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceHighlight)
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Подход ${set.setNumber}",
                        color = if (active) AccentLime else TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    if (done) {
                        Text(
                            "Готово",
                            color = AccentLime,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        ActionChip(
                            text = "Заполнить",
                            textColor = if (active) AccentLime else TextPrimary,
                            onClick = { onOpenSet(set) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChip(
    text: String,
    textColor: androidx.compose.ui.graphics.Color = TextPrimary,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceHighlight)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private data class PendingSetUi(
    val exerciseId: String,
    val exerciseName: String,
    val set: SetLog
)

private fun WorkoutSession.findNextPendingSet(): PendingSetUi? {
    exerciseLogs.forEach { log ->
        val pending = log.sets.firstOrNull { !it.isCompleted } ?: return@forEach
        return PendingSetUi(
            exerciseId = log.exerciseId,
            exerciseName = log.exerciseName,
            set = pending
        )
    }
    return null
}
