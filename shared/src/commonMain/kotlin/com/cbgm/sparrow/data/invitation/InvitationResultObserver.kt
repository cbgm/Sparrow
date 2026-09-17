package com.cbgm.sparrow.data.invitation

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.feature.chats.data.direct.outgoing.DirectPendingAuthorizationMessageCoordinator
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ActivateAuthorizedDirectConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.DiscardPendingAuthorizationMessagesUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.BlockContactUseCase
import com.cbgm.sparrow.feature.invite.domain.event.InvitationResultStream
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResultAction
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class InvitationResultObserver(
    private val invitationResultStream: InvitationResultStream,
    private val contactBlocklistRepository: ContactBlocklistRepository,
    private val blockContact: BlockContactUseCase,
    private val activateAuthorizedDirectConversation: ActivateAuthorizedDirectConversationUseCase,
    private val discardPendingAuthorizationMessages: DiscardPendingAuthorizationMessagesUseCase,
    private val pendingAuthorizationMessageCoordinator: DirectPendingAuthorizationMessageCoordinator
) {
    private val logger = SparrowLog.withTag("InvitationResultObserver")

    suspend fun run(): Unit =
        coroutineScope {
            launch { observeAccepted() }
            launch { observeOutgoingDeclined() }
            launch { observeBlockRequested() }
            launch { pendingAuthorizationMessageCoordinator.run() }
        }

    private suspend fun observeAccepted() {
        invitationResultStream.observeInvitationResults()
            .map { results ->
                results
                    .asSequence()
                    .filter { result ->
                        result.payloadType == InvitationPayloadType.DIRECT &&
                            result.response == InvitationResponse.ACCEPTED
                    }
                    .mapTo(mutableSetOf()) { result -> result.peerId }
            }
            .distinctUntilChanged()
            .collect { peerIds ->
                peerIds.forEach { peerId ->
                    activateAuthorizedDirectConversation(peerId)
                        .onFailure { error ->
                            logger.warn(error) {
                                "Accepted invitation reaction failed for peerId=$peerId"
                            }
                        }
                }
            }
    }

    private suspend fun observeOutgoingDeclined() {
        invitationResultStream.observeInvitationResults()
            .map { results ->
                results
                    .asSequence()
                    .filter { result ->
                        result.payloadType == InvitationPayloadType.DIRECT &&
                            result.direction == InvitationDirection.OUTGOING &&
                            result.response == InvitationResponse.DECLINED
                    }
                    .mapTo(mutableSetOf()) { result -> result.peerId }
            }
            .distinctUntilChanged()
            .collect { peerIds ->
                peerIds.forEach { peerId ->
                    discardPendingAuthorizationMessages(peerId)
                        .onFailure { error ->
                            logger.warn(error) {
                                "Declined invitation reaction failed for peerId=$peerId"
                            }
                        }
                }
            }
    }

    private suspend fun observeBlockRequested() {
        invitationResultStream.observeInvitationResults()
            .map { results ->
                results
                    .asSequence()
                    .filter { result ->
                        result.direction == InvitationDirection.INCOMING &&
                            result.response == InvitationResponse.DECLINED &&
                            result.action == InvitationResultAction.BLOCK_PEER
                    }
                    .mapTo(mutableSetOf()) { result -> result.peerId }
            }
            .distinctUntilChanged()
            .collect { peerIds ->
                peerIds.forEach { peerId ->
                    if (contactBlocklistRepository.isBlocked(peerId)) {
                        return@forEach
                    }

                    blockContact(peerId)
                        .onFailure { error ->
                            logger.warn(error) {
                                "Invitation block action could not be applied for peerId=$peerId"
                            }
                        }
                }
            }
    }
}
