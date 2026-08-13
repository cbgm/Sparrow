package com.cbgm.securechat.feature.contacts.data.repository

import com.cbgm.securechat.core.security.DirectIdentitySetupMode
import com.cbgm.securechat.core.security.DirectIdentitySetupModeRepository
import com.cbgm.securechat.feature.contacts.data.exchange.ManualIdentityExchange
import com.cbgm.securechat.feature.contacts.domain.repository.IdentityExchangeRepository
import com.cbgm.securechat.feature.contacts.domain.repository.IdentityInvitationRepository

class IdentityExchangeRepositoryImpl(
    private val modeRepository: DirectIdentitySetupModeRepository,
    private val identityInvitationRepository: IdentityInvitationRepository,
    private val manualIdentityExchange: ManualIdentityExchange
) : IdentityExchangeRepository {
    override suspend fun ensureStarted(contactId: String): Result<Unit> =
        when (modeRepository.getMode()) {
            DirectIdentitySetupMode.AUTOMATIC_INVITATION ->
                identityInvitationRepository.start(contactId)
            DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING ->
                startManualExchange(contactId)
        }

    override suspend fun startManualExchange(contactId: String): Result<Unit> = manualIdentityExchange.ensureStarted(contactId)
}
