package com.joulearc.app.core

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Simple rounded bar chart. [values] are scaled against [maxValue]; a null value draws a faint stub.
 * Bars whose index is in [highlighted] use the accent color. [yLabels] go top to bottom on the right,
 * and [xLabels] has one entry per bar (use "" to skip a label).
 *
 * With [onSelect] the bars can be tapped: tapping a bar selects it ([selected]), tapping the selected
 * bar again clears the selection (null), and holding a bar shows its [hints] label without selecting.
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
    selected: Int? = null,
    onSelect: ((Int?) -> Unit)? = null,
    hints: List<String> = emptyList(),
) {
    val barColor = MaterialTheme.colorScheme.outline
    val accent = MaterialTheme.colorScheme.primary
    val selectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val interactive = onSelect != null
    var held by remember { mutableStateOf<Int?>(null) }
    var canvasWidth by remember { mutableIntStateOf(0) }
    var tipWidth by remember { mutableIntStateOf(0) }
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val shown = (held ?: selected)?.takeIf { it in values.indices }

    Column(modifier) {
        if (interactive) {
            Box(Modifier.fillMaxWidth().height(32.dp).padding(end = 44.dp)) {
                val label = shown?.let { hints.getOrNull(it) }
                if (shown != null && label != null && canvasWidth > 0 && values.isNotEmpty()) {
                    val center = (shown + 0.5f) * canvasWidth / values.size
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (center - tipWidth / 2f).roundToInt().coerceIn(0, (canvasWidth - tipWidth).coerceAtLeast(0)),
                                    0,
                                )
                            }
                            .onSizeChanged { tipWidth = it.width },
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(chartHeight)) {
            Canvas(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onSizeChanged { canvasWidth = it.width }
                    .then(
                        if (interactive) {
                            Modifier.pointerInput(values.size) {
                                val count = values.size
                                fun indexAt(x: Float): Int =
                                    (x / (size.width.toFloat() / count)).toInt().coerceIn(0, count - 1)
                                if (count > 0) {
                                    detectTapGestures(
                                        onPress = { offset ->
                                            val index = indexAt(offset.x)
                                            tryAwaitRelease()
                                            if (held == index) held = null
                                        },
                                        onLongPress = { offset -> held = indexAt(offset.x) },
                                        onTap = { offset ->
                                            val index = indexAt(offset.x)
                                            currentOnSelect?.invoke(if (index == currentSelected) null else index)
                                        },
                                    )
                                }
                            }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                val count = values.size
                if (count == 0) return@Canvas
                val slot = size.width / count
                val barWidth = slot * 0.6f
                val stub = 3.dp.toPx()
                listOf(0f, size.height / 2, size.height).forEach { y ->
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
                shown?.let {
                    val x = (it + 0.5f) * slot
                    drawLine(
                        color = barColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx())),
                    )
                }
                values.forEachIndexed { index, value ->
                    val height = if (value == null) stub
                    else ((value / maxValue).coerceIn(0f, 1f) * size.height).coerceAtLeast(stub)
                    val color = when {
                        value == null -> barColor.copy(alpha = 0.25f)
                        index == selected -> selectedColor
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
