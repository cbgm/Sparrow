package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityExchangeRepository
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentityExchangeRepository

class EnsureIdentityExchangeStartedUseCase(
    private val modeRepository: DirectIdentitySetupModeRepository,
    private val contactBlocklistRepository: ContactBlocklistRepository,
    private val directIdentityExchangeRepository: DirectIdentityExchangeRepository,
    private val identityExchangeRepository: IdentityExchangeRepository
) {
    suspend operator fun invoke(contactId: String): Result<Unit> =
        runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            when (modeRepository.getMode()) {
                DirectIdentitySetupMode.AUTOMATIC_INVITATION -> {
                    check(!contactBlocklistRepository.isBlocked(contactId)) {
                        "Blocked contacts cannot be invited"
                    }
                    directIdentityExchangeRepository.start(contactId).getOrThrow()
                }

                DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING -> {
                    directIdentityExchangeRepository.cancelForManualSetup(contactId).getOrThrow()
                    identityExchangeRepository.startManualExchange(contactId).getOrThrow()
                }
            }
        }
}
