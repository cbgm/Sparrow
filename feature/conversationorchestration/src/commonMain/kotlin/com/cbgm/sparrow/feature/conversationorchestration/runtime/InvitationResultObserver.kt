package com.cbgm.sparrow.feature.conversationorchestration.runtime

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.BlockContactUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.port.DirectConversationPort
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResultAction
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveInvitationResultsUseCase
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class InvitationResultObserver(
    private val observeInvitationResults: ObserveInvitationResultsUseCase,
    private val contactBlocklistRepository: ContactBlocklistRepository,
    private val blockContact: BlockContactUseCase,
    private val directConversationPort: DirectConversationPort
) {
    private val logger = SparrowLog.withTag("InvitationResultObserver")

    suspend fun run(): Unit =
        coroutineScope {
            launch { observeAccepted() }
            launch { observeOutgoingDeclined() }
            launch { observeBlockRequested() }
            launch { directConversationPort.runPendingAuthorizationCleanup() }
        }

    private suspend fun observeAccepted() {
        observeInvitationResults()
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
                    directConversationPort
                        .activateAuthorizedConversation(peerId)
                        .onFailure { error ->
                            logger.warn(error) {
                                "Accepted invitation reaction failed for peerId=$peerId"
                            }
                        }
                }
            }
    }

    private suspend fun observeOutgoingDeclined() {
        observeInvitationResults()
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
                    directConversationPort
                        .discardPendingAuthorizationMessages(peerId)
                        .onFailure { error ->
                            logger.warn(error) {
                                "Declined invitation reaction failed for peerId=$peerId"
                            }
                        }
                }
            }
    }

    private suspend fun observeBlockRequested() {
        observeInvitationResults()
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
