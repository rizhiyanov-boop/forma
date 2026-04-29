package com.forma.app.presentation.screens.exercisedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Timer
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
import com.forma.app.presentation.components.FormaCard
import com.forma.app.presentation.theme.AccentLime
import com.forma.app.presentation.theme.BgBlack
import com.forma.app.presentation.theme.BorderSubtle
import com.forma.app.presentation.theme.NumericFont
import com.forma.app.presentation.theme.SurfaceDark
import com.forma.app.presentation.theme.SurfaceHighlight
import com.forma.app.presentation.theme.TextPrimary
import com.forma.app.presentation.theme.TextSecondary
import com.forma.app.presentation.theme.TextTertiary

@Composable
fun ExerciseDetailScreen(
    vm: ExerciseDetailViewModel = hiltViewModel(),
    onBack: () -> Unit
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
        val ex = s.exercise
        if (ex == null) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = TextPrimary)
                }
                Spacer(Modifier.height(40.dp))
                Text("Упражнение не найдено",
                    color = TextPrimary, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMedium)
            }
            return@Box
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Топ-бар
            Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = TextPrimary)
                }
            }

            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    ex.primaryMuscle.displayName.uppercase(),
                    color = AccentLime,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    ex.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.displaySmall
                )
            }

            Spacer(Modifier.height(20.dp))

            // Заглушка под видео/GIF — на будущее
            Column(Modifier.padding(horizontal = 20.dp)) {
                MediaPlaceholder()
            }

            Spacer(Modifier.height(20.dp))

            // Метрики
            Column(Modifier.padding(horizontal = 20.dp)) {
                MetricsGrid(ex)
            }

            Spacer(Modifier.height(20.dp))

            // Инструкция
            if (!ex.description.isNullOrBlank()) {
                Section(title = "Как выполнять") {
                    Text(ex.description!!,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // Подсказки от AI
            if (!ex.notes.isNullOrBlank()) {
                Section(title = "Подсказки тренера") {
                    Text(ex.notes!!,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // Доп. целевые группы
            if (ex.secondaryMuscles.isNotEmpty()) {
                Section(title = "Дополнительная нагрузка") {
                    Text(
                        ex.secondaryMuscles.joinToString(", ") { it.displayName },
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // Оборудование
            if (ex.equipment.isNotEmpty()) {
                Section(title = "Оборудование") {
                    Text(
                        ex.equipment.joinToString(", ") { it.displayName },
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun MediaPlaceholder() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.PlayCircle, null,
                tint = TextTertiary, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            Text("Видео-демонстрация",
                color = TextTertiary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold)
            Text("скоро будет",
                color = TextTertiary,
                style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun MetricsGrid(ex: Exercise) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricCard(
            value = ex.targetSets.toString(),
            label = "подходов",
            modifier = Modifier.weight(1f)
        )
        val reps = if (ex.targetRepsMin == ex.targetRepsMax) "${ex.targetRepsMin}"
        else "${ex.targetRepsMin}-${ex.targetRepsMax}"
        MetricCard(
            value = reps,
            label = "повторов",
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            value = "${ex.restSeconds}с",
            label = "отдых",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    FormaCard(modifier = modifier, padding = PaddingValues(14.dp)) {
        Column {
            Text(value,
                color = TextPrimary,
                fontFamily = NumericFont,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium)
            Text(label,
                color = TextTertiary,
                style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text(title.uppercase(),
            color = TextTertiary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold)
        content()
    }
}
