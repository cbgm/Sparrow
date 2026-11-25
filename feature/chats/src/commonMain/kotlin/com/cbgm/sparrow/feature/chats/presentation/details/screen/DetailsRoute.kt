package com.cbgm.sparrow.feature.chats.presentation.details.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.chats.domain.usecase.contact.EncodeContactForSharingUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.contact.ReconnectContactUseCase
import com.cbgm.sparrow.feature.chats.presentation.details.GroupDetailsFlow
import com.cbgm.sparrow.feature.chats.presentation.details.model.DetailsTarget
import com.cbgm.sparrow.feature.contactimport.presentation.verify.ContactQrVerificationFlow
import com.cbgm.sparrow.feature.contacts.presentation.details.ContactDetailsRoute
import com.cbgm.sparrow.feature.identity.device.rememberIdentityShareLauncher
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_share_contact
import com.cbgm.sparrow.resources.feature_chats_reconnect_queued
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun DetailsRoute(
    target: DetailsTarget,
    openVerification: Boolean,
    requestGroupLeave: Boolean = false
) {
    val encodeContactForSharing = koinInject<EncodeContactForSharingUseCase>()
    val reconnectContact = koinInject<ReconnectContactUseCase>()
    val scope = rememberCoroutineScope()
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
                qrScanner = { enabled ->
                    ContactQrVerificationFlow(
                        contactId = target.contactId,
                        enabled = enabled
                    )
                },
                onShareContact = { contact ->
                    scope.launch {
                        encodeContactForSharing(
                            contactId = contact.id,
                            displayName = contact.displayName,
                            phoneNumber = contact.preferredPhoneNumber
                        )
                            .onSuccess { encodedIdentity ->
                                if (!encodedIdentity.isNullOrBlank()) {
                                    encodedContactToShare = encodedIdentity
                                    shouldLaunchShare = true
                                }
                            }
                    }
                },
                onReconnect = {
                    scope.launch {
                        reconnectContact(target.contactId)
                            .onSuccess { SparrowLog.hint(getString(Res.string.feature_chats_reconnect_queued)) }
                            .onFailure { error ->
                                SparrowLog.error("DetailsRoute", "Could not queue a new invitation", error)
                            }
                    }
                }
            )
        }

        is DetailsTarget.Group -> {
            GroupDetailsFlow(
                conversationId = target.conversationId,
                requestLeave = requestGroupLeave
            )
        }
    }
}
