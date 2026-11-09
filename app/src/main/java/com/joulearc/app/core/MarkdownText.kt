package com.joulearc.app.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

private val INLINE = Regex("""\*\*(.+?)\*\*|`(.+?)`|\[(.+?)]\((.+?)\)""")

/** Bold, code and links (shown as their text) inside one line of release notes. */
private fun inline(text: String): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in INLINE.findAll(text)) {
        append(text.substring(last, m.range.first))
        when {
            m.groups[1] != null -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(m.groupValues[1]) }
            m.groups[2] != null -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(m.groupValues[2]) }
            else -> append(m.groupValues[3])
        }
        last = m.range.last + 1
    }
    append(text.substring(last))
}

/** Small renderer for release notes: headings, bullet lists, bold, code and plain paragraphs. */
@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        text.lines().forEach { raw ->
            val line = raw.trim()
            when {
                line.isEmpty() -> Unit
                line.startsWith("#") -> Text(
                    text = inline(line.trimStart('#').trim()),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                line.startsWith("- ") || line.startsWith("* ") -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("•", style = MaterialTheme.typography.bodyLarge)
                    Text(inline(line.drop(2).trim()), style = MaterialTheme.typography.bodyLarge)
                }
                else -> Text(inline(line), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
