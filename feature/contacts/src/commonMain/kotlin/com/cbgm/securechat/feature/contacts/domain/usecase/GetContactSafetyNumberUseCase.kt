package com.cbgm.securechat.feature.contacts.domain.usecase

import com.cbgm.securechat.core.crypto.model.PublicIdentityKeySet
import com.cbgm.securechat.core.crypto.safety.SafetyNumber
import com.cbgm.securechat.core.crypto.safety.SafetyNumberGenerator
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository

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
                contact.secureChatIdentity ?: error("Contact has no SecureChat identity")

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
