package com.forma.app.presentation.screens.workoutdetail

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SwapHoriz
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.forma.app.domain.model.Exercise
import com.forma.app.domain.model.Workout
import com.forma.app.presentation.components.FormaCard
import com.forma.app.presentation.components.PrimaryButton
import com.forma.app.presentation.theme.AccentLime
import com.forma.app.presentation.theme.BgBlack
import com.forma.app.presentation.theme.NumericFont
import com.forma.app.presentation.theme.SurfaceDark
import com.forma.app.presentation.theme.SurfaceHighlight
import com.forma.app.presentation.theme.TextPrimary
import com.forma.app.presentation.theme.TextSecondary
import com.forma.app.presentation.theme.TextTertiary

@Composable
fun WorkoutDetailScreen(
    vm: WorkoutDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onOpenExercise: (workoutId: String, exerciseId: String) -> Unit,
    onSessionStarted: (sessionId: String) -> Unit
) {
    val s by vm.ui.collectAsState()

    Box(
        Modifier
            .fillMaxSize()
            .background(BgBlack)
            .systemBarsPadding()
    ) {
        if (s.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentLime, strokeWidth = 2.dp)
            }
            return@Box
        }

        val workout = s.workout
        if (workout == null) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = TextPrimary)
                }
                Spacer(Modifier.height(40.dp))
                Text(
                    "Тренировка не найдена",
                    color = TextPrimary, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            return@Box
        }

        Column(Modifier.fillMaxSize()) {
            TopBar(workout, onBack)

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(workout.exercises) { ex ->
                    ExerciseRow(
                        exercise = ex,
                        onOpen = { onOpenExercise(workout.id, ex.id) },
                        onReplace = { vm.openReplaceDialog(ex) }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // Кнопка старта сессии
            Column(Modifier.padding(20.dp)) {
                PrimaryButton(
                    text = "Начать тренировку",
                    leadingIcon = Icons.Rounded.PlayArrow,
                    onClick = { vm.startSession(onSessionStarted) }
                )
            }
        }

        // Bottom sheet замены упражнения
        if (s.replaceDialogFor != null) {
            ReplaceExerciseSheet(
                state = s,
                onClose = vm::closeReplaceDialog,
                onReasonChange = vm::setReplaceReason,
                onFetch = vm::fetchReplacements,
                onSelect = vm::selectReplacement,
                onCancelPending = vm::cancelPending,
                onApplyPermanent = { vm.applyPermanent(onDone = {}) },
                onApplyOnce = { vm.applyOnceAndStart(onSessionStarted) }
            )
        }
    }
}

@Composable
private fun TopBar(workout: Workout, onBack: () -> Unit) {
    Column(Modifier.padding(horizontal = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, null, tint = TextPrimary)
            }
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Text(
                workout.dayOfWeek.full.uppercase(),
                color = AccentLime,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                workout.title,
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "${workout.focus} · ${workout.estimatedMinutes} мин · ${workout.exercises.size} упражнений",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ExerciseRow(
    exercise: Exercise,
    onOpen: () -> Unit,
    onReplace: () -> Unit
) {
    FormaCard(padding = PaddingValues(0.dp)) {
        Column(Modifier.fillMaxWidth()) {
            // Основная зона — клик открывает детали упражнения
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MuscleBadge(exercise)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        exercise.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        buildSetsLabel(exercise),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Icon(
                    Icons.Rounded.ChevronRight, null,
                    tint = TextTertiary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Тонкая разделительная полоска и кнопка замены
            Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceDark))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReplace() }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.SwapHoriz, null,
                    tint = AccentLime,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Заменить упражнение",
                    color = AccentLime,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun MuscleBadge(exercise: Exercise) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(SurfaceHighlight),
        contentAlignment = Alignment.Center
    ) {
        Text(
            exercise.primaryMuscle.displayName.take(2).uppercase(),
            color = AccentLime,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

private fun buildSetsLabel(ex: Exercise): String {
    val reps = if (ex.targetRepsMin == ex.targetRepsMax) "${ex.targetRepsMin}"
    else "${ex.targetRepsMin}-${ex.targetRepsMax}"
    val weightPart = if (ex.usesWeight) "" else " · без веса"
    val rest = "${ex.restSeconds}с отдых"
    return "${ex.targetSets}×$reps · $rest$weightPart"
}
