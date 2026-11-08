package com.joulearc.app.core

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.joulearc.app.R

const val REPOSITORY_URL = "https://github.com/rilclesaraiva/Joule-Arc"
const val PRIVACY_URL = "https://rilclesaraiva.github.io/Joule-Arc/privacy"
const val TERMS_URL = "https://rilclesaraiva.github.io/Joule-Arc/terms"

/** Invite that leads to the repository, plus the legal links. Never mentions donations or payments. */
@Composable
fun SupportFooter(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.support_invite),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            textDecoration = TextDecoration.Underline,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { uriHandler.openUri(REPOSITORY_URL) }
                .padding(vertical = 12.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp, androidx.compose.ui.Alignment.CenterHorizontally),
        ) {
            Text(
                text = stringResource(R.string.link_privacy),
                style = MaterialTheme.typography.bodySmall,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { uriHandler.openUri(PRIVACY_URL) }.padding(8.dp),
            )
            Text(
                text = stringResource(R.string.link_terms),
                style = MaterialTheme.typography.bodySmall,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { uriHandler.openUri(TERMS_URL) }.padding(8.dp),
            )
        }
    }
}
