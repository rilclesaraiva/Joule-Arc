package com.joulearc.app.core

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Line chart for normalized points (x and y from 0 to 1). [yLabels] go top to bottom on the right,
 * [xLabels] are spread along the bottom.
 */
@Composable
fun LineChart(
    points: List<Offset>,
    color: Color,
    yLabels: List<String>,
    xLabels: List<String>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 160.dp,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(chartHeight)) {
            Canvas(Modifier.weight(1f).fillMaxHeight()) {
                listOf(0f, size.height / 2f, size.height).forEach { y ->
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
                if (points.size < 2) return@Canvas
                fun map(p: Offset) = Offset(p.x * size.width, (1f - p.y.coerceIn(0f, 1f)) * size.height)
                val line = Path()
                points.forEachIndexed { i, p ->
                    val m = map(p)
                    if (i == 0) line.moveTo(m.x, m.y) else line.lineTo(m.x, m.y)
                }
                val area = Path().apply {
                    addPath(line)
                    lineTo(map(points.last()).x, size.height)
                    lineTo(map(points.first()).x, size.height)
                    close()
                }
                drawPath(area, color.copy(alpha = 0.15f))
                drawPath(
                    line, color,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
            Column(
                modifier = Modifier.width(56.dp).fillMaxHeight().padding(start = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                yLabels.forEach { Text(it, style = MaterialTheme.typography.labelSmall) }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, end = 56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            xLabels.forEach { Text(it, style = MaterialTheme.typography.labelSmall) }
        }
    }
}
