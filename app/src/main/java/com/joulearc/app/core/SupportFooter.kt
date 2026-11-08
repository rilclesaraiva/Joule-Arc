package com.joulearc.app.core

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

/** Invite that leads to the repository. Never mentions donations or payments in the app. */
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
    }
}
