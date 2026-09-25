package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository

/** Persists a verified remote exchange decline without depending on the wire packet type. */
class RecordRemoteIdentityDeclineUseCase(
    private val repository: IdentityExchangeRepository
) {
    suspend operator fun invoke(
        exchangeId: String,
        peerId: String,
        inviteChallenge: ByteArray,
        remoteSigningPublicKey: ByteArray,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> = repository.recordRemoteDecline(
        exchangeId = exchangeId,
        peerId = peerId,
        inviteChallenge = inviteChallenge,
        remoteSigningPublicKey = remoteSigningPublicKey,
        receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
    )
}
