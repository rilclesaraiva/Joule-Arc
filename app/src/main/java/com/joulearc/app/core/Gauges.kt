package com.joulearc.app.core

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Picks the smallest gauge range from a fixed list that fits the value. */
fun gaugeRange(magnitude: Int): Float =
    listOf(1000f, 2000f, 5000f, 10000f, 20000f).firstOrNull { magnitude <= it } ?: 20000f

/**
 * A gauge range that does not flip back and forth. It grows at once when the value no longer fits,
 * but shrinks only after the value has fit the smaller range, with some margin, for [SHRINK_DELAY_MS].
 */
@Composable
fun rememberStableGaugeRange(magnitude: Int): Float {
    var range by remember { mutableFloatStateOf(gaugeRange(magnitude)) }
    val growTo = gaugeRange(magnitude)
    val shrinkTo = gaugeRange((magnitude / 0.7f).toInt())
    LaunchedEffect(growTo, shrinkTo) {
        when {
            growTo > range -> range = growTo
            shrinkTo < range -> {
                delay(SHRINK_DELAY_MS)
                range = shrinkTo
            }
        }
    }
    return range
}

private const val SHRINK_DELAY_MS = 20_000L

private fun tickText(v: Float): String {
    if (abs(v) < 1000f) return v.roundToInt().toString()
    val k = v / 1000f
    return if (k == k.roundToInt().toFloat()) "${k.roundToInt()}k" else String.format(Locale.US, "%.1fk", k)
}

/**
 * Half-circle gauge centered on zero: the left half is negative (out of the battery), the right
 * half positive (into the battery). The needle and the scale animate with the app's own timing.
 */
@Composable
fun BidirectionalGauge(
    value: Float,
    range: Float,
    negativeColor: Color,
    positiveColor: Color,
    modifier: Modifier = Modifier,
) {
    val spec = tween<Float>(AppMotion.LONG)
    val animatedRange by animateFloatAsState(range, spec, label = "gaugeRange")
    val animatedValue by animateFloatAsState(value, spec, label = "gaugeValue")
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val needleColor = MaterialTheme.colorScheme.onSurface
    val hubColor = MaterialTheme.colorScheme.surface
    val tickColor = MaterialTheme.colorScheme.outline

    Canvas(modifier.fillMaxWidth().aspectRatio(1.8f)) {
        val stroke = size.width * 0.085f
        val center = Offset(size.width / 2f, size.height - 14.dp.toPx())
        val radius = min(size.width / 2f, center.y) - stroke / 2f
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2f, radius * 2f)
        val style = Stroke(width = stroke)

        drawArc(negativeColor.copy(alpha = 0.22f), 180f, 90f, false, topLeft, arcSize, style = style)
        drawArc(positiveColor.copy(alpha = 0.22f), 270f, 90f, false, topLeft, arcSize, style = style)
        val frac = if (animatedRange > 0f) (animatedValue / animatedRange).coerceIn(-1f, 1f) else 0f
        if (frac < 0f) drawArc(negativeColor, 270f, frac * 90f, false, topLeft, arcSize, style = style)
        if (frac > 0f) drawArc(positiveColor, 270f, frac * 90f, false, topLeft, arcSize, style = style)

        val inner = radius - stroke / 2f - 6.dp.toPx()
        val tickLength = 8.dp.toPx()
        listOf(-1f, -0.5f, 0f, 0.5f, 1f).forEach { f ->
            val angle = Math.toRadians((270f + f * 90f).toDouble())
            val c = cos(angle).toFloat()
            val s = sin(angle).toFloat()
            drawLine(
                color = tickColor,
                start = Offset(center.x + inner * c, center.y + inner * s),
                end = Offset(center.x + (inner - tickLength) * c, center.y + (inner - tickLength) * s),
                strokeWidth = 2.dp.toPx(),
            )
            val layout = measurer.measure(tickText(f * animatedRange), labelStyle)
            val r = inner - tickLength - 16.dp.toPx()
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    center.x + r * c - layout.size.width / 2f,
                    center.y + r * s - layout.size.height / 2f,
                ),
            )
        }

        val angle = Math.toRadians((270f + frac * 90f).toDouble())
        val length = radius - stroke * 0.3f
        drawLine(
            color = needleColor,
            start = center,
            end = Offset(center.x + length * cos(angle).toFloat(), center.y + length * sin(angle).toFloat()),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(needleColor, 9.dp.toPx(), center)
        drawCircle(hubColor, 4.dp.toPx(), center)
    }
}

