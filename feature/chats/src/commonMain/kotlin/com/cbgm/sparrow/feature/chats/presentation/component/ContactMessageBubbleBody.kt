package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
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
import com.cbgm.sparrow.core.ui.theme.attachmentColors
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessagePartUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.fake_contact_card
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun ContactMessageBubbleBody(
    contactPart: MessagePartUi.Contact,
    onAttachmentVisible: (String) -> Unit,
    onContactClick: (SharedContact) -> Unit
) {
    val contact = contactPart.contact

    LaunchedEffect(contactPart.id, contact) {
        if (contact == null) {
            onAttachmentVisible(contactPart.id)
        }
    }

    Content(
        contact = contact,
        onContactClick = onContactClick
    )
}

@Composable
private fun Content(
    contact: SharedContact?,
    onContactClick: (SharedContact) -> Unit
) {
    Box(
        modifier =
            Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(enabled = contact != null) {
                    contact?.let(onContactClick)
                }
    ) {
        if (contact == null) {
            LoadingContent()
        } else {
            ContactContent(contact = contact)
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
private fun ContactContent(
    contact: SharedContact
) {
    Column(modifier = Modifier.width(Dimens.MessageBubble.staticBubbleSize)) {
        FakeContactPreview(
            contact = contact,
            modifier = Modifier.aspectRatio(CONTACT_PREVIEW_ASPECT_RATIO)
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
                contact.displayName
                    ?.takeIf(String::isNotBlank)
                    ?.let { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.OpaqueText),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null,
                tint = MaterialTheme.attachmentColors.contact
            )
        }
    }
}

@Composable
private fun FakeContactPreview(
    contact: SharedContact,
    modifier: Modifier = Modifier
) {
    Box(
        modifier =
            modifier
                .clip(
                    RoundedCornerShape(
                        topStart = MaterialTheme.shapes.extraSmall.topStart,
                        topEnd = MaterialTheme.shapes.extraSmall.topEnd,
                        bottomStart = CornerSize(Dimens.Base.zero),
                        bottomEnd = CornerSize(Dimens.Base.zero)
                    )
                )
                .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Image(
            painter = painterResource(Res.drawable.fake_contact_card),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = MaterialTheme.spacing.base)
                    .size(Dimens.MessageAttachment.previewSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.initial(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

private fun SharedContact.initial(): String =
    displayName
        ?.trim()
        ?.firstOrNull()
        ?.uppercase()
        ?: phoneNumber
            .trim()
            .firstOrNull()
            ?.uppercase()
        ?: "?"

@Preview
@Composable
private fun ContactMessageBubbleBodyPreview() {
    SparrowTheme {
        ContactMessageBubbleBody(
            contactPart =
                MessagePartUi.Contact(
                    id = "preview-contact",
                    contact =
                        SharedContact(
                            displayName = "Anna Keller",
                            phoneNumber = "+49 151 12345678"
                        )
                ),
            onAttachmentVisible = {},
            onContactClick = {}
        )
    }
}

private const val CONTACT_PREVIEW_ASPECT_RATIO = 1.6f
