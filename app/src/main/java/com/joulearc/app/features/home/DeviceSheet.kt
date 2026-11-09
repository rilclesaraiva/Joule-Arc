package com.joulearc.app.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joulearc.app.R
import com.joulearc.app.data.DeviceInfo
import java.util.Locale

/** One sheet with everything about this phone, opened by tapping the Device section. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeviceSheet(state: HomeState, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(R.drawable.ic_chip, MaterialTheme.colorScheme.primary)
                Text(
                    text = stringResource(R.string.device_this_phone),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
            Text(DeviceInfo.model, style = MaterialTheme.typography.displaySmall)
            Text(
                text = DeviceInfo.manufacturer.replaceFirstChar { it.titlecase(Locale.getDefault()) },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val capacityLabel = if (state.designFromSystem) R.string.device_capacity_detected else R.string.device_capacity_user
            InfoRows(
                listOfNotNull(
                    stringResource(R.string.device_android) to DeviceInfo.androidVersion,
                    stringResource(R.string.device_api) to DeviceInfo.apiLevel.toString(),
                    DeviceInfo.securityPatch.takeIf { it.isNotBlank() }?.let { stringResource(R.string.device_patch) to it },
                    DeviceInfo.deviceCode.takeIf { it.isNotBlank() }?.let { stringResource(R.string.device_code) to it },
                    state.designCapacityMah?.let { stringResource(capacityLabel) to stringResource(R.string.value_mah, it) },
                ),
            )
            MeaningBox(stringResource(R.string.sheet_meaning), stringResource(R.string.device_note), null)
        }
    }
}
