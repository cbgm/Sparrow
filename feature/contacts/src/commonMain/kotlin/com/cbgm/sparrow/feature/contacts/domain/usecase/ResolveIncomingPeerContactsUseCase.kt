package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.feature.contacts.domain.model.IncomingPeerContactCandidate
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository

/** Resolve contact IDs only after the caller has authenticated the packet. Null marks the local peer. */
class ResolveIncomingPeerContactsUseCase(
    private val contacts: ContactRepository,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val phoneNumberNormalizer: PhoneNumberNormalizer
) {
    suspend operator fun invoke(
        candidates: List<IncomingPeerContactCandidate>,
        senderContactId: String,
        senderSigningPublicKey: ByteArray,
        localSigningPublicKey: ByteArray?
    ): Result<List<String?>> = runCatching {
        val localPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrNull()
            ?.let { phoneNumberNormalizer.normalize(it).getOrNull() }
        candidates.map { candidate ->
            val isLocalSigningIdentity = localSigningPublicKey != null &&
                candidate.signingPublicKey.isNotEmpty() &&
                candidate.signingPublicKey.contentEquals(localSigningPublicKey)
            val isLocalPhone = localPhoneNumber != null && candidate.phoneNumber
                ?.let { phoneNumberNormalizer.normalize(it).getOrNull() } == localPhoneNumber
            if (isLocalSigningIdentity || isLocalPhone) {
                null
            } else {
                contacts.resolveAuthenticatedPeerContact(
                    senderContactId = senderContactId.takeIf {
                        candidate.signingPublicKey.contentEquals(senderSigningPublicKey)
                    },
                    phoneNumber = candidate.phoneNumber
                ).getOrThrow()
            }
        }
    }
}
