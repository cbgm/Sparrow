package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository

class InvalidateIdentityExchangeUseCase(
    private val repository: IdentityExchangeRepository
) {
    suspend operator fun invoke(
        exchangeId: String,
        peerId: String,
        expectedChallenge: ByteArray,
        expectedSigningPublicKey: ByteArray,
        atEpochMilliseconds: Long
    ): Result<Unit> = repository.invalidateExchange(
        exchangeId = exchangeId,
        peerId = peerId,
        expectedChallenge = expectedChallenge,
        expectedSigningPublicKey = expectedSigningPublicKey,
        atEpochMilliseconds = atEpochMilliseconds
    )
}
