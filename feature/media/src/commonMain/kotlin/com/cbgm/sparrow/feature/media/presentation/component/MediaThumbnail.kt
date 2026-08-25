package com.cbgm.sparrow.feature.media.presentation.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.feature.media.device.MediaImage
import com.cbgm.sparrow.feature.media.device.VideoThumbnail
import com.cbgm.sparrow.feature.media.presentation.model.MediaItem
import com.cbgm.sparrow.feature.media.presentation.model.MediaType

/**
 * Loads media thumbnail content only.
 *
 * Shape, clipping, tile size, badges and overlays belong to the caller so message attachments,
 * gallery selection and future camera UI can keep their own visual language.
 */
@Composable
fun MediaThumbnail(
    media: MediaItem,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null
) {
    val thumbnailCacheKey = "media-thumbnail:${media.id}"

    when (media.type) {
        MediaType.IMAGE ->
            MediaImage(
                data = media.thumbnailBytes ?: media.bytes,
                cacheKey = thumbnailCacheKey,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )

        MediaType.VIDEO -> {
            val explicitThumbnail = media.thumbnailBytes
            if (explicitThumbnail != null) {
                MediaImage(
                    data = explicitThumbnail,
                    cacheKey = thumbnailCacheKey,
                    contentDescription = contentDescription,
                    modifier = modifier,
                    contentScale = contentScale
                )
            } else {
                VideoThumbnail(
                    media = media,
                    modifier = modifier,
                    contentScale = contentScale
                )
            }
        }
    }
}

@Preview
@Composable
private fun MediaThumbnailPreview() {
    SparrowTheme {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            MediaThumbnail(
                media =
                    MediaItem(
                        id = "preview-image",
                        type = MediaType.IMAGE,
                        mimeType = "image/jpeg",
                        bytes = byteArrayOf()
                    )
            )
        }
    }
}
