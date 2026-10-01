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
import com.cbgm.sparrow.feature.media.presentation.model.MediaItemUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaTypeUi
import com.cbgm.sparrow.feature.media.presentation.model.VisualMediaUi

@Composable
fun MediaThumbnail(
    media: VisualMediaUi,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
    localFilePath: String? = media.localFilePath,
    thumbnailFilePath: String? = media.thumbnailFilePath
) {
    val thumbnailCacheKey = "media-thumbnail:${media.id}"

    when (media.type) {
        MediaTypeUi.IMAGE ->
            MediaImage(
                data = null,
                localFilePath = thumbnailFilePath ?: localFilePath,
                cacheKey = thumbnailCacheKey,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )

        MediaTypeUi.VIDEO -> {
            if (thumbnailFilePath != null) {
                MediaImage(
                    data = null,
                    localFilePath = thumbnailFilePath,
                    cacheKey = thumbnailCacheKey,
                    contentDescription = contentDescription,
                    modifier = modifier,
                    contentScale = contentScale
                )
            } else {
                VideoThumbnail(
                    media = media,
                    localFilePath = localFilePath,
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
                    MediaItemUi(
                        id = "preview-image",
                        type = MediaTypeUi.IMAGE,
                        mimeType = "image/jpeg",
                        localFilePath = "/preview/image.png"
                    )
            )
        }
    }
}
