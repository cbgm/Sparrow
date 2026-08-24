package com.cbgm.sparrow.feature.chats.presentation.details.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.cbgm.sparrow.feature.chats.presentation.details.GroupDetailsFlow
import com.cbgm.sparrow.feature.chats.presentation.details.model.DetailsTarget
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.presentation.details.ContactDetailsRoute
import com.cbgm.sparrow.feature.identity.device.rememberIdentityShareLauncher
import com.cbgm.sparrow.feature.identity.domain.model.SharedContactDetails
import com.cbgm.sparrow.feature.identity.domain.model.SharedIdentityPayload
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityShareRepository
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_share_contact
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun DetailsRoute(
    target: DetailsTarget,
    openVerification: Boolean,
    requestGroupLeave: Boolean = false
) {
    val identityShareRepository = koinInject<IdentityShareRepository>()
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
            ContactDetailsRoute(
                contactId = target.contactId,
                openVerification = openVerification,
                onShareContact = { contact ->
                    encodeContactForSharing(
                        contact = contact,
                        identityShareRepository = identityShareRepository,
                        onEncoded = { encodedIdentity ->
                            encodedContactToShare = encodedIdentity
                            shouldLaunchShare = true
                        }
                    )
                }
            )
        }

        is DetailsTarget.Group -> {
            GroupDetailsFlow(
                requestLeave = requestGroupLeave
            )
        }
    }
}

private fun encodeContactForSharing(
    contact: Contact,
    identityShareRepository: IdentityShareRepository,
    onEncoded: (String) -> Unit
) {
    val identity = contact.sparrowIdentity
    val phoneNumber =
        contact
            .preferredPhoneNumber
            ?.value
            ?.takeIf(String::isNotBlank)

    if (identity != null && phoneNumber != null) {
        identityShareRepository
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
