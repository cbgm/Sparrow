package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessagePartUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.fake_location_map
import com.cbgm.sparrow.resources.feature_chats_location
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LocationMessageBubbleBody(
    locationPart: MessagePartUi.Location,
    onAttachmentVisible: (String) -> Unit,
    onAttachmentClick: (String) -> Unit
) {
    LaunchedEffect(
        locationPart.id,
        locationPart.location
    ) {
        if (locationPart.location == null) {
            onAttachmentVisible(locationPart.id)
        }
    }

    Content(
        location = locationPart.location,
        onClick = {
            onAttachmentClick(locationPart.id)
        }
    )
}

@Composable
private fun Content(
    location: CurrentLocation?,
    onClick: () -> Unit
) {
    Box(
        modifier =
            Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(enabled = location != null) {
                    location?.let { onClick() }
                }
    ) {
        if (location == null) {
            LoadingContent()
        } else {
            LocationContent(location = location)
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.large),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(Dimens.MessageAttachment.loadingIndicatorSize),
            strokeWidth = Dimens.Base.progressIndicatorStrokeWidth
        )
    }
}

@Composable
private fun LocationContent(
    location: CurrentLocation
) {
    Column(modifier = Modifier.width(Dimens.MessageBubble.staticBubbleSize)) {
        FakeLocationPreview(
            modifier = Modifier.aspectRatio(LOCATION_PREVIEW_ASPECT_RATIO)
        )

        Row(
            modifier =
                Modifier.padding(
                    horizontal = MaterialTheme.spacing.base,
                    vertical = MaterialTheme.spacing.small
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
            ) {
                Text(
                    text = stringResource(Res.string.feature_chats_location),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = location.coordinateText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.OpaqueText),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun FakeLocationPreview(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    topStart = MaterialTheme.shapes.extraSmall.topStart,
                    topEnd = MaterialTheme.shapes.extraSmall.topEnd,
                    bottomStart = CornerSize(Dimens.Base.zero),
                    bottomEnd = CornerSize(Dimens.Base.zero)
                )
            )
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Image(
            painter = painterResource(Res.drawable.fake_location_map),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .size(Dimens.MessageAttachment.previewSize),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun CurrentLocation.coordinateText(): String =
    "$latitude, $longitude"

@Preview
@Composable
private fun LocationMessageBubbleBodyPreview() {
    SparrowTheme {
        LocationMessageBubbleBody(
            locationPart =
                MessagePartUi.Location(
                    id = "location-preview",
                    location =
                        CurrentLocation(
                            latitude = 50.2586,
                            longitude = 10.9644
                        )
                ),
            onAttachmentVisible = {},
            onAttachmentClick = {}
        )
    }
}

private const val LOCATION_PREVIEW_ASPECT_RATIO = 1.6f
