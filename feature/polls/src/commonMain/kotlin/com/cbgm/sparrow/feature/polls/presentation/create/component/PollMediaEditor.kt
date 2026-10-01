package com.cbgm.sparrow.feature.polls.presentation.create.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.media.presentation.component.MediaSelectionPreview
import com.cbgm.sparrow.feature.media.presentation.model.MediaSourceUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaTypeUi
import com.cbgm.sparrow.feature.media.presentation.model.VisualMediaSelectionUi
import com.cbgm.sparrow.feature.polls.util.PollConstants
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_polls_add_media
import com.cbgm.sparrow.resources.feature_polls_media
import com.cbgm.sparrow.resources.feature_polls_media_limit
import org.jetbrains.compose.resources.stringResource

@Composable
fun PollMediaEditor(
    media: List<VisualMediaSelectionUi>,
    onAddMedia: () -> Unit,
    onRemoveMedia: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        Text(text = stringResource(Res.string.feature_polls_media), style = MaterialTheme.typography.titleSmall)
        Text(
            text = stringResource(Res.string.feature_polls_media_limit, PollConstants.MAX_MEDIA_ITEMS),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        MediaSelectionPreview(
            media = media,
            onClick = { onAddMedia() },
            onRemove = onRemoveMedia,
            previewSize = Dimens.Poll.previewMediaSelectionSize
        )

        if (media.size < PollConstants.MAX_MEDIA_ITEMS) {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)) {
                TextButton(onClick = onAddMedia) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Text(text = stringResource(Res.string.feature_polls_add_media))
                }
            }
        }
    }
}

@Preview
@Composable
private fun PollMediaEditorPreview() {
    SparrowTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            PollMediaEditor(
                media = previewPollMediaSelections(),
                onAddMedia = {},
                onRemoveMedia = {}
            )
        }
    }
}

internal fun previewPollMediaSelections(): List<VisualMediaSelectionUi> =
    listOf(
        VisualMediaSelectionUi(
            id = "poll-preview-image",
            localFilePath = "/preview/poll-image.jpg",
            byteSize = 1024,
            mimeType = "image/jpeg",
            source = MediaSourceUi.GALLERY,
            type = MediaTypeUi.IMAGE,
            width = 1200,
            height = 900
        ),
        VisualMediaSelectionUi(
            id = "poll-preview-video",
            localFilePath = "/preview/poll-video.mp4",
            byteSize = 4096,
            mimeType = "video/mp4",
            source = MediaSourceUi.GALLERY,
            type = MediaTypeUi.VIDEO,
            thumbnailFilePath = "/preview/poll-video-thumb.jpg",
            width = 1920,
            height = 1080,
            durationMilliseconds = 12_000
        )
    )
