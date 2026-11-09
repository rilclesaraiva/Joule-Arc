package com.joulearc.app.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.joulearc.app.core.statusPalette
import com.joulearc.app.data.Band

@Composable
internal fun ToneChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(shape = CircleShape, color = color.copy(alpha = 0.16f), modifier = modifier) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
internal fun RatingChip(band: Band, modifier: Modifier = Modifier) {
    ToneChip(stringResource(ratingLabel(band.label)), statusPalette().of(band.tone), modifier)
}

@Composable
internal fun IconBadge(icon: Int, tint: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
internal fun SectionHeader(title: String, hint: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
    }
}

/** Label/value rows inside an outlined card. */
@Composable
internal fun InfoRows(rows: List<Pair<String, String>>, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    if (rows.isEmpty()) return
    val content: @Composable () -> Unit = {
        Column {
            rows.forEachIndexed { i, (label, value) ->
                if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(text = value, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End)
                }
            }
        }
    }
    if (onClick != null) {
        OutlinedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) { content() }
    } else {
        OutlinedCard(modifier.fillMaxWidth()) { content() }
    }
}

/** "What this means" box, tinted with the reading's color. */
@Composable
internal fun MeaningBox(title: String, text: String, color: Color?) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = color?.copy(alpha = 0.14f) ?: MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
