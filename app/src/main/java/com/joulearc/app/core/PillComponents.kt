package com.joulearc.app.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Full-width pill with equal segments, like a two-option switch. */
@Composable
fun PillToggle(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(4.dp)) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selected
                Surface(
                    onClick = { onSelect(index) },
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(Modifier.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                        Text(text = option, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
