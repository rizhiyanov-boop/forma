package com.forma.app.presentation.screens.progress

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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.forma.app.domain.analytics.ExerciseProgressSeries
import com.forma.app.domain.analytics.MuscleGroupVolume
import com.forma.app.domain.analytics.ProgressData
import com.forma.app.domain.analytics.WeeklyAggregate
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
fun ProgressScreen(
    vm: ProgressViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val s by vm.ui.collectAsState()
    val selectedIdx by vm.selectedExerciseIdx.collectAsState()

    Box(
        Modifier
            .fillMaxSize()
            .background(BgBlack)
            .systemBarsPadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            TopBar(onBack)

            if (s.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentLime, strokeWidth = 2.dp)
                }
                return@Column
            }

            val data = s.data
            if (data == null || data.isEmpty) {
                EmptyState()
                return@Column
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { OverviewStrip(data) }
                item { VolumeSection(data.weeks) }
                if (data.keyExercises.isNotEmpty()) {
                    item {
                        ExerciseProgressSection(
                            seriesList = data.keyExercises,
                            selectedIndex = selectedIdx.coerceIn(0, data.keyExercises.size - 1),
                            onSelect = vm::selectExercise
                        )
                    }
                }
                if (data.muscleVolume.isNotEmpty()) {
                    item { MuscleBalanceSection(data.muscleVolume) }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, null, tint = TextPrimary)
        }
        Spacer(Modifier.width(4.dp))
        Column(Modifier.padding(vertical = 4.dp)) {
            Text(
                "ПРОГРЕСС",
                color = AccentLime,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                "Динамика",
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.headlineLarge
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun EmptyState() {
    Box(
        Modifier
            .fillMaxSize()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Нет данных для графиков",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Заверши хотя бы одну тренировку — здесь появится твой прогресс.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// ─── Обзор сверху ───────────────────────────────────────────────

@Composable
private fun OverviewStrip(data: ProgressData) {
    val totalVolume = data.weeks.sumOf { it.totalVolumeKg }
    val totalSessions = data.weeks.sumOf { it.sessionsCount }
    val avgRir = data.weeks.mapNotNull { it.avgRir }
        .takeIf { it.isNotEmpty() }
        ?.average()

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatTile(
            value = formatVolumeKg(totalVolume),
            label = "кг суммарно",
            modifier = Modifier.weight(1f)
        )
        StatTile(
            value = totalSessions.toString(),
            label = "тренировок",
            modifier = Modifier.weight(1f)
        )
        StatTile(
            value = avgRir?.let { "%.1f".format(it) } ?: "—",
            label = "ср. RIR",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    FormaCard(modifier = modifier, padding = PaddingValues(14.dp)) {
        Column {
            Text(
                value,
                color = TextPrimary,
                fontFamily = NumericFont,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                label,
                color = TextTertiary,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

// ─── Объём по неделям ────────────────────────────────────────────

@Composable
private fun VolumeSection(weeks: List<WeeklyAggregate>) {
    SectionCard(
        title = "Объём по неделям",
        subtitle = "Суммарный тоннаж выполненных подходов"
    ) {
        if (weeks.all { it.totalVolumeKg == 0.0 }) {
            EmptyChartHint()
        } else {
            LineChart(
                values = weeks.map { it.totalVolumeKg },
                xLabels = weeks.map { it.weekLabel },
                valueFormatter = { formatVolumeKg(it) }
            )
        }
    }
}

// ─── Прогресс по упражнениям ─────────────────────────────────────

@Composable
private fun ExerciseProgressSection(
    seriesList: List<ExerciseProgressSeries>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val current = seriesList[selectedIndex]
    SectionCard(
        title = "Рабочий вес",
        subtitle = "Максимум за тренировку"
    ) {
        // Селектор упражнений
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(seriesList.withIndex().toList()) { (i, s) ->
                ExerciseChip(
                    label = s.exerciseName,
                    selected = i == selectedIndex,
                    onClick = { onSelect(i) }
                )
            }
        }

        if (current.points.size < 2) {
            EmptyChartHint("Нужно минимум 2 тренировки")
        } else {
            LineChart(
                values = current.points.map { it.workingWeightKg },
                xLabels = current.points.map { formatShortDate(it.timestamp) },
                valueFormatter = { "${it.toInt()}" }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Последний: ${current.points.last().workingWeightKg.toInt()} кг × ${current.points.last().totalRepsAtMaxWeight}",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ExerciseChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) SurfaceHighlight else SurfaceDark)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) AccentLime else BorderSubtle,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            color = if (selected) TextPrimary else TextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

// ─── Объём по группам мышц ───────────────────────────────────────

@Composable
private fun MuscleBalanceSection(volumes: List<MuscleGroupVolume>) {
    SectionCard(
        title = "Группы мышц",
        subtitle = "Распределение нагрузки"
    ) {
        HorizontalBarChart(
            items = volumes.take(8).map {
                BarItem(
                    name = it.muscle.displayName,
                    value = it.volumeKg,
                    formattedValue = formatVolumeKg(it.volumeKg)
                )
            }
        )
    }
}

// ─── Универсальная карточка секции ───────────────────────────────

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    FormaCard(padding = PaddingValues(20.dp)) {
        Column {
            Text(
                title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun EmptyChartHint(text: String = "Пока недостаточно данных") {
    Box(
        Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TextTertiary, style = MaterialTheme.typography.bodyMedium)
    }
}

// ─── Утилиты форматирования ──────────────────────────────────────

private fun formatVolumeKg(v: Double): String = when {
    v >= 1000 -> "%.1fк".format(v / 1000)
    else -> v.toInt().toString()
}

private fun formatShortDate(ts: Long): String {
    val fmt = java.text.SimpleDateFormat("d.MM", java.util.Locale("ru"))
    return fmt.format(java.util.Date(ts))
}
