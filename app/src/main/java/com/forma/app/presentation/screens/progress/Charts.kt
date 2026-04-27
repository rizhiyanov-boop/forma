package com.forma.app.presentation.screens.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forma.app.presentation.theme.AccentLime
import com.forma.app.presentation.theme.AccentMint
import com.forma.app.presentation.theme.BorderSubtle
import com.forma.app.presentation.theme.TextSecondary
import com.forma.app.presentation.theme.TextTertiary
import kotlin.math.ceil
import kotlin.math.max

/**
 * Простой LineChart с заполненной зоной под линией.
 * Принимает значения и подписи по X. Сам рассчитывает шкалу Y.
 */
@Composable
fun LineChart(
    values: List<Double>,
    xLabels: List<String>,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 180.dp,
    valueFormatter: (Double) -> String = { it.toInt().toString() }
) {
    val density = LocalDensity.current

    Box(modifier.fillMaxWidth().height(height)) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            if (values.isEmpty()) return@Canvas

            val padTop = 16.dp.toPx()
            val padBottom = 28.dp.toPx()    // место для x labels
            val padLeft = 36.dp.toPx()      // место для y labels
            val padRight = 8.dp.toPx()

            val chartW = size.width - padLeft - padRight
            val chartH = size.height - padTop - padBottom

            val maxVal = values.maxOrNull() ?: 1.0
            val minVal = 0.0  // якорим к нулю
            val range = (maxVal - minVal).coerceAtLeast(1.0)

            // Y gridlines + labels (4 шага)
            val yStepCount = 4
            for (i in 0..yStepCount) {
                val frac = i.toDouble() / yStepCount
                val y = padTop + chartH * (1 - frac).toFloat()
                val gridColor = BorderSubtle.copy(alpha = 0.5f)
                drawLine(
                    color = gridColor,
                    start = Offset(padLeft, y),
                    end = Offset(padLeft + chartW, y),
                    strokeWidth = 1f,
                    pathEffect = if (i in 1 until yStepCount)
                        PathEffect.dashPathEffect(floatArrayOf(4f, 4f)) else null
                )
                val labelValue = minVal + range * frac
                drawValueLabel(
                    text = valueFormatter(labelValue),
                    x = padLeft - 4.dp.toPx(),
                    y = y,
                    density = density,
                    align = android.graphics.Paint.Align.RIGHT,
                    color = TextTertiary
                )
            }

            // X labels
            if (xLabels.isNotEmpty() && values.size > 1) {
                xLabels.forEachIndexed { i, label ->
                    if (i % maxOf(1, xLabels.size / 6) != 0 && i != xLabels.size - 1) return@forEachIndexed
                    val x = padLeft + chartW * (i.toFloat() / (values.size - 1))
                    drawValueLabel(
                        text = label,
                        x = x,
                        y = padTop + chartH + 16.dp.toPx(),
                        density = density,
                        align = android.graphics.Paint.Align.CENTER,
                        color = TextTertiary
                    )
                }
            }

            // Линия + заливка
            val xStep = if (values.size > 1) chartW / (values.size - 1) else 0f
            val pathLine = Path()
            val pathFill = Path()
            values.forEachIndexed { i, v ->
                val x = padLeft + xStep * i
                val y = padTop + chartH * (1 - ((v - minVal) / range).toFloat())
                if (i == 0) {
                    pathLine.moveTo(x, y)
                    pathFill.moveTo(x, padTop + chartH)
                    pathFill.lineTo(x, y)
                } else {
                    pathLine.lineTo(x, y)
                    pathFill.lineTo(x, y)
                }
            }
            // Замыкаем путь заливки
            pathFill.lineTo(padLeft + chartW, padTop + chartH)
            pathFill.close()

            // Заливка градиентом
            drawPath(
                path = pathFill,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AccentLime.copy(alpha = 0.25f),
                        AccentLime.copy(alpha = 0.0f)
                    ),
                    startY = padTop,
                    endY = padTop + chartH
                )
            )
            // Линия — лаймовая
            drawPath(
                path = pathLine,
                color = AccentLime,
                style = Stroke(width = 2.5f.dp.toPx())
            )
            // Точки на узлах
            values.forEachIndexed { i, v ->
                val x = padLeft + xStep * i
                val y = padTop + chartH * (1 - ((v - minVal) / range).toFloat())
                drawCircle(AccentLime, radius = 3.5f.dp.toPx(), center = Offset(x, y))
                drawCircle(
                    color = androidx.compose.ui.graphics.Color(0xFF0A0A0B),
                    radius = 1.5f.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
    }
}

/**
 * Multi-series LineChart — несколько серий разных цветов.
 * Используется для рабочего веса по нескольким упражнениям.
 */
