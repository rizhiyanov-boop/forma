package com.forma.app.presentation.screens.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.forma.app.presentation.components.FormaCard
import com.forma.app.presentation.components.PrimaryButton
import com.forma.app.presentation.components.SecondaryButton
import com.forma.app.presentation.theme.AccentLime
import com.forma.app.presentation.theme.BgBlack
import com.forma.app.presentation.theme.ErrorRed
import com.forma.app.presentation.theme.SurfaceHighlight
import com.forma.app.presentation.theme.TextPrimary
import com.forma.app.presentation.theme.TextSecondary
import com.forma.app.presentation.theme.TextTertiary

@Composable
fun WelcomeScreen(
    vm: WelcomeViewModel = hiltViewModel(),
    onChooseAi: () -> Unit,
    onPresetReady: () -> Unit
) {
    val s by vm.ui.collectAsState()

    Box(
        Modifier
            .fillMaxSize()
            .background(BgBlack)
            .systemBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(top = 48.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Column {
                Text(
                    "FORMA",
                    color = AccentLime,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "С чего начнём?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.displayMedium
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Загрузи готовую программу или попроси AI составить новую под твои цели.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(Modifier.height(36.dp))

                // Preset карточка — выделяется
                PresetCard()
                Spacer(Modifier.height(12.dp))

                // AI карточка
                AiCard()
            }

            // Нижняя зона с кнопками или прогрессом
            Column {
                if (s.errorMessage != null) {
                    Text(
                        s.errorMessage!!,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                if (s.isLoading) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = AccentLime, strokeWidth = 2.dp)
                        Spacer(Modifier.height(8.dp))
                        Text("Загружаю программу…",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    PrimaryButton(
                        text = "Загрузить мою программу",
                        leadingIcon = Icons.Rounded.Done,
                        onClick = { vm.loadPreset(onPresetReady) }
                    )
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(
                        text = "Создать через AI",
                        onClick = onChooseAi
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetCard() {
    FormaCard(elevated = true, padding = PaddingValues(20.dp)) {
        Column {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .background(SurfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Bookmark, null, tint = AccentLime, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Моя программа",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Готовый Push/Pull сплит на 3 дня — Пн/Ср/Пт. С детальными подходами, RIR и стартовыми весами.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun AiCard() {
    FormaCard(padding = PaddingValues(20.dp)) {
        Column {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .background(SurfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = TextTertiary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Создать через AI",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Расскажи о цели, оборудовании и опыте — AI составит персональную программу.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
