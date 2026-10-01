package com.cbgm.sparrow.feature.polls.presentation.message.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.media.presentation.component.MediaThumbnail
import com.cbgm.sparrow.feature.media.presentation.model.MediaItemUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaTypeUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_polls_more_media
import org.jetbrains.compose.resources.stringResource

@Composable
fun PollMessageMedia(
    media: List<MediaItemUi>,
    onMediaClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (media.isEmpty()) return
    val visible = media.take(3)

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)) {
        visible.forEachIndexed { index, item ->
            Surface(
                modifier = Modifier.weight(1f).aspectRatio(1.25f).clickable { onMediaClick(index) },
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(Dimens.Base.borderStrokeWidth, MaterialTheme.colorScheme.outlineVariant),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    MediaThumbnail(media = item, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                    if (index == 2 && media.size > 3) {
                        Surface(
                            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = stringResource(Res.string.feature_polls_more_media, media.size - 3),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PollMessageMediaPreview() {
    SparrowTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            PollMessageMedia(
                media = List(5) { index ->
                    MediaItemUi("$index", MediaTypeUi.IMAGE, "image/jpeg", localFilePath = "/preview/$index.jpg")
                },
                onMediaClick = {}
            )
        }
    }
}
