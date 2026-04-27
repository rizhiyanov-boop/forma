package com.forma.app.presentation.screens.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.forma.app.domain.model.ExerciseLog
import com.forma.app.domain.model.SetLog
import com.forma.app.domain.model.WorkoutSession
import com.forma.app.presentation.components.FormaCard
import com.forma.app.presentation.components.PrimaryButton
import com.forma.app.presentation.theme.AccentDark
import com.forma.app.presentation.theme.AccentLime
import com.forma.app.presentation.theme.BgBlack
import com.forma.app.presentation.theme.BorderFocused
import com.forma.app.presentation.theme.BorderSubtle
import com.forma.app.presentation.theme.NumericFont
import com.forma.app.presentation.theme.SurfaceDark
import com.forma.app.presentation.theme.SurfaceHighlight
import com.forma.app.presentation.theme.TextPrimary
import com.forma.app.presentation.theme.TextSecondary
import com.forma.app.presentation.theme.TextTertiary

@Composable
fun WorkoutScreen(
    sessionId: String,
    vm: WorkoutViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onFinished: (reviewId: String?) -> Unit
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

        Column(Modifier.fillMaxSize()) {
            // Топ-бар
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
                        "Тренировка",
                        color = TextTertiary,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        "${s.completedSetsCount} из ${s.totalSetsCount} подходов",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                VolumeBadge(volume = s.totalVolumeKg)
                Spacer(Modifier.width(12.dp))
            }

            // Прогресс-полоса
            ProgressBar(ratio = s.completionRatio)

            // Список упражнений
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(s.exerciseLogs) { log ->
                    ExerciseCard(
                        log = log,
                        onSetUpdate = { setId, reps, weight, rir, done ->
                            vm.updateSet(setId, reps, weight, rir, done)
                        },
                        onRestStart = { restSec -> vm.startRest(restSec) }
                    )
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton(
                        text = if (ui.isFinishing) "Анализирую тренировку…" else "Завершить тренировку",
                        enabled = !ui.isFinishing,
                        onClick = { vm.finish(onFinished) }
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // Таймер отдыха — всплывающая плашка снизу
        AnimatedVisibility(
            visible = ui.restCountdown != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            RestTimerCard(
                seconds = ui.restCountdown ?: 0,
                onSkip = vm::skipRest
            )
        }
    }
}

@Composable
private fun VolumeBadge(volume: Double) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceHighlight)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            "${volume.toInt()} кг",
            color = AccentLime,
            fontFamily = NumericFont,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun ProgressBar(ratio: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(BorderSubtle)
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
private fun ExerciseCard(
    log: ExerciseLog,
    onSetUpdate: (setId: String, reps: Int, weight: Double?, rir: Int?, done: Boolean) -> Unit,
    onRestStart: (restSec: Int) -> Unit
) {
    FormaCard(padding = PaddingValues(16.dp)) {
        Column {
            Text(
                log.exerciseName,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            if (!log.notes.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(log.notes!!, color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(14.dp))

            // Заголовки таблицы подходов
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ColHeader("#", Modifier.width(28.dp))
                ColHeader("Вес", Modifier.weight(1.2f))
                Spacer(Modifier.width(6.dp))
                ColHeader("Повт.", Modifier.weight(1f))
                Spacer(Modifier.width(6.dp))
                ColHeader("RIR", Modifier.weight(0.7f))
                Spacer(Modifier.width(6.dp))
                Spacer(Modifier.size(40.dp))
            }

            Spacer(Modifier.height(6.dp))

            log.sets.forEach { set ->
                SetRow(
                    set = set,
                    onUpdate = { reps, weight, rir, done ->
                        onSetUpdate(set.id, reps, weight, rir, done)
                        if (done && !set.isCompleted) {
                            onRestStart(defaultRestForLog())
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// Примерный отдых по умолчанию, если точное значение из Exercise не передано
private fun defaultRestForLog(): Int = 90

@Composable
private fun ColHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text, color = TextTertiary,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
    )
}

@Composable
private fun SetRow(
    set: SetLog,
    onUpdate: (reps: Int, weight: Double?, rir: Int?, done: Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    var repsText by remember(set.id, set.isCompleted) {
        mutableStateOf(if (set.reps == 0) "" else set.reps.toString())
    }
    var weightText by remember(set.id, set.isCompleted) {
        mutableStateOf(set.weightKg?.takeIf { it > 0 }?.let { formatWeight(it) } ?: "")
    }
    var rirText by remember(set.id, set.isCompleted) {
        mutableStateOf(set.rir?.toString() ?: "")
    }

    val usesWeight = set.weightKg != null || set.targetWeightKg != null
    // Опциональные подходы — приглушённый цвет
    val optAlpha = if (set.isOptional && !set.isCompleted) 0.55f else 1f

    Column(modifier = Modifier.fillMaxWidth().alpha(optAlpha)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "${set.setNumber}",
                color = TextSecondary,
                fontFamily = NumericFont,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(28.dp),
                style = MaterialTheme.typography.titleMedium
            )

            // Вес
            if (usesWeight) {
                CompactField(
                    value = weightText,
                    onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1.2f),
                    suffix = "кг",
                    placeholder = set.targetWeightKg?.let { formatWeight(it) },
                    enabled = !set.isCompleted
                )
            } else {
                Box(Modifier.weight(1.2f).height(44.dp), contentAlignment = Alignment.Center) {
                    Text("—", color = TextTertiary)
                }
            }

            Spacer(Modifier.width(6.dp))

            // Повторы — placeholder с целевым диапазоном
            CompactField(
                value = repsText,
                onValueChange = { repsText = it.filter { c -> c.isDigit() } },
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
                placeholder = buildRepsPlaceholder(set),
                enabled = !set.isCompleted
            )

            Spacer(Modifier.width(6.dp))

            // RIR
            CompactField(
                value = rirText,
                onValueChange = { rirText = it.filter { c -> c.isDigit() }.take(2) },
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(0.7f),
                placeholder = "—",
                enabled = !set.isCompleted
            )

            Spacer(Modifier.width(6.dp))

            // Чекбокс завершения
            CompletedToggle(
                checked = set.isCompleted,
                onToggle = {
                    val reps = repsText.toIntOrNull() ?: 0
                    val w = if (usesWeight) weightText.replace(',', '.').toDoubleOrNull() else null
                    val rir = rirText.toIntOrNull()?.coerceIn(0, 10)
                    val newState = !set.isCompleted
                    if (newState && reps == 0) return@CompletedToggle
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onUpdate(reps, w, rir, newState)
                }
            )
        }
        if (set.isOptional && !set.isCompleted) {
            Text(
                "опциональный",
                color = TextTertiary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 28.dp, top = 2.dp)
            )
        }
    }
}

private fun buildRepsPlaceholder(set: SetLog): String? {
    val min = set.targetRepsMin
    val max = set.targetRepsMax
    return when {
        min == null || max == null -> null
        min == max -> "$min"
        else -> "$min-$max"
    }
}

private fun formatWeight(v: Double): String {
    // Без лишних нулей: 60.0 -> "60", 22.5 -> "22.5"
    return if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
}

@Composable
private fun CompactField(
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(48.dp),
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = NumericFont),
        placeholder = if (placeholder != null) {
            { Text(placeholder, color = TextTertiary,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = NumericFont)) }
        } else null,
        suffix = if (suffix != null) {
            { Text(suffix, color = TextTertiary, style = MaterialTheme.typography.labelSmall) }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            disabledTextColor = TextSecondary,
            focusedBorderColor = AccentLime,
            unfocusedBorderColor = BorderFocused,
            disabledBorderColor = BorderSubtle,
            focusedContainerColor = SurfaceDark,
            unfocusedContainerColor = SurfaceDark,
            disabledContainerColor = SurfaceDark,
            cursorColor = AccentLime
        )
    )
}

@Composable
private fun CompletedToggle(checked: Boolean, onToggle: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (checked) AccentLime else SurfaceHighlight)
            .border(
                1.dp,
                if (checked) AccentLime else BorderFocused,
                CircleShape
            )
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.Check, null,
            tint = if (checked) AccentDark else TextTertiary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun RestTimerCard(seconds: Int, onSkip: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceHighlight)
            .border(1.dp, AccentLime, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Timer, null, tint = AccentLime, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Отдых",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    formatSeconds(seconds),
                    color = AccentLime,
                    fontFamily = NumericFont,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            IconButton(onClick = onSkip) {
                Icon(Icons.Rounded.Close, null, tint = TextSecondary)
            }
        }
    }
}

private fun formatSeconds(s: Int): String {
    val m = s / 60
    val sec = s % 60
    return "%d:%02d".format(m, sec)
}
