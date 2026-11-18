package com.cbgm.sparrow.feature.chats.presentation.common.composer.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.ComposerPreviewUi
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.ComposerPreviewUi.Type

@Composable
internal fun ComposerPreview(
    previewUi: ComposerPreviewUi,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = MaterialTheme.spacing.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        Content(
            previewUi = previewUi,
            modifier = Modifier.weight(1f)
        )

        ComposerButton(
            icon = Icons.Default.Close,
            onClick = onCancel
        )
    }
}

@Composable
private fun Content(
    previewUi: ComposerPreviewUi,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro),
        modifier = modifier
    ) {
        Icon(
            imageVector = previewUi.icon,
            contentDescription = null,
            modifier = Modifier.size(Dimens.MessageBubble.iconSize),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = previewUi.iconText,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.primary
        )

        previewUi.additionalText.takeIf(String::isNotBlank)?.let { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
private fun ComposerPreviewPreview() {
    SparrowTheme {
        ComposerPreview(
            previewUi = ComposerPreviewUi(
                icon = Icons.AutoMirrored.Filled.Reply,
                iconText = "Reply",
                additionalText = "The original message should only appear as a short excerpt in the composer",
                type = Type.REPLY
            ),
            onCancel = {}
        )
    }
}
