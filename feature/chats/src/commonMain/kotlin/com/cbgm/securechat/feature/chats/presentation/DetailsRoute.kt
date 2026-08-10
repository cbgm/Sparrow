package com.cbgm.securechat.feature.chats.presentation.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.cbgm.securechat.feature.chats.presentation.screen.details.GroupDetailsFlow
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.presentation.ContactDetailsFlow
import com.cbgm.securechat.feature.identity.domain.model.SharedContactDetails
import com.cbgm.securechat.feature.identity.domain.model.SharedIdentityPayload
import com.cbgm.securechat.feature.identity.domain.service.IdentityShareCodec
import com.cbgm.securechat.feature.identity.presentation.platform.rememberIdentityShareLauncher
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_share_contact
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

sealed interface DetailsTarget {
    data class Contact(
        val contactId: String
    ) : DetailsTarget

    data class Group(
        val conversationId: String
    ) : DetailsTarget
}

@Composable
fun DetailsRoute(
    target: DetailsTarget,
    openVerification: Boolean,
    verificationRevision: Int
) {
    val identityShareCodec = koinInject<IdentityShareCodec>()
    var encodedContactToShare by remember { mutableStateOf("") }
    val launchContactShare =
        rememberIdentityShareLauncher(
            encodedIdentity = encodedContactToShare,
            shareTitle = stringResource(Res.string.base_share_contact)
        )
    var shouldLaunchShare by remember { mutableStateOf(false) }

    LaunchedEffect(encodedContactToShare, shouldLaunchShare) {
        if (shouldLaunchShare && encodedContactToShare.isNotBlank()) {
            launchContactShare()
            shouldLaunchShare = false
        }
    }

    when (target) {
        is DetailsTarget.Contact -> {
            ContactDetailsFlow(
                contactId = target.contactId,
                openVerification = openVerification,
                verificationRevision = verificationRevision,
                onShareContact = { contact ->
                    encodeContactForSharing(
                        contact = contact,
                        identityShareCodec = identityShareCodec,
                        onEncoded = { encodedIdentity ->
                            encodedContactToShare = encodedIdentity
                            shouldLaunchShare = true
                        }
                    )
                }
            )
        }

        is DetailsTarget.Group -> {
            GroupDetailsFlow(conversationId = target.conversationId)
        }
    }
}

private fun encodeContactForSharing(
    contact: Contact,
    identityShareCodec: IdentityShareCodec,
    onEncoded: (String) -> Unit
) {
    val identity = contact.secureChatIdentity
    val phoneNumber =
        contact
            .preferredPhoneNumber
            ?.value
            ?.takeIf(String::isNotBlank)

    if (identity != null && phoneNumber != null) {
        identityShareCodec
            .encode(
                payload =
                    SharedIdentityPayload(
                        version = 1,
                        encryptionPublicKey = identity.encryptionPublicKey,
                        signingPublicKey = identity.signingPublicKey,
                        contactDetails =
                            SharedContactDetails(
                                displayName = contact.displayName,
                                phoneNumber = phoneNumber
                            )
                    )
            ).onSuccess(onEncoded)
    }
}
