package com.cbgm.sparrow.feature.identity.presentation.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowSectionLabel
import com.cbgm.sparrow.core.ui.device.clipboard.rememberClipboardWriter
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.common_copy
import org.jetbrains.compose.resources.stringResource

@Composable
fun SparrowKeyBlock(
    label: String,
    key: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    onCopied: () -> Unit = {}
) {
    val clipboard = rememberClipboardWriter()
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SparrowSectionLabel(text = label, modifier = Modifier.weight(1f))
            Icon(
                imageVector = SparrowIcons.Copy,
                contentDescription = stringResource(Res.string.common_copy),
                modifier = Modifier
                    .size(Dimens.SettingsScreen.secondaryIconSize)
                    .clickable {
                        clipboard.copyText(key.filter(Char::isLetterOrDigit).uppercase())
                        onCopied()
                    },
                tint = MaterialTheme.colorScheme.outline
            )
        }
        description?.let { text ->
            Text(
                text = text,
                modifier = Modifier.padding(top = MaterialTheme.spacing.micro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SparrowKeyGrid(
            key = key,
            onCopied = onCopied,
            modifier = Modifier.padding(top = MaterialTheme.spacing.small)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SparrowKeyGrid(
    key: String,
    modifier: Modifier = Modifier,
    onCopied: () -> Unit = {}
) {
    val hex = key.filter(Char::isLetterOrDigit).uppercase()
    val clipboard = rememberClipboardWriter()
    SparrowCodeGrid(
        groups = hex.chunked(GroupLength),
        columns = GroupsPerLine,
        modifier = modifier.combinedClickable(
            onClick = {},
            onLongClick = {
                clipboard.copyText(hex)
                onCopied()
            }
        )
    )
}

@Composable
fun SparrowCodeGrid(
    groups: List<String>,
    columns: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        groups.chunked(columns).forEach { line ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(columns) { index ->
                    Text(
                        text = line.getOrElse(index) { "" },
                        modifier = Modifier.weight(1f),
                        style = style,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = columnAlignment(index = index, columns = columns),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun columnAlignment(
    index: Int,
    columns: Int
): TextAlign =
    when (index) {
        0 -> TextAlign.Start
        columns - 1 -> TextAlign.End
        else -> TextAlign.Center
    }

private const val GroupLength = 4
private const val GroupsPerLine = 4

@Preview
@Composable
private fun SparrowKeyBlockPreview() {
    SparrowTheme {
        SparrowKeyBlock(
            label = "Signing fingerprint",
            description = "Used to verify identity information.",
            key = "0F8D-95C9-36C5-B3A4-77E1-0A9C-5D2B-E348-0F8D-95C9-36C5-B3A4-77E1-0A9C-5D2B-E348"
        )
    }
}
