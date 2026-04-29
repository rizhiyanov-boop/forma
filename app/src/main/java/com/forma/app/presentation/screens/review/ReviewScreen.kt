package com.forma.app.presentation.screens.review

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingFlat
import androidx.compose.material.icons.rounded.TrendingUp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.forma.app.domain.review.Recommendation
import com.forma.app.domain.review.RecommendationType
import com.forma.app.domain.review.Verdict
import com.forma.app.domain.review.WorkoutReview
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
import com.forma.app.presentation.theme.WarnAmber

@Composable
fun ReviewScreen(
    vm: ReviewViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val review by vm.review.collectAsState()
    val ui by vm.ui.collectAsState()

    Box(
        Modifier
            .fillMaxSize()
            .background(BgBlack)
            .systemBarsPadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            TopBar(onBack)

            val r = review
            if (r == null) {
                LoadingState()
                return@Column
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { VerdictCard(r) }

                if (r.isDismissed) {
                    item { DismissedNote() }
                } else {
                    item {
                        Text(
                            "Рекомендации",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                        )
                    }
                    items(r.recommendations) { rec ->
                        RecommendationCard(
                            rec = rec,
                            isApplied = rec.id in r.appliedRecommendationIds,
                            isApplying = rec.id in ui.isApplying,
                            onApply = { vm.applyRecommendation(rec.id) }
                        )
                    }
                }

                if (ui.errorMessage != null) {
                    item {
                        Text(
                            ui.errorMessage!!,
                            color = ErrorRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }

            // Нижняя зона с CTA
            Column(Modifier.padding(20.dp)) {
                PrimaryButton(
                    text = "На главную",
                    leadingIcon = Icons.Rounded.Check,
                    onClick = onDone
                )
                if (!r.isDismissed && r.recommendations.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    SecondaryButton(
                        text = "Скрыть рекомендации",
                        onClick = {
                            vm.dismiss()
                            onDone()
                        }
                    )
                }
            }
        }
    }
}

// ─── Топ-бар ─────────────────────────────────────────────────────

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
                "РАЗБОР",
                color = AccentLime,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                "Тренировка завершена",
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AccentLime, strokeWidth = 2.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Анализ тренировки…",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// ─── Вердикт ────────────────────────────────────────────────────

@Composable
private fun VerdictCard(review: WorkoutReview) {
    val (color, icon) = verdictStyle(review.verdict)
    FormaCard(elevated = true, padding = PaddingValues(20.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceHighlight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    review.verdict.displayName,
                    color = color,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                review.summary,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun verdictStyle(v: Verdict): Pair<Color, ImageVector> = when (v) {
    Verdict.PROGRESS -> AccentLime to Icons.Rounded.TrendingUp
    Verdict.REGRESS -> ErrorRed to Icons.Rounded.TrendingDown
    Verdict.STAGNATION -> WarnAmber to Icons.Rounded.TrendingFlat
    Verdict.EARLY -> TextSecondary to Icons.Rounded.TrendingFlat
    Verdict.SOLID -> AccentLime to Icons.Rounded.Check
}

// ─── Карточка рекомендации ───────────────────────────────────────

@Composable
private fun RecommendationCard(
    rec: Recommendation,
    isApplied: Boolean,
    isApplying: Boolean,
    onApply: () -> Unit
) {
    FormaCard(padding = PaddingValues(16.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RecommendationBadge(rec.type)
                Spacer(Modifier.width(10.dp))
                Text(
                    rec.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                rec.rationale,
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )

            // Кнопки действий — только для рекомендаций которые можно применить
            val isActionable = rec.type in setOf(
                RecommendationType.INCREASE_WEIGHT,
                RecommendationType.DECREASE_WEIGHT,
                RecommendationType.CHANGE_REPS,
                RecommendationType.REST_LONGER,
                RecommendationType.REST_SHORTER
            )

            if (isActionable) {
                Spacer(Modifier.height(12.dp))
                if (isApplied) {
                    AppliedPill()
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ApplyButton(
                            isLoading = isApplying,
                            onClick = onApply,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationBadge(type: RecommendationType) {
    val label = when (type) {
        RecommendationType.INCREASE_WEIGHT -> "+ВЕС"
        RecommendationType.DECREASE_WEIGHT -> "−ВЕС"
        RecommendationType.CHANGE_REPS -> "ПОВТОРЫ"
        RecommendationType.DELOAD -> "РАЗГРУЗКА"
        RecommendationType.KEEP -> "ДЕРЖИ"
        RecommendationType.TECHNIQUE -> "ТЕХНИКА"
        RecommendationType.REST_LONGER -> "+ОТДЫХ"
        RecommendationType.REST_SHORTER -> "−ОТДЫХ"
        RecommendationType.INFO -> "ИНФО"
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceHighlight)
            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            label,
            color = AccentLime,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun ApplyButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        if (isLoading) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = AccentLime,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            PrimaryButton(
                text = "Применить",
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AppliedPill() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceHighlight)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            Icons.Rounded.Check,
            null,
            tint = AccentLime,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "Применено",
            color = AccentLime,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun DismissedNote() {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            "Разбор закрыт. Рекомендации не применены.",
            color = TextTertiary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
