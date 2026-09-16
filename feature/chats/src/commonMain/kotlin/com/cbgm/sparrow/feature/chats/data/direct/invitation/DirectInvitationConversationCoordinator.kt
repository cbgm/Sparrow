package com.cbgm.sparrow.feature.chats.data.direct.invitation

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.chats.data.direct.outgoing.DirectPendingAuthorizationMessageCoordinator
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.HandleAcceptedDirectInvitationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.HandleDeclinedDirectInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.event.InvitationResultStream
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DirectInvitationConversationCoordinator(
    private val invitationResultStream: InvitationResultStream,
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
        invitationResultStream.observeInvitationResults()
            .map { results ->
                results
                    .asSequence()
                    .filter { result -> result.response == InvitationResponse.ACCEPTED }
                    .mapTo(mutableSetOf()) { result -> result.peerId }
            }
            .distinctUntilChanged()
            .collect { peerIds ->
                peerIds.forEach { peerId ->
                    handleAcceptedInvitation(peerId)
                        .onFailure { error ->
                            logger.warn(error) {
                                "Accepted invitation could not be applied for peerId=$peerId"
                            }
                        }
                }
            }
    }

    private suspend fun collectDeclinedInvitations() {
        invitationResultStream.observeInvitationResults()
            .map { results ->
                results
                    .asSequence()
                    .filter { result ->
                        result.direction == InvitationDirection.OUTGOING &&
                            result.response == InvitationResponse.DECLINED
                    }
                    .mapTo(mutableSetOf()) { result -> result.peerId }
            }
            .distinctUntilChanged()
            .collect { peerIds ->
                peerIds.forEach { peerId ->
                    handleDeclinedInvitation(peerId)
                        .onFailure { error ->
                            logger.warn(error) {
                                "Declined invitation could not be applied for peerId=$peerId"
                            }
                        }
                }
            }
    }
}
