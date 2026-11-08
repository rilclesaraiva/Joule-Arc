package com.joulearc.app.core

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Simple rounded bar chart. [values] are scaled against [maxValue]; a null value draws a faint stub.
 * Bars whose index is in [highlighted] use the accent color. [yLabels] go top to bottom on the right,
 * and [xLabels] has one entry per bar (use "" to skip a label).
 */
@Composable
fun BarChart(
    values: List<Float?>,
    maxValue: Float,
    yLabels: List<String>,
    xLabels: List<String>,
    highlighted: Set<Int>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 140.dp,
) {
    val barColor = MaterialTheme.colorScheme.outline
    val accent = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(chartHeight)) {
            Canvas(Modifier.weight(1f).fillMaxHeight()) {
                val count = values.size
                if (count == 0) return@Canvas
                val slot = size.width / count
                val barWidth = slot * 0.6f
                val stub = 3.dp.toPx()
                listOf(0f, size.height / 2, size.height).forEach { y ->
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
                values.forEachIndexed { index, value ->
                    val height = if (value == null) stub
                    else ((value / maxValue).coerceIn(0f, 1f) * size.height).coerceAtLeast(stub)
                    val color = when {
                        value == null -> barColor.copy(alpha = 0.25f)
                        index in highlighted -> accent
                        else -> barColor
                    }
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(index * slot + (slot - barWidth) / 2, size.height - height),
                        size = Size(barWidth, height),
                        cornerRadius = CornerRadius(barWidth / 2, barWidth / 2),
                    )
                }
            }
            Column(
                modifier = Modifier.width(44.dp).fillMaxHeight().padding(start = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                yLabels.forEach { Text(it, style = MaterialTheme.typography.labelSmall) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp, end = 44.dp)) {
            xLabels.forEach { label ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.wrapContentWidth(unbounded = true),
                    )
                }
            }
        }
    }
}