@Composable
fun MultiLineChart(
    series: List<ChartSeries>,
    xLabels: List<String>,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 200.dp
) {
    val density = LocalDensity.current

    Box(modifier.fillMaxWidth().height(height)) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            if (series.isEmpty() || series.all { it.values.isEmpty() }) return@Canvas

            val padTop = 16.dp.toPx()
            val padBottom = 28.dp.toPx()
            val padLeft = 36.dp.toPx()
            val padRight = 8.dp.toPx()

            val chartW = size.width - padLeft - padRight
            val chartH = size.height - padTop - padBottom

            val allValues = series.flatMap { it.values }
            val maxVal = allValues.maxOrNull() ?: 1.0
            val minVal = 0.0
            val range = (maxVal - minVal).coerceAtLeast(1.0)

            // Gridlines
            val yStepCount = 4
            for (i in 0..yStepCount) {
                val frac = i.toDouble() / yStepCount
                val y = padTop + chartH * (1 - frac).toFloat()
                val gridColor = BorderSubtle.copy(alpha = 0.5f)
                drawLine(
                    color = gridColor,
                    start = Offset(padLeft, y),
                    end = Offset(padLeft + chartW, y),
                    strokeWidth = 1f,
                    pathEffect = if (i in 1 until yStepCount)
                        PathEffect.dashPathEffect(floatArrayOf(4f, 4f)) else null
                )
                val labelValue = minVal + range * frac
                drawValueLabel(
                    text = "${labelValue.toInt()}",
                    x = padLeft - 4.dp.toPx(),
                    y = y,
                    density = density,
                    align = android.graphics.Paint.Align.RIGHT,
                    color = TextTertiary
                )
            }

            // X labels (берём из самой длинной серии)
            val maxLen = series.maxOf { it.values.size }
            if (xLabels.isNotEmpty() && maxLen > 1) {
                xLabels.forEachIndexed { i, label ->
                    if (i % maxOf(1, xLabels.size / 5) != 0 && i != xLabels.size - 1) return@forEachIndexed
                    val x = padLeft + chartW * (i.toFloat() / (maxLen - 1))
                    drawValueLabel(
                        text = label,
                        x = x,
                        y = padTop + chartH + 16.dp.toPx(),
                        density = density,
                        align = android.graphics.Paint.Align.CENTER,
                        color = TextTertiary
                    )
                }
            }

            // Серии
            series.forEach { s ->
                if (s.values.isEmpty()) return@forEach
                val xStep = if (s.values.size > 1) chartW / (s.values.size - 1) else 0f
                val path = Path()
                s.values.forEachIndexed { i, v ->
                    val x = padLeft + xStep * i
                    val y = padTop + chartH * (1 - ((v - minVal) / range).toFloat())
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color = s.color, style = Stroke(width = 2.5.dp.toPx()))
                s.values.forEachIndexed { i, v ->
                    val x = padLeft + xStep * i
                    val y = padTop + chartH * (1 - ((v - minVal) / range).toFloat())
                    drawCircle(s.color, radius = 3.dp.toPx(), center = Offset(x, y))
                }
            }
        }
    }
}

data class ChartSeries(
    val name: String,
    val color: Color,
    val values: List<Double>
)

/**
 * Горизонтальный bar chart — для распределения по группам мышц.
 */
@Composable
fun HorizontalBarChart(
    items: List<BarItem>,
    modifier: Modifier = Modifier,
    barHeightDp: androidx.compose.ui.unit.Dp = 28.dp,
    spacingDp: androidx.compose.ui.unit.Dp = 10.dp
) {
    val density = LocalDensity.current
    val totalH = (items.size * barHeightDp.value + (items.size - 1).coerceAtLeast(0) * spacingDp.value + 8).dp

    Box(modifier.fillMaxWidth().height(totalH)) {
        Canvas(Modifier.fillMaxWidth().height(totalH)) {
            if (items.isEmpty()) return@Canvas

            val maxVal = items.maxOf { it.value }.coerceAtLeast(1.0)
            val labelW = 100.dp.toPx()      // фиксированная зона под подписи слева
            val rightPadValueW = 60.dp.toPx() // зона под значение справа
            val barAreaW = size.width - labelW - rightPadValueW
            val barH = barHeightDp.toPx()
            val spacing = spacingDp.toPx()

            items.forEachIndexed { i, item ->
                val y = i * (barH + spacing)

                // Label слева
                drawValueLabel(
                    text = item.name,
                    x = 0f,
                    y = y + barH * 0.6f,
                    density = density,
                    align = android.graphics.Paint.Align.LEFT,
                    color = TextSecondary,
                    sizeSp = 12f
                )

                // Bar background
                drawRoundRect(
                    color = BorderSubtle,
                    topLeft = Offset(labelW, y),
                    size = Size(barAreaW, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barH / 2, barH / 2)
                )
                // Bar value
                val w = barAreaW * (item.value / maxVal).toFloat()
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(AccentLime, AccentMint)),
                    topLeft = Offset(labelW, y),
                    size = Size(w, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barH / 2, barH / 2)
                )

                // Значение справа
                drawValueLabel(
                    text = item.formattedValue,
                    x = size.width,
                    y = y + barH * 0.6f,
                    density = density,
                    align = android.graphics.Paint.Align.RIGHT,
                    color = TextSecondary,
                    sizeSp = 12f
                )
            }
        }
    }
}

data class BarItem(
    val name: String,
    val value: Double,
    val formattedValue: String
)

/**
 * Утилита — рисует текст на Canvas через native Paint.
 */
private fun DrawScope.drawValueLabel(
    text: String,
    x: Float,
    y: Float,
    density: Density,
    align: android.graphics.Paint.Align,
    color: Color,
    sizeSp: Float = 11f
) {
    val paint = android.graphics.Paint().apply {
        this.color = color.toArgb()
        textAlign = align
        textSize = with(density) { sizeSp.sp.toPx() }
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}

private fun Color.toArgb(): Int =
    android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
