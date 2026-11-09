package com.joulearc.app.core

import android.os.SystemClock
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joulearc.app.R

/** Large page title that scrolls with the content. It leaves room for the floating back button above it. */
@Composable
fun PageTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineLarge,
        modifier = modifier.fillMaxWidth().padding(start = 8.dp, top = 64.dp, bottom = 4.dp),
    )
}

/** Round back button, slightly see-through, fixed over the page. */
@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
        modifier = modifier.size(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.cd_back))
        }
    }
}

private const val DOUBLE_TAP_MS = 1_000L

/**
 * For destructive buttons: the action runs only on a second tap within a second. The first tap
 * shows a small pop-up asking for the second one.
 */
@Composable
fun rememberDoubleTapConfirm(onConfirm: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val action by rememberUpdatedState(onConfirm)
    var last by remember { mutableLongStateOf(0L) }
    return {
        val now = SystemClock.elapsedRealtime()
        if (last != 0L && now - last <= DOUBLE_TAP_MS) {
            last = 0L
            action()
        } else {
            last = now
            Toast.makeText(context, R.string.confirm_double_tap, Toast.LENGTH_SHORT).show()
        }
    }
}

/** Rows joined into one block: big outer corners, small gaps and corners between the rows. */
@Composable
fun RowGroup(items: List<@Composable () -> Unit>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.dp)) {
        items.forEachIndexed { i, item ->
            val outer = 28.dp
            val inner = 6.dp
            val shape = androidx.compose.foundation.shape.RoundedCornerShape(
                topStart = if (i == 0) outer else inner,
                topEnd = if (i == 0) outer else inner,
                bottomStart = if (i == items.lastIndex) outer else inner,
                bottomEnd = if (i == items.lastIndex) outer else inner,
            )
            Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                item()
            }
        }
    }
}

/** Round icon badge used at the start of list rows. */
@Composable
fun RowIcon(icon: Int, modifier: Modifier = Modifier) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = modifier.size(48.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
        }
    }
}

/** Small section label in the accent color, as in the About page. */
@Composable
fun GroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 12.dp, top = 12.dp),
    )
}
