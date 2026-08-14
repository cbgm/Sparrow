package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.crypto.model.PublicIdentityKeySet
import com.cbgm.sparrow.core.crypto.safety.SafetyNumber
import com.cbgm.sparrow.core.crypto.safety.SafetyNumberGenerator
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository

class GetContactSafetyNumberUseCase(
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val contactRepository: ContactRepository,
    private val safetyNumberGenerator: SafetyNumberGenerator
) {
    suspend fun invoke(contactId: String): Result<SafetyNumber> =
        runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()

            val contact =
                contactRepository.getContact(contactId = contactId).getOrThrow()
                    ?: error("Contact was not found")

            val remoteIdentity =
                contact.sparrowIdentity ?: error("Contact has no Sparrow identity")

            safetyNumberGenerator
                .generate(
                    firstIdentity =
                        PublicIdentityKeySet(
                            signingPublicKey = localIdentity.signingPublicKey,
                            encryptionPublicKey = localIdentity.encryptionPublicKey
                        ),
                    secondIdentity =
                        PublicIdentityKeySet(
                            signingPublicKey = remoteIdentity.signingPublicKey,
                            encryptionPublicKey = remoteIdentity.encryptionPublicKey
                        )
                ).getOrThrow()
        }
}