/** Small half-circle gauge with colored segments ([segments] = end fraction to color, ascending). */
@Composable
fun MiniGauge(
    fraction: Float,
    segments: List<Pair<Float, Color>>,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(AppMotion.LONG), label = "miniGauge")
    val needleColor = MaterialTheme.colorScheme.onSurface
    Canvas(modifier.size(width = 84.dp, height = 48.dp)) {
        val stroke = 10.dp.toPx()
        val center = Offset(size.width / 2f, size.height - 4.dp.toPx())
        val radius = min(size.width / 2f, center.y) - stroke / 2f
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2f, radius * 2f)
        var start = 0f
        segments.forEach { (end, color) ->
            val sweep = (end - start) * 180f
            if (sweep > 0f) {
                drawArc(
                    color, 180f + start * 180f + 1f, (sweep - 2f).coerceAtLeast(0.5f), false,
                    topLeft, arcSize, style = Stroke(width = stroke),
                )
            }
            start = end
        }
        val angle = Math.toRadians((180f + animated * 180f).toDouble())
        val length = radius - stroke / 2f
        drawLine(
            color = needleColor,
            start = center,
            end = Offset(center.x + length * cos(angle).toFloat(), center.y + length * sin(angle).toFloat()),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(needleColor, 3.5.dp.toPx(), center)
    }
}

/** Horizontal bar of colored ranges with a marker for the current value and labels at the boundaries. */
@Composable
fun RangeBar(
    fraction: Float?,
    segments: List<Pair<Float, Color>>,
    labels: List<Pair<Float, String>>,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState((fraction ?: 0f).coerceIn(0f, 1f), tween(AppMotion.LONG), label = "rangeBar")
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val ringOuter = MaterialTheme.colorScheme.onSurface
    val ringInner = MaterialTheme.colorScheme.surface
    Canvas(modifier.fillMaxWidth().height(44.dp)) {
        val barHeight = 10.dp.toPx()
        val y = 12.dp.toPx()
        val gap = 3.dp.toPx()
        val pad = 10.dp.toPx()
        val width = size.width - pad * 2f
        var start = 0f
        segments.forEach { (end, color) ->
            val x0 = pad + start * width + if (start > 0f) gap / 2f else 0f
            val x1 = pad + end * width - if (end < 1f) gap / 2f else 0f
            if (x1 > x0) {
                drawRoundRect(
                    color = color.copy(alpha = 0.75f),
                    topLeft = Offset(x0, y - barHeight / 2f),
                    size = Size(x1 - x0, barHeight),
                    cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f),
                )
            }
            start = end
        }
        labels.forEach { (f, text) ->
            val layout = measurer.measure(text, labelStyle)
            val x = (pad + f * width - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width)
            drawText(textLayoutResult = layout, topLeft = Offset(x, y + barHeight / 2f + 5.dp.toPx()))
        }
        if (fraction != null) {
            val cx = pad + animated * width
            val fill = segments.firstOrNull { animated <= it.first }?.second ?: segments.lastOrNull()?.second ?: ringOuter
            drawCircle(ringOuter, 10.dp.toPx(), Offset(cx, y))
            drawCircle(ringInner, 8.dp.toPx(), Offset(cx, y))
            drawCircle(fill, 5.5.dp.toPx(), Offset(cx, y))
        }
    }
}
