package com.cbgm.sparrow.feature.identity.data.repository

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.data.datasource.LocalIdentityProfileDataSource
import com.cbgm.sparrow.feature.identity.domain.model.PhoneVerificationSlotStatus
import com.cbgm.sparrow.feature.identity.domain.repository.PhoneVerificationSlotRepository
import com.cbgm.sparrow.protocol.phone.PhoneVerificationSlot
import com.cbgm.sparrow.protocol.phone.PhoneVerificationSlotSource

class PhoneVerificationSlotRepositoryImpl(
    private val slotSource: PhoneVerificationSlotSource,
    private val profileDataSource: LocalIdentityProfileDataSource
) : PhoneVerificationSlotRepository {
    override suspend fun reserve(): Result<PhoneVerificationSlotStatus> =
        safeSuspendCall {
            when (val slot = slotSource.reserve()) {
                is PhoneVerificationSlot.Reserved -> {
                    profileDataSource.keepPhoneVerificationSlot(slot.slot, slot.expiresAtEpochMilliseconds)
                    PhoneVerificationSlotStatus.Reserved(resends = slot.resends)
                }
                is PhoneVerificationSlot.DayFull -> PhoneVerificationSlotStatus.DayFull(slot.retryAtEpochMilliseconds)
                // Without a verifier the number stays unverified anyway; the usual single resend applies.
                PhoneVerificationSlot.NotOffered -> PhoneVerificationSlotStatus.Reserved(resends = DEFAULT_RESENDS)
            }
        }

    private companion object {
        const val DEFAULT_RESENDS = 1
    }
}
