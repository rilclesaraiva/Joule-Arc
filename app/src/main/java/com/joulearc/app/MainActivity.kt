package com.joulearc.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joulearc.app.core.JouleArcTheme
import com.joulearc.app.core.SupportFooter
import com.joulearc.app.data.BatteryRepository
import com.joulearc.app.features.monitor.MonitorScreen
import com.joulearc.app.features.monitor.MonitorService
import com.joulearc.app.features.usage.UsageAccessCard
import com.joulearc.app.features.usage.isUsageAccessGranted
import com.joulearc.app.features.usage.usageAccessSettingsIntent

/** Entry point: wires the features together. Features never import each other. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MonitorService.start(this)

        setContent {
            JouleArcTheme {
                val context = LocalContext.current
                val snapshot by BatteryRepository.snapshot.collectAsStateWithLifecycle()

                var notificationsGranted by remember { mutableStateOf(hasNotificationPermission()) }
                var usageGranted by remember { mutableStateOf(isUsageAccessGranted(context)) }

                val notificationLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { notificationsGranted = it }

                LifecycleResumeEffect(Unit) {
                    notificationsGranted = hasNotificationPermission()
                    usageGranted = isUsageAccessGranted(context)
                    onPauseOrDispose { }
                }

                Scaffold { padding ->
                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.padding(top = 16.dp),
                        )

                        if (!notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ElevatedCard(Modifier.fillMaxWidth()) {
                                Column(
                                    Modifier.padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.notifications_title),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        text = stringResource(R.string.notifications_body),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Button(onClick = {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }) { Text(stringResource(R.string.notifications_action)) }
                                }
                            }
                        }

                        MonitorScreen(snapshot)

                        UsageAccessCard(
                            granted = usageGranted,
                            onOpenSettings = { context.startActivity(usageAccessSettingsIntent()) },
                        )

                        SupportFooter()
                    }
                }
            }
        }
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}
