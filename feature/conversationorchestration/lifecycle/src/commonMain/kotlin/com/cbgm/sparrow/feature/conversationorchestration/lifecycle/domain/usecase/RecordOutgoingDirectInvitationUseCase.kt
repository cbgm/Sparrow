package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchange
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleRecord
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.usecase.RecordPendingInvitationUseCase

/**
 * Only Invite persists user-facing contact metadata. The Identity exchange stores the remote
 * cryptographic identity, never a local Contacts label.
 */
class RecordOutgoingDirectInvitationUseCase(
    private val recordPendingInvitation: RecordPendingInvitationUseCase
) {
    suspend operator fun invoke(exchange: IdentityExchange, peerDisplayName: String?): Result<Unit> =
        recordPendingInvitation(
            InvitationLifecycleRecord(
                invitationId = exchange.exchangeId,
                payloadType = InvitationPayloadType.DIRECT,
                payloadId = exchange.peerId,
                peerId = exchange.peerId,
                direction =
                    when (exchange.direction) {
                        IdentityExchangeDirection.INCOMING -> InvitationDirection.INCOMING
                        IdentityExchangeDirection.OUTGOING -> InvitationDirection.OUTGOING
                    },
                createdAtEpochMilliseconds = exchange.createdAtEpochMilliseconds,
                expiresAtEpochMilliseconds = exchange.expiresAtEpochMilliseconds,
                updatedAtEpochMilliseconds =
                    maxOf(exchange.createdAtEpochMilliseconds, exchange.updatedAtEpochMilliseconds),
                peerDisplayName = peerDisplayName
            )
        )
}
