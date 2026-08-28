package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.attachmentColors
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessagePartUi
import kotlin.math.roundToLong

@Composable
internal fun LocationMessageBubbleBody(
    locationPart: MessagePartUi.LocationUi,
    onAttachmentVisible: (String) -> Unit,
    onAttachmentClick: (String) -> Unit
) {
    val location = locationPart.location

    LaunchedEffect(locationPart.id, location) {
        if (location == null) onAttachmentVisible(locationPart.id)
    }

    Box(
        modifier =
            Modifier
                .padding(
                    horizontal = MaterialTheme.spacing.small,
                    vertical = MaterialTheme.spacing.base
                )
                .size(Dimens.MessageAttachment.previewSize)
                .clickable(enabled = location != null) { onAttachmentClick(locationPart.id) },
        contentAlignment = Alignment.Center
    ) {
        if (location == null) {
            CircularProgressIndicator(
                modifier = Modifier.size(Dimens.MessageAttachment.loadingIndicatorSize),
                strokeWidth = Dimens.Base.progressIndicatorStrokeWidth
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = MaterialTheme.attachmentColors.location
                )
                Text(
                    text = "${location.latitude.toCoordinateText()}\n${location.longitude.toCoordinateText()}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Preview
@Composable
private fun LocationMessageBubbleBodyPreview() {
    SparrowTheme {
        LocationMessageBubbleBody(
            locationPart =
                MessagePartUi.LocationUi(
                    id = "preview-location",
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

private fun Double.toCoordinateText(): String =
    (
        (this * LOCATION_COORDINATE_SCALE).roundToLong()
            .toDouble() / LOCATION_COORDINATE_SCALE
    ).toString()

private const val LOCATION_COORDINATE_SCALE = 100_000.0
