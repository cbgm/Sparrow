package com.cbgm.sparrow.feature.contacts.domain.usecase.identity

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityPeerRepository

/**
 * Links an accepted Sparrow contact to the phone book contact whose number it claimed. The user chose this by
 * accepting the invitation; a phone book contact that gained any other use in the meantime, or that the user
 * blocked, is left alone, so linking never carries a contact out of the blocklist.
 */
class LinkPhoneContactUseCase(
    private val repository: IdentityPeerRepository,
    private val blocklistRepository: ContactBlocklistRepository
) {
    suspend operator fun invoke(
        peerId: String,
        phoneContactPeerId: String
    ): Result<Unit> =
        safeSuspendCall {
            if (blocklistRepository.isBlocked(phoneContactPeerId).getOrThrow()) return@safeSuspendCall
            repository.linkPhoneContact(peerId, phoneContactPeerId, SystemClock.nowEpochMilliseconds()).getOrThrow()
        }
}
