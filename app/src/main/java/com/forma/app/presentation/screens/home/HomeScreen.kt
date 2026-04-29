package com.forma.app.presentation.screens.home

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Settings
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
import com.forma.app.domain.model.DayOfWeek
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    vm: HomeViewModel = hiltViewModel(),
    onOpenWorkout: (workoutId: String) -> Unit,
    onOpenProgress: () -> Unit,
    onOpenSettings: () -> Unit
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Greeting(s.weekday, onOpenSettings) }
            item { TodayCard(s, onOpen = {
                val w = s.today ?: return@TodayCard
                onOpenWorkout(w.id)
            }) }
            item {
                Box(Modifier.clickable { onOpenProgress() }) {
                    StatsRow(
                        streak = s.streak,
                        workouts = s.thisWeekSessions.size,
                        volume = s.volumeKg
                    )
                }
            }
            item { ProgramHeader(s.program?.name ?: "Программа", s.program?.description) }
            items(s.program?.workouts.orEmpty()) { workout ->
                WorkoutRow(
                    workout = workout,
                    isToday = workout.dayOfWeek == s.weekday,
                    isDone = s.thisWeekSessions.any { it.workoutId == workout.id && it.isFinished },
                    onClick = { onOpenWorkout(workout.id) }
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun Greeting(today: DayOfWeek, onOpenSettings: () -> Unit) {
    val dateStr = SimpleDateFormat("d MMMM", Locale("ru")).format(Date())
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(
                dateStr.replaceFirstChar { it.uppercase() },
                color = TextTertiary,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(
                today.full,
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.displayMedium
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = "Настройки",
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun TodayCard(s: HomeUi, onOpen: () -> Unit) {
    val today = s.today
    FormaCard(padding = PaddingValues(20.dp)) {
        Column {
            Text(
                "СЕГОДНЯ",
                color = AccentLime,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(6.dp))
            if (today == null) {
                Text("День отдыха", color = TextPrimary, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text("Отдых тоже часть тренировки", color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(today.title, color = TextPrimary, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${today.focus} · ${today.estimatedMinutes} мин · ${today.exercises.size} упражнений",
                    color = TextSecondary, style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(20.dp))
                PrimaryButton(
                    text = "Открыть тренировку",
                    leadingIcon = Icons.Rounded.PlayArrow,
                    onClick = onOpen
                )
            }
        }
    }
}

@Composable
private fun StatsRow(streak: Int, workouts: Int, volume: Double) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            icon = Icons.Rounded.LocalFireDepartment,
            value = streak.toString(),
            label = "Подряд",
            highlighted = streak > 0,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Rounded.FitnessCenter,
            value = workouts.toString(),
            label = "За неделю",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Rounded.BarChart,
            value = volume.toInt().toString(),
            label = "кг объём",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
) {
    FormaCard(modifier = modifier, padding = PaddingValues(14.dp)) {
        Column {
            Icon(
                icon, null,
                tint = if (highlighted) AccentLime else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                value, color = TextPrimary,
                fontFamily = NumericFont, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(label, color = TextTertiary, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ProgramHeader(name: String, description: String?) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(
            "Программа",
            color = TextTertiary,
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.height(4.dp))
        Text(name, color = TextPrimary, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall)
        if (!description.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(description, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun WorkoutRow(
    workout: Workout,
    isToday: Boolean,
    isDone: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        FormaCard(elevated = isToday) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isToday || isDone) SurfaceDark else SurfaceDark),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(Icons.Rounded.Check, null, tint = AccentLime)
                    } else {
                        Text(
                            workout.dayOfWeek.short,
                            color = if (isToday) AccentLime else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(workout.title, color = TextPrimary, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${workout.focus} · ${workout.estimatedMinutes} мин",
                        color = TextSecondary, style = MaterialTheme.typography.bodySmall
                    )
                }
                if (isDone) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceHighlight)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Готово",
                            color = AccentLime, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
