package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.crypto.SafetyNumber
import com.cbgm.sparrow.feature.contacts.crypto.SafetyNumberGenerator
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import com.cbgm.sparrow.protocol.identity.LocalPublicIdentityProvider

class GetContactSafetyNumberUseCase(
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val contactRepository: ContactRepository,
    private val getRemoteIdentity: GetRemoteIdentityUseCase,
    private val safetyNumberGenerator: SafetyNumberGenerator
) {
    suspend operator fun invoke(contactId: String): Result<SafetyNumber> =
        runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()

            val contact =
                contactRepository.getContact(contactId = contactId).getOrThrow()
                    ?: error("Contact was not found")

            val remoteIdentity =
                getRemoteIdentity(contact.id).getOrThrow() ?: error("Contact has no Sparrow identity")

            safetyNumberGenerator
                .generate(
                    firstIdentity =
                        PublicIdentity(
                            signingPublicKey = localIdentity.signingPublicKey,
                            encryptionPublicKey = localIdentity.encryptionPublicKey
                        ),
                    secondIdentity =
                        PublicIdentity(
                            signingPublicKey = remoteIdentity.signingPublicKey,
                            encryptionPublicKey = remoteIdentity.encryptionPublicKey
                        )
                ).getOrThrow()
        }
}
