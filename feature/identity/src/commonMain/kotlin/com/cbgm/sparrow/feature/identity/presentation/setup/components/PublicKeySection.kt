package com.cbgm.sparrow.feature.identity.presentation.setup.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.extensions.toHexString
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing

@Composable
fun PublicKeySection(
    icon: ImageVector,
    title: String,
    description: String,
    key: ByteArray,
    onCopied: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.OpaqueText),
                modifier = Modifier.size(Dimens.IdentityScreen.publicKeyIconSize)
            )

            Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

        KeyHexGrid(
            key = key,
            modifier = Modifier.padding(vertical = MaterialTheme.spacing.small),
            onCopied = onCopied
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun KeyHexGrid(key: ByteArray, modifier: Modifier = Modifier, onCopied: () -> Unit = {}) {
    val fullHexValue = key.toHexString().uppercase()
    val groups = fullHexValue.chunked(4)
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = modifier.fillMaxWidth().combinedClickable(
            onClick = {},
            onLongClick = {
                clipboard.setText(AnnotatedString(fullHexValue))
                onCopied()
            }
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        groups.chunked(4).forEach { line ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
            ) {
                repeat(4) { index ->
                    Text(
                        text = line.getOrElse(index) { "" },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PublicKeySectionPreview() {
    SparrowTheme {
        PublicKeySection(
            icon = Icons.Default.VerifiedUser,
            title = "Test",
            description = "Test",
            key = ByteArray(32)
        )
    }
}
