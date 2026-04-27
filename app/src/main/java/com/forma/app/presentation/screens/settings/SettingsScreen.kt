package com.forma.app.presentation.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.forma.app.presentation.components.FormaCard
import com.forma.app.presentation.components.PrimaryButton
import com.forma.app.presentation.components.SecondaryButton
import com.forma.app.presentation.theme.AccentLime
import com.forma.app.presentation.theme.BgBlack
import com.forma.app.presentation.theme.BorderSubtle
import com.forma.app.presentation.theme.ErrorRed
import com.forma.app.presentation.theme.SurfaceDark
import com.forma.app.presentation.theme.SurfaceHighlight
import com.forma.app.presentation.theme.TextPrimary
import com.forma.app.presentation.theme.TextSecondary
import com.forma.app.presentation.theme.TextTertiary

@Composable
fun SettingsScreen(
    vm: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val s by vm.ui.collectAsState()
    val suggestedName = remember { vm.suggestExportFileName() }

    // SAF launcher для экспорта — юзер выбирает папку и имя
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) vm.export(uri) else vm.resetStatus()
    }

    // SAF launcher для импорта — юзер выбирает JSON файл
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) vm.import(uri) else vm.resetStatus()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BgBlack)
            .systemBarsPadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            TopBar(onBack)

            Column(
                Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BackupSection(
                    suggestedName = suggestedName,
                    isWorking = s.status is BackupStatus.Working,
                    onExport = { exportLauncher.launch(suggestedName) },
                    onImport = { importLauncher.launch(arrayOf("application/json", "*/*")) }
                )

                StatusCard(s.status)

                Spacer(Modifier.height(24.dp))
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
                "НАСТРОЙКИ",
                color = AccentLime,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                "Настройки",
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.headlineLarge
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun BackupSection(
    suggestedName: String,
    isWorking: Boolean,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    FormaCard(padding = PaddingValues(20.dp)) {
        Column {
            Text(
                "Резервная копия",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Сохрани все свои данные в файл — программу, тренировки, разборы. " +
                    "Перед обновлением приложения экспортируй, после — импортируй.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(20.dp))

            PrimaryButton(
                text = if (isWorking) "Работаю…" else "Экспорт в файл",
                leadingIcon = Icons.Rounded.Download,
                enabled = !isWorking,
                onClick = onExport
            )
            Spacer(Modifier.height(8.dp))
            SecondaryButton(
                text = "Импорт из файла",
                onClick = onImport
            )

            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceHighlight)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(
                    "Имя файла будет: $suggestedName",
                    color = TextTertiary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun StatusCard(status: BackupStatus) {
    when (status) {
        BackupStatus.Idle -> Unit
        BackupStatus.Working -> StatusBox(
            icon = null,
            iconTint = AccentLime,
            title = "Работаю…",
            text = "Не выходи из экрана пока операция не завершится."
        )
        is BackupStatus.ExportSuccess -> StatusBox(
            icon = Icons.Rounded.Check,
            iconTint = AccentLime,
            title = "Экспортировано",
            text = "Файл «${status.fileName}» сохранён. Перед обновлением приложения " +
                "проверь что файл лежит в надёжном месте."
        )
        is BackupStatus.ImportSuccess -> StatusBox(
            icon = Icons.Rounded.Check,
            iconTint = AccentLime,
            title = "Восстановлено",
            text = buildString {
                if (status.summary.profileLoaded) append("профиль ✓ ")
                if (status.summary.programLoaded) append("программа ✓ ")
                append("сессий: ${status.summary.sessionsImported} ")
                append("разборов: ${status.summary.reviewsImported} ")
                if (status.summary.overridesImported > 0)
                    append("замен: ${status.summary.overridesImported}")
            }
        )
        is BackupStatus.Error -> StatusBox(
            icon = null,
            iconTint = ErrorRed,
            title = "Ошибка",
            text = status.message,
            titleColor = ErrorRed
        )
    }
}

@Composable
private fun StatusBox(
    icon: ImageVector?,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    text: String,
    titleColor: androidx.compose.ui.graphics.Color = TextPrimary
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                } else {
                    CircularProgressIndicator(
                        color = iconTint,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    title,
                    color = titleColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(text, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}
