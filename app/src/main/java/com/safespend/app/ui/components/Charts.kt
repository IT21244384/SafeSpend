package com.safespend.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** One wedge of the donut, or one bar of the bar chart. */
data class ChartSlice(val label: String, val value: Long, val color: Color)

/**
 * Category breakdown as a ring.
 *
 * A ring rather than a pie because the hole is useful: the month's total sits in
 * it, so the chart answers "how much" and "on what" in one glance instead of
 * needing a separate figure above it.
 */
@Composable
fun DonutChart(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier,
    diameter: Int = 180,
    thickness: Int = 26,
    centerLabel: String = "",
    centerValue: String = "",
) {
    val total = slices.sumOf { it.value }.coerceAtLeast(1L)
    val sweepProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600),
        label = "donutSweep",
    )
    val emptyTrack = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier.size(diameter.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = thickness.dp.toPx())
            val inset = thickness.dp.toPx() / 2
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            if (slices.isEmpty()) {
                drawArc(
                    color = emptyTrack,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
                return@Canvas
            }

            // Start at 12 o'clock and run clockwise — the direction people read a clock.
            var startAngle = -90f
            slices.forEach { slice ->
                val sweep = 360f * (slice.value.toFloat() / total) * sweepProgress
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    // A 1.5 degree gap keeps adjacent wedges distinguishable without a
                    // border, which would muddy the colours at this thickness.
                    sweepAngle = (sweep - 1.5f).coerceAtLeast(0f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
                startAngle += sweep
            }
        }

        if (centerValue.isNotEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = centerLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = centerValue,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Daily spend as columns.
 *
 * Bars, not a line: daily spending is a set of discrete events, and a line between
 * them implies a continuous quantity that was never measured. Days with no spending
 * draw as a flat tick on the baseline rather than vanishing, so gaps stay visible.
 */
@Composable
fun BarChart(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier,
    height: Int = 140,
    barColor: Color = MaterialTheme.colorScheme.primary,
    labelEvery: Int = 5,
) {
    val max = slices.maxOfOrNull { it.value }?.coerceAtLeast(1L) ?: 1L
    val grow by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600),
        label = "barGrow",
    )
    val baseline = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height.dp),
        ) {
            if (slices.isEmpty()) return@Canvas

            val gap = 3.dp.toPx()
            val slot = size.width / slices.size
            val barWidth = (slot - gap).coerceAtLeast(1.5.dp.toPx())
            val radius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2, barWidth / 2)
            val minHeight = 2.dp.toPx()

            slices.forEachIndexed { index, slice ->
                val fraction = slice.value.toFloat() / max
                val barHeight = (size.height * fraction * grow).coerceAtLeast(
                    if (slice.value == 0L) minHeight else minHeight * 2,
                )
                drawRoundRect(
                    color = if (slice.value == 0L) baseline else barColor,
                    topLeft = Offset(index * slot + gap / 2, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = radius,
                )
            }
        }

        // Sparse axis labels: every Nth day, so a 31-day month doesn't turn the axis
        // into a grey smear.
        androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth()) {
            slices.forEachIndexed { index, slice ->
                Text(
                    text = if (index == 0 || (index + 1) % labelEvery == 0) slice.label else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
