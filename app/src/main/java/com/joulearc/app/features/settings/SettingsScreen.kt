package com.joulearc.app.features.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joulearc.app.R
import com.joulearc.app.core.PillToggle
import com.joulearc.app.core.REPOSITORY_URL
import com.joulearc.app.core.SUPPORT_EMAIL
import com.joulearc.app.core.SupportFooter
import com.joulearc.app.data.AppLanguage
import com.joulearc.app.data.DeviceInfo
import com.joulearc.app.data.SettingsRepository
import kotlin.math.roundToInt

private val LANGUAGE_TAGS = listOf("", "en", "pt-BR")

@Composable
fun SettingsScreen(systemDesignMah: Int?, modifier: Modifier = Modifier) {
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

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Section(stringResource(R.string.settings_reminders))
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column {
                SwitchRow(
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
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                HorizontalDivider()
                SwitchRow(
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
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                HorizontalDivider()
                ClickRow(
                    title = stringResource(R.string.settings_protection),
                    subtitle = stringResource(
                        if (protectionOk) R.string.settings_protection_on else R.string.settings_protection_sub,
                    ),
                    onClick = { open(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
                )
            }
        }

        Section(stringResource(R.string.home_battery))
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column {
                Column(Modifier.padding(20.dp)) {
                    var text by rememberSaveable { mutableStateOf(designUser?.toString() ?: "") }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }.take(5)
                            text = digits
                            SettingsRepository.setDesignCapacityMah(digits.toIntOrNull())
                        },
                        label = { Text(stringResource(R.string.settings_design)) },
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
                HorizontalDivider()
                SwitchRow(
                    title = stringResource(R.string.settings_dual_cell),
                    subtitle = stringResource(R.string.settings_dual_cell_sub),
                    checked = dualCell,
                    onChange = SettingsRepository::setDualCellBattery,
                )
                HorizontalDivider()
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.settings_temp_unit), style = MaterialTheme.typography.titleMedium)
                    PillToggle(
                        options = listOf("°C", "°F"),
                        selected = if (fahrenheit) 1 else 0,
                        onSelect = { SettingsRepository.setUseFahrenheit(it == 1) },
                    )
                }
                HorizontalDivider()
                ClickRow(
                    title = stringResource(R.string.settings_reset_recorded),
                    subtitle = stringResource(R.string.settings_reset_recorded_sub),
                    onClick = {
                        SettingsRepository.resetRecorded()
                        Toast.makeText(context, R.string.settings_reset_done, Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }

        Section(stringResource(R.string.settings_appearance))
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium)
                PillToggle(
                    options = listOf(
                        stringResource(R.string.theme_system),
                        stringResource(R.string.theme_light),
                        stringResource(R.string.theme_dark),
                    ),
                    selected = themeMode,
                    onSelect = SettingsRepository::setThemeMode,
                )
                if (AppLanguage.isSupported) {
                    Text(
                        text = stringResource(R.string.settings_language),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    val current = AppLanguage.current(context)
                    val selected = when {
                        current.startsWith("pt") -> 2
                        current.startsWith("en") -> 1
                        else -> 0
                    }
                    PillToggle(
                        options = listOf(
                            stringResource(R.string.language_system),
                            "English",
                            "Português",
                        ),
                        selected = selected,
                        onSelect = { AppLanguage.set(context, LANGUAGE_TAGS[it]) },
                    )
                }
            }
        }

        Section(stringResource(R.string.settings_help))
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column {
                ClickRow(
                    title = stringResource(R.string.settings_report),
                    subtitle = stringResource(R.string.settings_report_sub),
                    onClick = { sendReport(context) },
                )
                HorizontalDivider()
                ClickRow(
                    title = stringResource(R.string.settings_source),
                    subtitle = REPOSITORY_URL.removePrefix("https://"),
                    onClick = { open(context, Intent(Intent.ACTION_VIEW, Uri.parse(REPOSITORY_URL))) },
                )
            }
        }

        SupportFooter()
        Text(
            text = stringResource(R.string.settings_version, DeviceInfo.appVersion(context)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Section(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ClickRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun open(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.no_app_found, Toast.LENGTH_SHORT).show()
    }
}

/** Opens the user's email app with a draft. Nothing is sent unless the user sends it. */
private fun sendReport(context: Context) {
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
