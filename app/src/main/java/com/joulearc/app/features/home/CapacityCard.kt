package com.joulearc.app.features.home

import androidx.compose.foundation.Canvas
import com.joulearc.app.core.formatInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joulearc.app.R
import com.joulearc.app.core.statusPalette
import com.joulearc.app.data.BatterySnapshot
import com.joulearc.app.data.Scales
import com.joulearc.app.data.estimateHealthPercent
import java.util.Locale
import kotlin.math.roundToInt

/** Battery capacity: charge now, full charge today, lost to wear and device capacity. */
@Composable
internal fun CapacityCard(
    s: BatterySnapshot,
    capacityMah: Int?,
    designMah: Int?,
    designFromSystem: Boolean,
    onClick: () -> Unit,
) {
    val palette = statusPalette()
    val nowMah = s.chargeCounterUah?.let { it / 1000f }
    val health = estimateHealthPercent(capacityMah, designMah)
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(R.drawable.ic_battery, MaterialTheme.colorScheme.primary)
                Text(
                    text = stringResource(R.string.capacity_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (health != null) {
                    val band = Scales.health.rate(health.toFloat())
                    ToneChip(stringResource(R.string.health_chip, health), palette.of(band.tone))
                }
            }

            if (nowMah == null) {
                Text(stringResource(R.string.capacity_unavailable), style = MaterialTheme.typography.bodyMedium)
                return@Column
            }

            val total = (designMah ?: capacityMah ?: 0).toFloat()
            if (total > 0f) {
                val nowColor = MaterialTheme.colorScheme.primary
                val fullColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                val lostColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                Canvas(Modifier.fillMaxWidth().height(14.dp)) {
                    val h = size.height
                    val r = CornerRadius(h / 2f, h / 2f)
                    drawRoundRect(lostColor, Offset.Zero, size, r)
                    capacityMah?.let {
                        drawRoundRect(fullColor, Offset.Zero, Size(size.width * (it / total).coerceIn(0f, 1f), h), r)
                    }
                    drawRoundRect(nowColor, Offset.Zero, Size(size.width * (nowMah / total).coerceIn(0f, 1f), h), r)
                }
            }

            val rows = buildList {
                add(
                    stringResource(R.string.capacity_now) to
                        stringResource(R.string.capacity_now_value, formatMah(nowMah), s.levelPercent),
                )
                capacityMah?.let { add(stringResource(R.string.capacity_full) to stringResource(R.string.value_mah, formatInt(it))) }
                if (capacityMah != null && designMah != null) {
                    add(
                        stringResource(R.string.capacity_lost) to
                            stringResource(R.string.value_mah, formatInt((designMah - capacityMah).coerceAtLeast(0))),
                    )
                }
                add(
                    stringResource(R.string.capacity_design) to
                        (designMah?.let { stringResource(R.string.value_mah, formatInt(it)) } ?: "—"),
                )
            }
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(value, style = MaterialTheme.typography.titleMedium)
                }
            }
            Text(
                text = when {
                    designMah == null -> stringResource(R.string.capacity_design_missing)
                    designFromSystem -> stringResource(R.string.capacity_design_system)
                    else -> stringResource(R.string.capacity_design_user)
                } + " " + stringResource(R.string.capacity_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun formatMah(mah: Float): String = formatInt(mah.roundToInt())

