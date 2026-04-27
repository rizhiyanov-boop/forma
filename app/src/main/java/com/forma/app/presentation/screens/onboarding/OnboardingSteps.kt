package com.forma.app.presentation.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.forma.app.domain.model.DayOfWeek
import com.forma.app.domain.model.Equipment
import com.forma.app.domain.model.ExperienceLevel
import com.forma.app.domain.model.Goal
import com.forma.app.domain.model.Sex
import com.forma.app.presentation.components.FormaCard
import com.forma.app.presentation.components.SelectableTile
import com.forma.app.presentation.theme.AccentDark
import com.forma.app.presentation.theme.AccentLime
import com.forma.app.presentation.theme.BorderFocused
import com.forma.app.presentation.theme.BorderSubtle
import com.forma.app.presentation.theme.NumericFont
import com.forma.app.presentation.theme.SurfaceDark
import com.forma.app.presentation.theme.SurfaceHighlight
import com.forma.app.presentation.theme.TextPrimary
import com.forma.app.presentation.theme.TextSecondary

@Composable
fun StepGoal(s: OnboardingUi, vm: OnboardingViewModel) {
    StepHeader("Какая цель?", "Это определит направление программы")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Goal.entries.forEach { g ->
            SelectableTile(
                title = g.displayName,
                subtitle = subtitleFor(g),
                selected = s.goal == g,
                onClick = { vm.setGoal(g) }
            )
        }
    }
}

private fun subtitleFor(g: Goal) = when (g) {
    Goal.MUSCLE_GAIN -> "Гипертрофия, рабочий диапазон 8-12"
    Goal.STRENGTH -> "Большие веса, 3-6 повторов"
    Goal.FAT_LOSS -> "Интенсивно, круговой формат"
    Goal.ENDURANCE -> "Больше повторов, меньше отдыха"
    Goal.GENERAL_FITNESS -> "Баланс силы и выносливости"
}

@Composable
fun StepSex(s: OnboardingUi, vm: OnboardingViewModel) {
    StepHeader(
        "Пол",
        "Влияет на расчёт прогрессии и тон рекомендаций. Можно не указывать."
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Sex.entries.forEach { x ->
            SelectableTile(
                title = x.displayName,
                subtitle = subtitleFor(x),
                selected = s.sex == x,
                onClick = { vm.setSex(x) }
            )
        }
    }
}

private fun subtitleFor(x: Sex) = when (x) {
    Sex.MALE -> "Мужская модель прогрессии"
    Sex.FEMALE -> "Женская модель — больше выносливости верха, мягче шаги"
    Sex.UNSPECIFIED -> "Усреднённая модель без специфики пола"
}

@Composable
fun StepLevel(s: OnboardingUi, vm: OnboardingViewModel) {
    StepHeader("Уровень подготовки", "Чтобы подобрать правильную интенсивность")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ExperienceLevel.entries.forEach { l ->
            SelectableTile(
                title = l.displayName,
                subtitle = subtitleFor(l),
                selected = s.level == l,
                onClick = { vm.setLevel(l) }
            )
        }
    }
}

private fun subtitleFor(l: ExperienceLevel) = when (l) {
    ExperienceLevel.BEGINNER -> "Тренируюсь менее 6 месяцев"
    ExperienceLevel.INTERMEDIATE -> "От полугода до двух лет"
    ExperienceLevel.ADVANCED -> "Более двух лет системных тренировок"
}

@Composable
fun StepDays(s: OnboardingUi, vm: OnboardingViewModel) {
    StepHeader("Сколько дней в неделю?", "И какие — если есть предпочтения")

    // Счётчик дней
    FormaCard {
        Column {
            Text("Тренировочных дней", color = TextSecondary,
                style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepperButton(icon = Icons.Rounded.Remove) {
                    if (s.daysPerWeek > 1) vm.setDays(s.daysPerWeek - 1)
                }
                Spacer(Modifier.width(24.dp))
                Text(
                    s.daysPerWeek.toString(),
                    fontFamily = NumericFont,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary
                )
                Spacer(Modifier.width(24.dp))
                StepperButton(icon = Icons.Rounded.Add) {
                    if (s.daysPerWeek < 7) vm.setDays(s.daysPerWeek + 1)
                }
            }
        }
    }

    Spacer(Modifier.height(20.dp))

    Text("Предпочтительные дни (не обязательно)",
        color = TextSecondary, style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(10.dp))
    DayPicker(selected = s.preferredDays, onToggle = vm::togglePreferredDay)

    Spacer(Modifier.height(24.dp))

    Text("Длительность тренировки",
        color = TextSecondary, style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(10.dp))
    FormaCard {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(30, 45, 60, 90).forEach { minutes ->
                DurationChip(
                    label = "$minutes мин",
                    selected = s.sessionDurationMin == minutes,
                    onClick = { vm.setDuration(minutes) }
                )
            }
        }
    }
}

