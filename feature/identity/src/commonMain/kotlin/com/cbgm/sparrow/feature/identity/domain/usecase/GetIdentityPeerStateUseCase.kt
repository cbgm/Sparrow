package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.domain.model.IdentityPeerState
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository
import com.cbgm.sparrow.feature.identity.domain.repository.PendingRemoteIdentityChangeRepository
import kotlinx.coroutines.flow.first

/** An unapproved replacement must not inherit the authorization of the old keys. */
class GetIdentityPeerStateUseCase(
    private val repository: IdentityExchangeRepository,
    private val pendingRemoteIdentityChanges: PendingRemoteIdentityChangeRepository
) {
    suspend operator fun invoke(peerId: String): Result<IdentityPeerState> =
        safeSuspendCall {
            require(peerId.isNotBlank())
            val state = repository.getPeerState(peerId).getOrThrow()
            // Fail closed while the identity-change request remains in the mailbox.
            // In particular, the direct outgoing processor checks this state again
            // when preparing a packet and when releasing messages awaiting auth.
            state.withRecoveryHold(
                hasPendingIdentityChange = pendingRemoteIdentityChanges.observeAll()
                    .first().any { it.peerId == peerId }
            )
        }
}

/** The old exchange remains stored for history; authorization is withheld until review. */
internal fun IdentityPeerState.withRecoveryHold(hasPendingIdentityChange: Boolean): IdentityPeerState =
    if (hasPendingIdentityChange) {
        copy(hasEstablishedExchange = false, hasMutualIdentity = false)
    } else {
        this
    }
