package com.forma.app.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.forma.app.domain.wellness.EnergyLevel
import com.forma.app.presentation.icons.iconRes
import com.forma.app.presentation.theme.AccentDark
import com.forma.app.presentation.theme.AccentGradient
import com.forma.app.presentation.theme.AccentLime
import com.forma.app.presentation.theme.BgBlack
import com.forma.app.presentation.theme.BorderFocused
import com.forma.app.presentation.theme.BorderSubtle
import com.forma.app.presentation.theme.SurfaceDark
import com.forma.app.presentation.theme.SurfaceElevated
import com.forma.app.presentation.theme.SurfaceHighlight
import com.forma.app.presentation.theme.TextPrimary
import com.forma.app.presentation.theme.TextSecondary

enum class HapticType {
    Soft,
    Strong,
    None
}

@Composable
fun FormaCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(20.dp),
    elevated: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (elevated) SurfaceElevated else SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .padding(padding)
    ) { content() }
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    hapticOnClick: HapticType = HapticType.Strong,
    onClick: () -> Unit
) {
    val alpha by animateFloatAsState(if (enabled) 1f else 0.4f, label = "btn_alpha")
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(AccentGradient)
            .clickable(enabled = enabled) {
                when (hapticOnClick) {
                    HapticType.Soft -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    HapticType.Strong -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    HapticType.None -> Unit
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = AccentDark, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = AccentDark,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    hapticOnClick: HapticType = HapticType.None,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .border(1.dp, BorderFocused, RoundedCornerShape(26.dp))
            .clickable {
                when (hapticOnClick) {
                    HapticType.Soft -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    HapticType.Strong -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    HapticType.None -> Unit
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun TertiaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: androidx.compose.ui.graphics.Color = TextSecondary,
    hapticOnClick: HapticType = HapticType.Soft,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) {
                when (hapticOnClick) {
                    HapticType.Soft -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    HapticType.Strong -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    HapticType.None -> Unit
                }
                onClick()
            }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = if (enabled) textColor else textColor.copy(alpha = 0.5f),
            fontWeight = FontWeight.SemiBold,
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun SelectableTile(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (selected) SurfaceHighlight else SurfaceDark, label = "tile_bg"
    )
    val borderColor by animateColorAsState(
        if (selected) com.forma.app.presentation.theme.AccentLime else BorderSubtle,
        label = "tile_border"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = TextSecondary,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun StepDots(total: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            val isActive = i == current
            val isPast = i < current
            val color = when {
                isActive -> com.forma.app.presentation.theme.AccentLime
                isPast -> com.forma.app.presentation.theme.AccentDim
                else -> BorderFocused
            }
            Box(
                Modifier
                    .width(if (isActive) 28.dp else 8.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
            if (i != total - 1) Spacer(Modifier.width(6.dp))
        }
    }
}

@Composable
fun GradientText(text: String, style: androidx.compose.ui.text.TextStyle) {
    Text(
        text = text,
        style = style.copy(brush = AccentGradient)
    )
}

@Composable
fun DividerLine(modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = BorderSubtle) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
fun EnergyPickerOverlay(
    title: String,
    subtitle: String? = null,
    onPick: (EnergyLevel) -> Unit,
    onSkip: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        FormaCard(
            modifier = Modifier.padding(horizontal = 20.dp),
            padding = PaddingValues(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    title,
                    color = AccentLime,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EnergyLevel.entries.forEach { level ->
                        EnergyPickerOption(level = level, onClick = { onPick(level) })
                    }
                }
                if (onSkip != null) {
                    Text(
                        text = "Пропустить пока",
                        color = TextSecondary,
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSkip()
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EnergyPickerOption(level: EnergyLevel, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceHighlight)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(level.iconRes()),
                contentDescription = null,
                tint = Color.Unspecified
            )
            Text(
                level.displayName,
                color = TextSecondary,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall
            )
        }
    }
}
