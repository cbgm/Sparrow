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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.InsertDriveFile
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
                    imageVector = Icons.Default.InsertDriveFile,
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

fun previewMediaSelections(): List<MediaSelection> =
    listOf(
        MediaSelection(
            id = "preview-image",
            type = MediaSelectionType.IMAGE,
            bytes = PREVIEW_IMAGE_PNG.hexToBytes(),
            mimeType = "image/png",
            source = MediaSelectionSource.GALLERY,
            width = 48,
            height = 48
        ),
        MediaSelection(
            id = "preview-video",
            type = MediaSelectionType.VIDEO,
            bytes = PREVIEW_VIDEO_THUMBNAIL_PNG.hexToBytes(),
            mimeType = "video/mp4",
            source = MediaSelectionSource.GALLERY,
            previewBytes = PREVIEW_VIDEO_THUMBNAIL_PNG.hexToBytes(),
            width = 48,
            height = 48,
            durationMilliseconds = 12_000L
        ),
        MediaSelection(
            id = "preview-file",
            type = MediaSelectionType.FILE,
            bytes = ByteArray(1024),
            mimeType = "application/pdf",
            source = MediaSelectionSource.FILE_PICKER,
            fileName = "document.pdf"
        )
    )

private fun String.hexToBytes(): ByteArray =
    chunked(2)
        .map { byte -> byte.toInt(radix = 16).toByte() }
        .toByteArray()

private const val PREVIEW_IMAGE_PNG =
    "89504e470d0a1a0a0000000d4948445200000030000000300802000000d8606ed0000001214944415478daed" +
        "97b11183300c4541e711320969e9d3a649459b21d8208c9126154ddaf4b461920c91823b1f015b285816e64e" +
        "aae02c5bcfdf92307971bd672919648999022950a8199930effa699f8fcd796385c634f357692067788409e4" +
        "69f021ad3205fa31a4c27d43d11572064640258e6c121e6f8cb9de8714681ba0d7ad4c0868a089c40421dac4" +
        "60da790ecd25611709c21399c2444e70696b2a28c1299d87228443fefb7ec50744ea7c7a5262e57b50d3e71e" +
        "260edd3975485e834145b5c0d7869c6feeb7602ecda0cb390b9f8b2511ae3e24e100790499dd02313a0f1852" +
        "0957d3c44db056cd99b750d261e225521ae74f66dd52ac40cb40eb16a9bbf8f2c3c4b888846beb09d88a7be4" +
        "be8c63846d4ffb2dd0119e72d49154a18e80b71397202eac99cd50000000049454e44ae426082"

private const val PREVIEW_VIDEO_THUMBNAIL_PNG =
    "89504e470d0a1a0a0000000d4948445200000030000000300802000000d8606ed00000007b4944415478daed" +
        "d0b10d4050184561efc51466515940b46a8552a1d1be52144a851dc40ec631844e21221afe5b9c33c197e3ca" +
        "34444ac5de7940ef40e35a1b3a9a7cd23a7432aea0b6987fa60c4bf504b21a06081020408000010204081020" +
        "408000010204081020409f80b23ed102597503daba5d0b24921ce80095dc0d5b50fa004e0000000049454e44" +
        "ae426082"
