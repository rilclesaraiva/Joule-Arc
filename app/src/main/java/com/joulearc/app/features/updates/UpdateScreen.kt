package com.joulearc.app.features.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.DpOffset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joulearc.app.R
import com.joulearc.app.core.MarkdownText
import com.joulearc.app.data.updates.ReleaseInfo

/** Full-screen notice that a newer version is out, with its release notes. */
@Composable
fun UpdateScreen(
    release: ReleaseInfo,
    installedVersion: String,
    onDownload: () -> Unit,
    onNotNow: () -> Unit,
    onSnoozeWeek: () -> Unit,
    onSnoozeMonth: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(76.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painterResource(R.drawable.ic_new),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.update_title),
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    text = stringResource(R.string.update_versions, release.version, installedVersion),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (release.notes.isBlank()) {
                    Text(stringResource(R.string.update_no_notes), style = MaterialTheme.typography.bodyLarge)
                } else {
                    MarkdownText(release.notes, Modifier.padding(top = 16.dp))
                }
            }
            HorizontalDivider()
            Column(
                modifier = Modifier.navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = onDownload, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text(stringResource(R.string.update_download), style = MaterialTheme.typography.titleMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onNotNow, modifier = Modifier.weight(1f).height(56.dp)) {
                        Text(stringResource(R.string.update_not_now), style = MaterialTheme.typography.titleMedium)
                    }
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        OutlinedIconButton(onClick = { menu = true }, modifier = Modifier.size(56.dp)) {
                            Icon(painterResource(R.drawable.ic_notifications_off), stringResource(R.string.update_snooze_menu))
                        }
                        DropdownMenu(
                            expanded = menu,
                            onDismissRequest = { menu = false },
                            shape = RoundedCornerShape(20.dp),
                            offset = DpOffset(0.dp, 8.dp),
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.update_snooze_week)) },
                                onClick = { menu = false; onSnoozeWeek() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.update_snooze_month)) },
                                onClick = { menu = false; onSnoozeMonth() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.update_skip)) },
                                onClick = { menu = false; onSkip() },
                            )
                        }
                    }
                }
            }
        }
    }
}
