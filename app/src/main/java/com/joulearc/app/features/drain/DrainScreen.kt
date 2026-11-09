package com.joulearc.app.features.drain

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joulearc.app.R
import com.joulearc.app.core.formatDuration
import com.joulearc.app.data.DayPeriod
import com.joulearc.app.data.DrainStats
import com.joulearc.app.core.PageTitle
import kotlin.math.roundToInt

@Composable
fun DrainScreen(
    stats: DrainStats,
    levelPercent: Int?,
    capacityMah: Int?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageTitle(stringResource(R.string.drain_title))

        if (stats.screenOnPph == null && stats.screenOffPph == null) {
            Text(
                text = stringResource(R.string.drain_collecting),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        RateCard(
            title = stringResource(R.string.drain_screen_on),
            pph = stats.screenOnPph,
            levelPercent = levelPercent,
            capacityMah = capacityMah,
        )
        RateCard(
            title = stringResource(R.string.drain_screen_off),
            pph = stats.screenOffPph,
            levelPercent = levelPercent,
            capacityMah = capacityMah,
        )

        if (stats.byPeriod.isNotEmpty()) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.drain_by_period),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    stats.highestPeriod?.let {
                        Text(
                            text = stringResource(R.string.drain_highest, stringResource(periodRes(it))),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    stats.lowestPeriod?.let {
                        Text(
                            text = stringResource(R.string.drain_lowest, stringResource(periodRes(it))),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    stats.byPeriod.forEach {
                        val mah = capacityMah?.let { cap -> (it.percentPerHour * cap / 100f).roundToInt() }
                        Text(
                            text = if (mah != null) {
                                stringResource(R.string.drain_period_row_mah, stringResource(periodRes(it.key)), it.percentPerHour, mah)
                            } else {
                                stringResource(R.string.drain_period_row, stringResource(periodRes(it.key)), it.percentPerHour)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        Text(
            text = stringResource(
                R.string.drain_tracked,
                formatDuration((stats.screenOnHours * 3_600_000f).toLong()),
                formatDuration((stats.screenOffHours * 3_600_000f).toLong()),
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = stringResource(R.string.drain_note),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun periodRes(period: DayPeriod): Int = when (period) {
    DayPeriod.NIGHT -> R.string.period_night
    DayPeriod.MORNING -> R.string.period_morning
    DayPeriod.AFTERNOON -> R.string.period_afternoon
    DayPeriod.EVENING -> R.string.period_evening
}

@Composable
private fun RateCard(title: String, pph: Float?, levelPercent: Int?, capacityMah: Int?) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (pph == null) {
                Text(
                    text = stringResource(R.string.drain_not_enough),
                    style = MaterialTheme.typography.bodyMedium,
                )
                return@Column
            }
            Text(
                text = stringResource(R.string.drain_rate, pph),
                style = MaterialTheme.typography.headlineMedium,
            )
            if (capacityMah != null) {
                Text(
                    text = stringResource(R.string.drain_mah, (pph * capacityMah / 100f).roundToInt()),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (levelPercent != null && pph > 0.05f) {
                Text(
                    text = stringResource(
                        R.string.drain_life,
                        formatDuration((levelPercent / pph * 3_600_000f).toLong()),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
