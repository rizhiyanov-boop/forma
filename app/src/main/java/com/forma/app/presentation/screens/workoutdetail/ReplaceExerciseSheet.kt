package com.forma.app.presentation.screens.workoutdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.forma.app.domain.model.Exercise
import com.forma.app.presentation.components.PrimaryButton
import com.forma.app.presentation.components.SecondaryButton
import com.forma.app.presentation.theme.AccentLime
import com.forma.app.presentation.theme.BgBlack
import com.forma.app.presentation.theme.BorderFocused
import com.forma.app.presentation.theme.BorderSubtle
import com.forma.app.presentation.theme.ErrorRed
import com.forma.app.presentation.theme.SurfaceDark
import com.forma.app.presentation.theme.SurfaceElevated
import com.forma.app.presentation.theme.SurfaceHighlight
import com.forma.app.presentation.theme.TextPrimary
import com.forma.app.presentation.theme.TextSecondary
import com.forma.app.presentation.theme.TextTertiary

/**
 * Bottom sheet замены упражнения. Состоит из 3 фаз:
 *  1) ввод опциональной причины + кнопка «Найти замены»
 *  2) список вариантов от AI
 *  3) после выбора варианта — выбор «Разово» / «Навсегда»
 */
@Composable
fun ReplaceExerciseSheet(
    state: WorkoutDetailUi,
    onClose: () -> Unit,
    onReasonChange: (String) -> Unit,
    onFetch: () -> Unit,
    onSelect: (Exercise) -> Unit,
    onCancelPending: () -> Unit,
    onApplyPermanent: () -> Unit,
    onApplyOnce: () -> Unit
) {
    val origin = state.replaceDialogFor ?: return

    // Затемнение фона
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(enabled = state.pendingReplacement == null) { onClose() }
    ) {
        // Сам sheet — не реагирует на клик по фону
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(SurfaceElevated)
                .clickable(enabled = false) {}    // блокирует прокидывание клика
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // "Палочка" сверху
                Box(
                    Modifier
                        .padding(top = 10.dp)
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(BorderFocused)
                )
                Spacer(Modifier.height(20.dp))

                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ЗАМЕНА УПРАЖНЕНИЯ",
                            color = TextTertiary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(origin.name,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineSmall)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, null, tint = TextSecondary)
                    }
                }
                Spacer(Modifier.height(20.dp))

                // Фаза 3: подтверждение «разово или навсегда»
                if (state.pendingReplacement != null) {
                    PendingConfirmation(
                        replacement = state.pendingReplacement!!,
                        onCancel = onCancelPending,
                        onApplyPermanent = onApplyPermanent,
                        onApplyOnce = onApplyOnce
                    )
                    return@Column
                }

                // Фаза 2: список вариантов
                if (state.replaceSuggestions.isNotEmpty()) {
                    if (!state.replaceReasoning.isNullOrBlank()) {
                        ReasoningCard(state.replaceReasoning)
                        Spacer(Modifier.height(12.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.replaceSuggestions.forEach { suggestion ->
                            SuggestionTile(
                                exercise = suggestion,
                                onClick = { onSelect(suggestion) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    return@Column
                }

                // Фаза 1: ввод причины + загрузка
                Text(
                    "Хочешь — расскажи почему. Так точнее.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))

                // Чипы быстрого выбора
                ReasonChips(state.replaceReason, onReasonChange)
                Spacer(Modifier.height(10.dp))

                // Поле для своей причины
                OutlinedTextField(
                    value = state.replaceReason,
                    onValueChange = onReasonChange,
                    placeholder = { Text("Например: болит плечо, нет скамьи", color = TextTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentLime,
                        unfocusedBorderColor = BorderFocused,
                        cursorColor = AccentLime,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    )
                )

                Spacer(Modifier.height(20.dp))

                if (state.replaceError != null) {
                    Text(
                        state.replaceError,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                if (state.replaceLoading) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = AccentLime, strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Поиск замен…", color = TextSecondary)
                    }
                } else {
                    PrimaryButton(
                        text = "Найти замены",
                        onClick = onFetch
                    )
                }
            }
        }
    }
}

@Composable
private fun ReasonChips(current: String, onChange: (String) -> Unit) {
    val chips = listOf("Боль или травма", "Нет инвентаря", "Слишком сложно", "Скучно")
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        chips.forEach { label ->
            val selected = current.equals(label, ignoreCase = true)
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) SurfaceHighlight else SurfaceDark)
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) AccentLime else BorderSubtle,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onChange(label) }
                    .padding(vertical = 10.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = TextPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun ReasoningCard(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Text("AI: ", color = AccentLime, style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(text, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SuggestionTile(exercise: Exercise, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Text(exercise.name, color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            val reps = if (exercise.targetRepsMin == exercise.targetRepsMax) "${exercise.targetRepsMin}"
            else "${exercise.targetRepsMin}-${exercise.targetRepsMax}"
            Text(
                "${exercise.primaryMuscle.displayName} · ${exercise.targetSets}×$reps · ${exercise.restSeconds}с",
                color = TextTertiary,
                style = MaterialTheme.typography.labelMedium
            )
            if (!exercise.description.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(exercise.description!!,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3)
            }
        }
    }
}

@Composable
private fun PendingConfirmation(
    replacement: Exercise,
    onCancel: () -> Unit,
    onApplyPermanent: () -> Unit,
    onApplyOnce: () -> Unit
) {
    Column {
        Text("ВЫБРАН ВАРИАНТ", color = TextTertiary,
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(replacement.name, color = TextPrimary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(20.dp))

        Text("Применить замену:",
            color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))

        PrimaryButton(
            text = "Только сегодня",
            onClick = onApplyOnce
        )
        Spacer(Modifier.height(8.dp))
        SecondaryButton(
            text = "Заменить навсегда",
            onClick = onApplyPermanent
        )
        Spacer(Modifier.height(8.dp))
        SecondaryButton(
            text = "Отмена",
            onClick = onCancel
        )
    }
}
