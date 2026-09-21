package com.cbgm.sparrow.feature.media.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.media.presentation.mapper.toMediaItem
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionSource
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionType
import com.cbgm.sparrow.feature.media.util.toReadableByteSize

@Composable
fun MediaSelectionPreview(
    media: List<MediaSelection>,
    onClick: (MediaSelectionSource) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (media.isEmpty()) return

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        items(media, key = MediaSelection::id) { selection ->
            when (selection.type) {
                MediaSelectionType.IMAGE,
                MediaSelectionType.VIDEO ->
                    MediaSelectionItem(
                        selection = selection,
                        enabled = enabled,
                        onClick = { onClick(MediaSelectionSource.GALLERY) },
                        onRemove = { onRemove(selection.id) }
                    )

                MediaSelectionType.FILE ->
                    FileSelectionItem(
                        selection = selection,
                        enabled = enabled,
                        onClick = { onClick(MediaSelectionSource.FILE_PICKER) },
                        onRemove = { onRemove(selection.id) }
                    )
            }
        }
    }
}

@Composable
private fun MediaSelectionItem(
    selection: MediaSelection,
    enabled: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Box(
        modifier =
            Modifier
                .size(Dimens.MediaSelection.previewSize)
                .clickable(enabled = enabled, onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            MediaThumbnail(
                media = selection.toMediaItem(),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        if (selection.type == MediaSelectionType.VIDEO) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = MaterialTheme.shapes.extraLarge,
                color =
                    MaterialTheme.colorScheme.scrim.copy(
                        alpha = Alpha.MediaSelection.playButtonBackground
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.MediaSelection.previewPlayIconSize),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        RemoveButton(enabled = enabled, onRemove = onRemove)
    }
}

@Composable
private fun FileSelectionItem(
    selection: MediaSelection,
    enabled: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Box {
        Surface(
            modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier
                    .width(Dimens.MediaSelection.filePreviewWidth)
                    .padding(MaterialTheme.spacing.base),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.MediaSelection.filePreviewIconSize)
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.base))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selection.fileName ?: selection.id,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = selection.byteSize.toReadableByteSize(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        RemoveButton(enabled = enabled, onRemove = onRemove)
    }
}

@Composable
private fun BoxScope.RemoveButton(
    enabled: Boolean,
    onRemove: () -> Unit
) {
    Surface(
        modifier =
            Modifier
                .align(Alignment.TopEnd)
                .size(Dimens.MediaSelection.previewRemoveButtonSize)
                .clickable(enabled = enabled, onClick = onRemove),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(Dimens.MediaSelection.previewRemoveIconSize),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview
@Composable
private fun MediaSelectionPreviewPreview() {
    SparrowTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MediaSelectionPreview(
                media = previewMediaSelections(),
                onClick = {},
                onRemove = {}
            )
        }
    }
}

fun previewMediaSelections(): List<MediaSelection> = listOf(
    MediaSelection(
        id = "preview-image",
        type = MediaSelectionType.IMAGE,
        localFilePath = "/preview/image.png",
        byteSize = 1024,
        mimeType = "image/png",
        source = MediaSelectionSource.GALLERY,
        width = 48,
        height = 48
    ),
    MediaSelection(
        id = "preview-video",
        type = MediaSelectionType.VIDEO,
        localFilePath = "/preview/video.mp4",
        byteSize = 1024,
        mimeType = "video/mp4",
        source = MediaSelectionSource.GALLERY,
        width = 48,
        height = 48,
        durationMilliseconds = 12_000L
    ),
    MediaSelection(
        id = "preview-file",
        type = MediaSelectionType.FILE,
        localFilePath = "/preview/document.pdf",
        byteSize = 1024,
        mimeType = "application/pdf",
        source = MediaSelectionSource.FILE_PICKER,
        fileName = "document.pdf"
    )
)
