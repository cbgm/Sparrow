package com.cbgm.securechat.startup

import com.cbgm.securechat.feature.identity.domain.model.IdentityStatus
import com.cbgm.securechat.feature.identity.domain.usecase.GetIdentityStatusUseCase
import com.cbgm.securechat.feature.identity.domain.usecase.RecoverIncompleteIdentityUseCase

class AppInitializer(
    private val getIdentityStatus: GetIdentityStatusUseCase,
    private val recoverIncompleteIdentity: RecoverIncompleteIdentityUseCase
) {
    suspend fun initialize(): Result<AppInitializationResult> =
        runCatching {
            val identityStatus = getIdentityStatus().getOrThrow()
            if (identityStatus == IdentityStatus.INCOMPLETE) {
                recoverIncompleteIdentity().getOrThrow()
            }

            AppInitializationResult(
                identityReady = identityStatus == IdentityStatus.READY
            )
        }
}
