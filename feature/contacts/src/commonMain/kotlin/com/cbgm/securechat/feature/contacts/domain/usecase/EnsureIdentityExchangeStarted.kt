package com.cbgm.securechat.feature.contacts.domain.usecase

import com.cbgm.securechat.feature.contacts.domain.identity.IdentityExchangeStarter

class EnsureIdentityExchangeStarted(
    private val identityExchangeStarter: IdentityExchangeStarter
) {
    suspend operator fun invoke(contactId: String): Result<Unit> =
        identityExchangeStarter.ensureStarted(contactId)
}
