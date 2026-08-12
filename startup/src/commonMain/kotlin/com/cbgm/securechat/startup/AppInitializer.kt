package com.cbgm.securechat.startup

import com.cbgm.securechat.feature.identity.domain.model.IdentityStatus
import com.cbgm.securechat.feature.identity.domain.usecase.GetIdentityStatus
import com.cbgm.securechat.feature.identity.domain.usecase.RecoverIncompleteIdentity

class AppInitializer(
    private val getIdentityStatus: GetIdentityStatus,
    private val recoverIncompleteIdentity: RecoverIncompleteIdentity
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
