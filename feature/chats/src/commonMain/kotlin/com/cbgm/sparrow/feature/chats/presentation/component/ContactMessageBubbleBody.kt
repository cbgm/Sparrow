package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.attachmentColors
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessagePartUi

@Composable
internal fun ContactMessageBubbleBody(
    contactPart: MessagePartUi.ContactUi,
    onAttachmentVisible: (String) -> Unit,
    onContactClick: (SharedContact) -> Unit
) {
    val contact = contactPart.contact

    LaunchedEffect(contactPart.id, contact) {
        if (contact == null) onAttachmentVisible(contactPart.id)
    }

    Box(
        modifier = Modifier
            .clickable(enabled = contact != null) {
                contact?.let(onContactClick)
            },
        contentAlignment = Alignment.Center
    ) {
        if (contact == null) {
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
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.attachmentColors.contact
                )
                contact.displayName?.takeIf(String::isNotBlank)?.let { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview
@Composable
private fun ContactMessageBubbleBodyPreview() {
    SparrowTheme {
        ContactMessageBubbleBody(
            contactPart =
                MessagePartUi.ContactUi(
                    id = "preview-contact",
                    contact =
                        SharedContact(
                            displayName = "Alex",
                            phoneNumber = "+49 151 12345678"
                        )
                ),
            onAttachmentVisible = {},
            onContactClick = {}
        )
    }
}
