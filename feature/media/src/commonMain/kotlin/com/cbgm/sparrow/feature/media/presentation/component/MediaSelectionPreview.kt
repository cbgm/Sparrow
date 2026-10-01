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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.media.presentation.model.FileMediaSelectionUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaSourceUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaTypeUi
import com.cbgm.sparrow.feature.media.presentation.model.VisualMediaSelectionUi
import com.cbgm.sparrow.feature.media.util.toReadableByteSize

@Composable
fun MediaSelectionPreview(
    media: List<MediaSelectionUi>,
    onClick: (MediaSourceUi) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (media.isEmpty()) return

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        items(media, key = MediaSelectionUi::id) { selection ->
            when (selection) {
                is VisualMediaSelectionUi ->
                    MediaSelectionItem(
                        selection = selection,
                        enabled = enabled,
                        onClick = { onClick(selection.source) },
                        onRemove = { onRemove(selection.id) }
                    )

                is FileMediaSelectionUi ->
                    FileSelectionItem(
                        selection = selection,
                        enabled = enabled,
                        onClick = { onClick(selection.source) },
                        onRemove = { onRemove(selection.id) }
                    )
            }
        }
    }
}

@Composable
private fun MediaSelectionItem(
    selection: VisualMediaSelectionUi,
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
                media = selection,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        if (selection.type == MediaTypeUi.VIDEO) {
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
    selection: FileMediaSelectionUi,
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
                        text = selection.fileName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
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
        color = MaterialTheme.colorScheme.surfaceContainerHigh
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

fun previewMediaSelections(): List<MediaSelectionUi> =
    listOf(
        VisualMediaSelectionUi(
            id = "preview-image",
            localFilePath = "/preview/image.png",
            byteSize = 1024,
            mimeType = "image/png",
            source = MediaSourceUi.GALLERY,
            type = MediaTypeUi.IMAGE,
            width = 48,
            height = 48
        ),
        VisualMediaSelectionUi(
            id = "preview-video",
            localFilePath = "/preview/video.mp4",
            byteSize = 4096,
            mimeType = "video/mp4",
            source = MediaSourceUi.GALLERY,
            type = MediaTypeUi.VIDEO,
            width = 48,
            height = 48,
            durationMilliseconds = 1_000
        ),
        FileMediaSelectionUi(
            id = "preview-file",
            localFilePath = "/preview/file.pdf",
            byteSize = 2048,
            mimeType = "application/pdf",
            source = MediaSourceUi.FILE_PICKER,
            fileName = "document.pdf"
        )
    )
