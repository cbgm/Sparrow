package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentitySetupModeRepository

/**
 * Retransmit an already initiated manual key exchange when an earlier identity
 * packet or acknowledgement was consumed before the other device imported us.
 * Never imports, trusts, or marks any identity as mutual on its own.
 */
class RecoverManualIdentityExchangeUseCase(
    private val setupModeRepository: DirectIdentitySetupModeRepository,
    private val getRemoteIdentity: GetRemoteIdentityUseCase,
    private val startManualIdentityExchange: StartManualIdentityExchangeUseCase
) {
    suspend operator fun invoke(peerId: String): Result<Boolean> = runCatching {
        require(peerId.isNotBlank()) { "Peer ID must not be blank" }
        if (setupModeRepository.getMode() != DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING) {
            return@runCatching false
        }
        val identity = getRemoteIdentity(peerId).getOrThrow()
        if (identity?.locallyImported != true || identity.keyExchangeStatus == KeyExchangeStatus.MUTUAL) {
            return@runCatching false
        }
        // An explicit import already started this exchange. A retry only sends our
        // public identity; receiving a matching packet or a verified acknowledgement
        // is still required before the status can become MUTUAL.
        startManualIdentityExchange(peerId).getOrThrow()
        true
    }
}
