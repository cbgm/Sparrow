package com.cbgm.sparrow.feature.chats.data.direct.invitation

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.chats.data.direct.outgoing.DirectPendingAuthorizationMessageCoordinator
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.HandleAcceptedDirectInvitationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.HandleDeclinedDirectInvitationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ObserveAcceptedDirectInvitationsUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ObserveDeclinedDirectInvitationsUseCase
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class DirectInvitationConversationCoordinator(
    private val observeAcceptedInvitationContactIds: ObserveAcceptedDirectInvitationsUseCase,
    private val observeDeclinedInvitationContactIds: ObserveDeclinedDirectInvitationsUseCase,
    private val handleAcceptedInvitation: HandleAcceptedDirectInvitationUseCase,
    private val handleDeclinedInvitation: HandleDeclinedDirectInvitationUseCase,
    private val pendingAuthorizationMessageCoordinator: DirectPendingAuthorizationMessageCoordinator
) {
    private val logger = SparrowLog.withTag("DirectInvitationConversationCoordinator")

    suspend fun run(): Unit =
        coroutineScope {
            launch { collectAcceptedInvitations() }
            launch { collectDeclinedInvitations() }
            launch { pendingAuthorizationMessageCoordinator.run() }
        }

    private suspend fun collectAcceptedInvitations() {
        observeAcceptedInvitationContactIds().collect { contactIds ->
            contactIds.forEach { contactId ->
                handleAcceptedInvitation(contactId)
                    .onFailure { error ->
                        logger.warn(error) {
                            "Accepted direct invitation could not be applied for contactId=$contactId"
                        }
                    }
            }
        }
    }

    private suspend fun collectDeclinedInvitations() {
        observeDeclinedInvitationContactIds().collect { contactIds ->
            contactIds.forEach { contactId ->
                handleDeclinedInvitation(contactId)
                    .onFailure { error ->
                        logger.warn(error) {
                            "Declined direct invitation could not be applied for contactId=$contactId"
                        }
                    }
            }
        }
    }
}
