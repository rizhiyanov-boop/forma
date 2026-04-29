package com.forma.app.presentation.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.forma.app.domain.livecoach.CoachContentItem
import com.forma.app.domain.livecoach.CoachContentType
import com.forma.app.domain.livecoach.SetReaction
import com.forma.app.domain.livecoach.SetVerdict
import com.forma.app.presentation.components.EnergyPickerOverlay
import com.forma.app.presentation.components.FormaCard
import com.forma.app.presentation.components.HapticType
import com.forma.app.presentation.components.PrimaryButton
import com.forma.app.presentation.components.SecondaryButton
import com.forma.app.presentation.icons.iconRes
import com.forma.app.presentation.theme.AccentLime
import com.forma.app.presentation.theme.BgBlack
import com.forma.app.presentation.theme.SurfaceHighlight
import com.forma.app.presentation.theme.TextPrimary
import com.forma.app.presentation.theme.TextSecondary
import com.forma.app.presentation.theme.WarnAmber

@Composable
fun RestScreen(
    sessionId: String,
    exerciseId: String,
    seconds: Int,
    vm: WorkoutViewModel = hiltViewModel(),
    onBackToPlan: () -> Unit,
    onOpenNextSet: (setId: String, exerciseId: String, restSeconds: Int) -> Unit
) {
    val ui by vm.ui.collectAsState()
    val session by vm.session.collectAsState()
    var initialized by remember(sessionId, exerciseId, seconds) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(initialized, sessionId, exerciseId, seconds) {
        if (!initialized) {
            initialized = true
            vm.startRest(seconds = seconds, exerciseId = exerciseId)
        }
    }

    val nextPending = remember(session) {
        session?.exerciseLogs
            ?.firstNotNullOfOrNull { log ->
                log.sets.firstOrNull { !it.isCompleted }?.let { set -> set.id to log.exerciseId }
            }
    }
    val nextExerciseId = nextPending?.second

    LaunchedEffect(nextExerciseId, exerciseId) {
        val next = nextExerciseId ?: return@LaunchedEffect
        if (next != exerciseId) {
            vm.checkBetweenExercisePicker(
                completedExerciseId = exerciseId,
                nextExerciseId = next
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .systemBarsPadding()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            val maxCards = when {
                maxHeight >= 860.dp -> 4
                maxHeight >= 680.dp -> 3
                maxHeight >= 560.dp -> 2
                else -> 1
            }

            val hasSuggestion = ui.pendingReactionExerciseId == exerciseId && ui.pendingSetReaction != null
            val pending = ui.pendingSetReaction
            val status = ui.pendingReactionStatus ?: "PENDING"

            val cardModels = remember(
                hasSuggestion,
                pending,
                status,
                ui.currentFact,
                ui.currentTechnique,
                ui.currentMotivation,
                maxCards,
                ui.restCardRotationOffset
            ) {
                buildList<RestCardModel> {
                    if (hasSuggestion && pending != null) {
                        add(RestCardModel.Adaptation(pending, status))
                    }
                    val coachCandidates = buildList<RestCardModel.Coach> {
                        ui.currentFact?.let { add(RestCardModel.Coach(CoachContentType.FACT, it)) }
                        ui.currentTechnique?.let { add(RestCardModel.Coach(CoachContentType.TECHNIQUE, it)) }
                        ui.currentMotivation?.let { add(RestCardModel.Coach(CoachContentType.MOTIVATION, it)) }
                    }
                    val slotsLeft = (maxCards - size).coerceAtLeast(0)
                    if (slotsLeft > 0 && coachCandidates.isNotEmpty()) {
                        val start = ui.restCardRotationOffset % coachCandidates.size
                        repeat(minOf(slotsLeft, coachCandidates.size)) { i ->
                            add(coachCandidates[(start + i) % coachCandidates.size])
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackToPlan) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                    Text(
                        "Отдых",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                RestTimerHero(
                    seconds = ui.restCountdown ?: 0,
                    modifier = Modifier.weight(1f)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    userScrollEnabled = cardModels.size > 1
                ) {
                    items(cardModels) { model ->
                        when (model) {
                            is RestCardModel.Adaptation -> AdaptationCard(
                                reaction = model.reaction,
                                status = model.status,
                                onAccept = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    vm.acceptPendingSuggestion {}
                                },
                                onDecline = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    vm.declinePendingSuggestion {}
                                }
                            )

                            is RestCardModel.Coach -> CoachContentCard(
                                type = model.type,
                                content = model.content,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    vm.nextCoachCard(model.type)
                                }
                            )
                        }
                    }
                }

                nextPending?.let { (setId, exId) ->
                    PrimaryButton(
                        text = "Продолжить тренировку",
                        hapticOnClick = HapticType.Strong,
                        onClick = {
                            vm.getRestSecondsForExercise(exId) { rest ->
                                onOpenNextSet(setId, exId, rest)
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
                SecondaryButton(
                    text = "Вернуться к плану",
                    hapticOnClick = HapticType.Soft,
                    onClick = onBackToPlan
                )
            }
        }

        if (ui.showBetweenExercisePicker) {
            EnergyPickerOverlay(
                title = "КАК САМОЧУВСТВИЕ ТЕПЕРЬ?",
                subtitle = "Помогает подобрать нагрузку для следующего упражнения",
                onPick = { level -> vm.saveBetweenExerciseEnergy(level, exerciseId) },
                onSkip = { vm.skipBetweenExercisePicker(exerciseId) }
            )
        }
    }
}

private sealed interface RestCardModel {
    data class Adaptation(val reaction: SetReaction, val status: String) : RestCardModel
    data class Coach(val type: CoachContentType, val content: CoachContentItem) : RestCardModel
}

@Composable
private fun AdaptationCard(
    reaction: SetReaction,
    status: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val accent = when (reaction.verdict) {
        SetVerdict.TOO_HEAVY, SetVerdict.UNUSUAL -> WarnAmber
        else -> AccentLime
    }
    FormaCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(20.dp)),
        elevated = true,
        padding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "КОРРЕКЦИЯ",
                    color = accent,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                if (status != "PENDING") {
                    Box(
                        modifier = Modifier
                            .background(SurfaceHighlight, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (status == "APPLIED") "РЕШЕНИЕ ПРИНЯТО" else "ОСТАВИЛИ БЕЗ ИЗМЕНЕНИЙ",
                            color = if (status == "APPLIED") AccentLime else TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                reaction.shortMessage,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium
            )
            reaction.nextSetSuggestion?.rationale?.let {
                Text(it, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }

            if (status == "PENDING" && reaction.nextSetSuggestion != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SecondaryButton(
                        text = "Оставить",
                        modifier = Modifier.weight(1f),
                        hapticOnClick = HapticType.Soft,
                        onClick = onDecline
                    )
                    PrimaryButton(
                        text = "Принять",
                        modifier = Modifier.weight(1f),
                        hapticOnClick = HapticType.Soft,
                        onClick = onAccept
                    )
                }
            }
        }
    }
}

@Composable
private fun RestTimerHero(
    seconds: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "ОТДЫХ",
                color = TextSecondary,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "%d:%02d".format(seconds / 60, seconds % 60),
                color = AccentLime,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 96.sp,
                style = MaterialTheme.typography.displayLarge
            )
        }
    }
}

@Composable
private fun CoachContentCard(
    type: CoachContentType,
    content: CoachContentItem,
    onClick: () -> Unit
) {
    FormaCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        padding = PaddingValues(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(type.iconRes()),
                    contentDescription = null,
                    tint = Color.Unspecified
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    type.displayName.uppercase(),
                    color = AccentLime,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                content.text,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
