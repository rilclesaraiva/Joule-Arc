package com.joulearc.app.features.settings

import androidx.compose.foundation.Image
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.joulearc.app.data.updates.DeveloperAvatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.joulearc.app.R
import com.joulearc.app.core.CHANGELOG_URL
import com.joulearc.app.core.DEVELOPER_URL
import com.joulearc.app.core.GroupLabel
import com.joulearc.app.core.LICENSE_URL
import com.joulearc.app.core.PRIVACY_URL
import com.joulearc.app.core.PageTitle
import com.joulearc.app.core.REPOSITORY_URL
import com.joulearc.app.core.RowGroup
import com.joulearc.app.core.TERMS_URL
import com.joulearc.app.data.DeviceInfo

/** About page: the app, the developer, the project, the legal pages and the invitation to the repository. */
@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // The saved picture shows at once; GitHub is asked for a new one only when it is time.
    val avatar by produceState<ImageBitmap?>(null) {
        value = withContext(Dispatchers.IO) { DeveloperAvatar.cached(context)?.asImageBitmap() }
        val changed = withContext(Dispatchers.IO) {
            DeveloperAvatar.refreshIfDue(context, "JouleArc/" + DeviceInfo.appVersion(context))
        }
        if (changed) value = withContext(Dispatchers.IO) { DeveloperAvatar.cached(context)?.asImageBitmap() }
    }
    val avatarContent: (@Composable () -> Unit)? = avatar?.let { bitmap ->
        @Composable {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(CircleShape),
            )
        }
    }
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PageTitle(stringResource(R.string.about_section))

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
                painter = painterResource(R.drawable.ic_app_logo),
                contentDescription = null,
                modifier = Modifier.size(112.dp),
            )
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Text(
                    text = DeviceInfo.appVersion(context),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            Text(
                text = stringResource(R.string.about_description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }

        GroupLabel(stringResource(R.string.about_developer))
        RowGroup(
            listOf {
                ClickRow(
                    icon = R.drawable.ic_person,
                    title = "rilclesaraiva",
                    subtitle = stringResource(R.string.about_developer_sub),
                    onClick = { openUrl(context, DEVELOPER_URL) },
                    chevron = false,
                    iconContent = avatarContent,
                )
            },
        )

        GroupLabel(stringResource(R.string.about_project))
        RowGroup(
            listOf(
                {
                    ClickRow(
                        icon = R.drawable.ic_code,
                        title = stringResource(R.string.settings_source),
                        subtitle = stringResource(R.string.about_source_sub),
                        onClick = { openUrl(context, REPOSITORY_URL) },
                        chevron = false,
                    )
                },
                {
                    ClickRow(
                        icon = R.drawable.ic_new,
                        title = stringResource(R.string.about_changelog),
                        subtitle = stringResource(R.string.about_changelog_sub),
                        onClick = { openUrl(context, CHANGELOG_URL) },
                        chevron = false,
                    )
                },
                {
                    ClickRow(
                        icon = R.drawable.ic_bug,
                        title = stringResource(R.string.settings_report),
                        subtitle = stringResource(R.string.settings_report_sub),
                        onClick = { sendReport(context) },
                        chevron = false,
                    )
                },
            ),
        )

        GroupLabel(stringResource(R.string.about_legal))
        RowGroup(
            listOf(
                {
                    ClickRow(
                        icon = R.drawable.ic_lock,
                        title = stringResource(R.string.link_privacy),
                        subtitle = stringResource(R.string.about_privacy_sub),
                        onClick = { openUrl(context, PRIVACY_URL) },
                        chevron = false,
                    )
                },
                {
                    ClickRow(
                        icon = R.drawable.ic_description,
                        title = stringResource(R.string.link_terms),
                        subtitle = stringResource(R.string.about_terms_sub),
                        onClick = { openUrl(context, TERMS_URL) },
                        chevron = false,
                    )
                },
                {
                    ClickRow(
                        icon = R.drawable.ic_description,
                        title = stringResource(R.string.about_license),
                        subtitle = stringResource(R.string.about_license_sub),
                        onClick = { openUrl(context, LICENSE_URL) },
                        chevron = false,
                    )
                },
            ),
        )

        RowGroup(
            listOf {
                ClickRow(
                    icon = R.drawable.ic_heart,
                    title = stringResource(R.string.support_title),
                    subtitle = stringResource(R.string.support_sub),
                    onClick = { openUrl(context, REPOSITORY_URL) },
                    chevron = false,
                )
            },
        )
    }
}
