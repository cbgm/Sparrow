package com.cbgm.sparrow.feature.contacts.data.invitation

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.BlockContactUseCase
import com.cbgm.sparrow.feature.invite.domain.event.InvitationResultStream
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResultAction
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class InvitationResultObserver(
    private val invitationResultStream: InvitationResultStream,
    private val contactBlocklistRepository: ContactBlocklistRepository,
    private val blockContact: BlockContactUseCase
) {
    private val logger = SparrowLog.withTag("InvitationResultObserver")

    suspend fun run() {
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
