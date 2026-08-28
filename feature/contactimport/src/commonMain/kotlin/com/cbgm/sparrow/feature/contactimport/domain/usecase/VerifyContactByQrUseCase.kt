package com.cbgm.sparrow.feature.contactimport.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityImportTrust
import com.cbgm.sparrow.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactVerificationRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityExchangeRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityInvitationRepository
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityShareRepository

class VerifyContactByQrUseCase(
    private val identityShareRepository: IdentityShareRepository,
    private val contactRepository: ContactRepository,
    private val identityInvitationRepository: IdentityInvitationRepository,
    private val identityExchangeRepository: IdentityExchangeRepository,
    private val contactVerificationRepository: ContactVerificationRepository
) {
    suspend operator fun invoke(
        contactId: String,
        encodedIdentity: String
    ): Result<Contact> =
        runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            val sharedIdentity =
                identityShareRepository
                    .decode(encodedIdentity)
                    .getOrThrow()

            val persistedContact =
                contactRepository
                    .importContact(
                        ImportContactRequest(
                            contactId = contactId,
                            displayName = null,
                            phoneNumber = null,
                            encryptionPublicKey = sharedIdentity.encryptionPublicKey.copyOf(),
                            signingPublicKey = sharedIdentity.signingPublicKey.copyOf(),
                            identityImportTrust = IdentityImportTrust.VERIFIED_IN_PERSON
                        )
                    ).getOrThrow()

            identityInvitationRepository
                .cancelForManualSetup(persistedContact.id)
                .getOrThrow()

            identityExchangeRepository
                .startManualExchange(persistedContact.id)
                .getOrThrow()

            contactVerificationRepository
                .verify(persistedContact.id)
                .getOrThrow()

            contactRepository
                .getContact(persistedContact.id)
                .getOrThrow()
                ?: error("Contact not found: ${persistedContact.id}")
        }
}
