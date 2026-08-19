package com.cbgm.sparrow.feature.chats.data.direct.invitation

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.chats.data.direct.outgoing.DirectOutgoingMessageProcessor
import com.cbgm.sparrow.feature.chats.data.direct.outgoing.DirectPendingAuthorizationMessageCoordinator
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityInvitationRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class DirectInvitationConversationCoordinator(
    private val identityInvitationRepository: IdentityInvitationRepository,
    private val directConversationRepository: DirectConversationRepository,
    private val outgoingMessageProcessor: DirectOutgoingMessageProcessor,
    private val pendingAuthorizationMessageCoordinator: DirectPendingAuthorizationMessageCoordinator
) {
    private val logger = SparrowLog.withTag("DirectInvitationConversationCoordinator")

    suspend fun run(): Unit =
        coroutineScope {
            launch { observeAcceptedInvitations() }
            launch { observeRejectedInvitations() }
            launch { pendingAuthorizationMessageCoordinator.run() }
        }

    private suspend fun observeAcceptedInvitations() {
        identityInvitationRepository
            .observeAcceptedContactIds()
            .collect { contactIds ->
                contactIds.forEach { contactId ->
                    directConversationRepository.getOrCreate(contactId)
                    outgoingMessageProcessor
                        .releaseWaitingForAuthorization(contactId)
                        .onFailure { error ->
                            logger.warn(error) {
                                "Queued direct messages could not be released for contactId=$contactId"
                            }
                        }
                }
            }
    }

    private suspend fun observeRejectedInvitations() {
        identityInvitationRepository
            .observeDeclinedOutgoingContactIds()
            .collect { contactIds ->
                contactIds.forEach { contactId ->
                    outgoingMessageProcessor
                        .discardWaitingForAuthorization(contactId)
                        .onFailure { error ->
                            logger.warn(error) {
                                "Queued direct messages could not be discarded for contactId=$contactId"
                            }
                        }
                }
            }
    }
}