@Composable
private fun StepperButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceHighlight)
            .border(1.dp, BorderFocused, RoundedCornerShape(22.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = TextPrimary) }
}

@Composable
private fun DurationChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) AccentLime else SurfaceHighlight)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            color = if (selected) AccentDark else TextPrimary,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun DayPicker(selected: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        DayOfWeek.entries.forEach { d ->
            val isSel = d in selected
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSel) AccentLime else SurfaceDark)
                    .border(
                        1.dp,
                        if (isSel) AccentLime else BorderSubtle,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { onToggle(d) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    d.short,
                    color = if (isSel) AccentDark else TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepEquipment(s: OnboardingUi, vm: OnboardingViewModel) {
    StepHeader("Что есть в твоём зале?", "Выбери всё, что доступно")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Equipment.entries.forEach { e ->
            val isSel = e in s.equipment
            Box(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSel) SurfaceHighlight else SurfaceDark)
                    .border(
                        width = if (isSel) 1.5.dp else 1.dp,
                        color = if (isSel) AccentLime else BorderSubtle,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { vm.toggleEquipment(e) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    e.displayName,
                    color = TextPrimary,
                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun StepBody(s: OnboardingUi, vm: OnboardingViewModel) {
    StepHeader("Немного о тебе", "Всё опционально, поможет точнее подобрать веса")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NumberField(label = "Возраст", value = s.age, onChange = vm::setAge)
        NumberField(label = "Рост, см", value = s.heightCm, onChange = vm::setHeight)
        NumberField(
            label = "Вес, кг", value = s.weightKg, onChange = vm::setWeight,
            allowDecimal = true
        )
        TextArea(
            label = "Ограничения или травмы",
            value = s.limitations,
            onChange = vm::setLimitations
        )
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, allowDecimal: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, color = TextSecondary) },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (allowDecimal) KeyboardType.Decimal else KeyboardType.Number
        ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = outlinedColors()
    )
}

@Composable
private fun TextArea(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, color = TextSecondary) },
        modifier = Modifier.fillMaxWidth().height(110.dp),
        colors = outlinedColors(),
        maxLines = 4
    )
}

@Composable
private fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = AccentLime,
    unfocusedBorderColor = BorderFocused,
    cursorColor = AccentLime,
    focusedContainerColor = SurfaceDark,
    unfocusedContainerColor = SurfaceDark
)

@Composable
fun StepSummary(s: OnboardingUi) {
    StepHeader("Почти готово", "Проверь вводные и жми «Создать программу»")

    FormaCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryRow("Цель", s.goal?.displayName ?: "—")
            SummaryRow("Уровень", s.level?.displayName ?: "—")
            SummaryRow("Дней/неделю", s.daysPerWeek.toString())
            SummaryRow("Длительность", "${s.sessionDurationMin} мин")
            SummaryRow("Оборудование", s.equipment.joinToString(", ") { it.displayName }.ifBlank { "—" })
            if (s.preferredDays.isNotEmpty()) {
                SummaryRow("Дни", s.preferredDays.sortedBy { it.ordinal }.joinToString(", ") { it.short })
            }
            if (s.age.isNotBlank() || s.heightCm.isNotBlank() || s.weightKg.isNotBlank()) {
                val parts = listOfNotNull(
                    s.age.takeIf { it.isNotBlank() }?.let { "$it лет" },
                    s.heightCm.takeIf { it.isNotBlank() }?.let { "$it см" },
                    s.weightKg.takeIf { it.isNotBlank() }?.let { "$it кг" }
                ).joinToString(" · ")
                SummaryRow("Антропометрия", parts)
            }
            if (s.limitations.isNotBlank()) SummaryRow("Ограничения", s.limitations)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelLarge)
        Text(value, color = TextPrimary, fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 16.dp))
    }
}
