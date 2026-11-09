package com.joulearc.app.features.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.style.TextAlign
import com.joulearc.app.data.updates.GitHubUpdatesRepository
import com.joulearc.app.data.updates.ManualCheckResult
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joulearc.app.R
import com.joulearc.app.core.GroupLabel
import com.joulearc.app.core.PageTitle
import com.joulearc.app.core.PillToggle
import com.joulearc.app.core.RowGroup
import com.joulearc.app.core.RowIcon
import com.joulearc.app.core.SUPPORT_EMAIL
import com.joulearc.app.core.rememberDoubleTapConfirm
import com.joulearc.app.data.AppLanguage
import com.joulearc.app.data.DeviceInfo
import com.joulearc.app.data.SettingsRepository
import kotlin.math.roundToInt

private val LANGUAGE_TAGS = listOf("", "en", "pt-BR")

/** Settings as plain lists, most used first. The only heading is About, at the end. */
@Composable
fun SettingsScreen(systemDesignMah: Int?, onOpenAbout: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val chargeOn by SettingsRepository.chargeAlertEnabled.collectAsStateWithLifecycle()
    val chargeLevel by SettingsRepository.chargeAlertLevel.collectAsStateWithLifecycle()
    val lowOn by SettingsRepository.lowAlertEnabled.collectAsStateWithLifecycle()
    val lowLevel by SettingsRepository.lowAlertLevel.collectAsStateWithLifecycle()
    val dualCell by SettingsRepository.dualCellBattery.collectAsStateWithLifecycle()
    val fahrenheit by SettingsRepository.useFahrenheit.collectAsStateWithLifecycle()
    val themeMode by SettingsRepository.themeMode.collectAsStateWithLifecycle()
    val designUser by SettingsRepository.designCapacityMah.collectAsStateWithLifecycle()

    var protectionOk by remember { mutableStateOf(DeviceInfo.isIgnoringBatteryOptimizations(context)) }
    LifecycleResumeEffect(Unit) {
        protectionOk = DeviceInfo.isIgnoringBatteryOptimizations(context)
        onPauseOrDispose { }
    }
    val checking by GitHubUpdatesRepository.checking.collectAsStateWithLifecycle()
    val manual by GitHubUpdatesRepository.manualResult.collectAsStateWithLifecycle()
    val latest by GitHubUpdatesRepository.latest.collectAsStateWithLifecycle()
    val snoozeUntil by GitHubUpdatesRepository.snoozeUntilMs.collectAsStateWithLifecycle()
    var showAlerts by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { GitHubUpdatesRepository.clearManualResult() }
    val confirmReset = rememberDoubleTapConfirm {
        SettingsRepository.resetRecorded()
        Toast.makeText(context, R.string.settings_reset_done, Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageTitle(stringResource(R.string.settings_title))

        RowGroup(
            buildList<@Composable () -> Unit> {
                add {
                    ControlRow(R.drawable.ic_palette, stringResource(R.string.settings_theme)) {
                        PillToggle(
                            options = listOf(
                                stringResource(R.string.theme_system),
                                stringResource(R.string.theme_light),
                                stringResource(R.string.theme_dark),
                                stringResource(R.string.theme_amoled),
                            ),
                            selected = themeMode,
                            onSelect = SettingsRepository::setThemeMode,
                        )
                    }
                }
                if (AppLanguage.isSupported) {
                    add {
                        ControlRow(R.drawable.ic_language, stringResource(R.string.settings_language)) {
                            val current = AppLanguage.current(context)
                            val selected = when {
                                current.startsWith("pt") -> 2
                                current.startsWith("en") -> 1
                                else -> 0
                            }
                            PillToggle(
                                options = listOf(stringResource(R.string.language_system), "English", "Português"),
                                selected = selected,
                                onSelect = { AppLanguage.set(context, LANGUAGE_TAGS[it]) },
                            )
                        }
                    }
                }
                add {
                    ControlRow(R.drawable.ic_thermometer, stringResource(R.string.settings_temp_unit)) {
                        PillToggle(
                            options = listOf("°C", "°F"),
                            selected = if (fahrenheit) 1 else 0,
                            onSelect = { SettingsRepository.setUseFahrenheit(it == 1) },
                        )
                    }
                }
            },
        )

        RowGroup(
            listOf(
                {
                    ControlRow(R.drawable.ic_battery, stringResource(R.string.settings_design)) {
                        var text by rememberSaveable { mutableStateOf(designUser?.toString() ?: "") }
                        OutlinedTextField(
                            value = text,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }.take(5)
                                text = digits
                                SettingsRepository.setDesignCapacityMah(digits.toIntOrNull())
                            },
                            supportingText = {
                                Text(
                                    stringResource(
                                        R.string.settings_design_hint,
                                        systemDesignMah?.let { stringResource(R.string.value_mah, it) }
                                            ?: stringResource(R.string.settings_design_none),
                                    ),
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                {
                    SwitchRow(
                        icon = R.drawable.ic_chip,
                        title = stringResource(R.string.settings_dual_cell),
                        subtitle = stringResource(R.string.settings_dual_cell_sub),
                        checked = dualCell,
                        onChange = SettingsRepository::setDualCellBattery,
                    )
                },
            ),
        )

        RowGroup(
            listOf(
                {
                    Column {
                        SwitchRow(
                            icon = R.drawable.ic_bolt,
                            title = stringResource(R.string.settings_charge_alert),
                            subtitle = stringResource(R.string.settings_charge_alert_sub, chargeLevel),
                            checked = chargeOn,
                            onChange = SettingsRepository::setChargeAlertEnabled,
                        )
                        Slider(
                            value = chargeLevel.toFloat(),
                            onValueChange = { SettingsRepository.setChargeAlertLevel((it / 5f).roundToInt() * 5) },
                            valueRange = 50f..100f,
                            steps = 9,
                            enabled = chargeOn,
                            modifier = Modifier.padding(start = 84.dp, end = 20.dp),
                        )
                    }
                },
                {
                    Column {
                        SwitchRow(
                            icon = R.drawable.ic_trending_down,
                            title = stringResource(R.string.settings_low_alert),
                            subtitle = stringResource(R.string.settings_low_alert_sub, lowLevel),
                            checked = lowOn,
                            onChange = SettingsRepository::setLowAlertEnabled,
                        )
                        Slider(
                            value = lowLevel.toFloat(),
                            onValueChange = { SettingsRepository.setLowAlertLevel((it / 5f).roundToInt() * 5) },
                            valueRange = 5f..50f,
                            steps = 8,
                            enabled = lowOn,
                            modifier = Modifier.padding(start = 84.dp, end = 20.dp),
                        )
                    }
                },
                {
                    ClickRow(
                        icon = R.drawable.ic_shield,
                        title = stringResource(R.string.settings_protection),
                        subtitle = stringResource(
                            if (protectionOk) R.string.settings_protection_on else R.string.settings_protection_sub,
                        ),
                        onClick = { open(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
                    )
                },
            ),
        )

        RowGroup(
            listOf {
                ClickRow(
                    icon = R.drawable.ic_refresh,
                    title = stringResource(R.string.settings_reset_recorded),
                    subtitle = stringResource(R.string.settings_reset_recorded_sub),
                    onClick = confirmReset,
                    chevron = false,
                )
            },
        )

        RowGroup(
            listOf(
                {
                    ClickRow(
                        icon = R.drawable.ic_download,
                        title = stringResource(R.string.update_check),
                        subtitle = when {
                            checking -> stringResource(R.string.update_checking)
                            manual is ManualCheckResult.UpToDate ->
                                stringResource(R.string.update_up_to_date, (manual as ManualCheckResult.UpToDate).version)
                            manual is ManualCheckResult.Failed -> stringResource(R.string.update_failed)
                            manual is ManualCheckResult.Available ->
                                stringResource(R.string.update_found, latest?.version ?: "")
                            else -> stringResource(R.string.update_check_sub)
                        },
                        onClick = GitHubUpdatesRepository::checkNow,
                        chevron = false,
                    )
                },
                {
                    ClickRow(
                        icon = R.drawable.ic_notifications_off,
                        title = stringResource(R.string.update_alerts),
                        subtitle = if (snoozeUntil > System.currentTimeMillis()) {
                            stringResource(
                                R.string.update_alerts_snoozed,
                                java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT).format(java.util.Date(snoozeUntil)),
                            )
                        } else {
                            stringResource(R.string.update_alerts_on)
                        },
                        onClick = { showAlerts = true },
                        chevron = false,
                    )
                },
            ),
        )

        GroupLabel(stringResource(R.string.about_section))
        if (showAlerts) UpdateAlertsDialog { showAlerts = false }
        RowGroup(
            listOf {
                ClickRow(
                    icon = R.drawable.ic_info,
                    title = stringResource(R.string.about_title),
                    subtitle = stringResource(R.string.about_sub, DeviceInfo.appVersion(context)),
                    onClick = onOpenAbout,
                )
            },
        )
    }
}

/** A row with an icon and title, and a control (toggle, field) underneath. */
@Composable
private fun ControlRow(icon: Int, title: String, control: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RowIcon(icon)
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        control()
    }
}

@Composable
private fun SwitchRow(icon: Int, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RowIcon(icon)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
internal fun ClickRow(
    icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    chevron: Boolean = true,
    iconContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (iconContent != null) iconContent() else RowIcon(icon)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (chevron) {
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun open(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.no_app_found, Toast.LENGTH_SHORT).show()
    }
}

internal fun openUrl(context: Context, url: String) = open(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))

/** Opens the user's email app with a draft. Nothing is sent unless the user sends it. */
internal fun sendReport(context: Context) {
    val body = context.getString(
        R.string.report_body,
        DeviceInfo.appVersion(context),
        DeviceInfo.androidVersion,
        DeviceInfo.manufacturer,
        DeviceInfo.model,
    )
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.report_subject))
        putExtra(Intent.EXTRA_TEXT, body)
    }
    open(context, intent)
}

@Composable
private fun UpdateAlertsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_alerts)) },
        text = {
            Column {
                listOf(
                    R.string.update_alerts_normal to 0,
                    R.string.update_snooze_week to 7,
                    R.string.update_snooze_month to 30,
                ).forEach { (label, days) ->
                    TextButton(onClick = { GitHubUpdatesRepository.snooze(days); onDismiss() }) {
                        Text(stringResource(label), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_dialog_cancel)) } },
    )
}
