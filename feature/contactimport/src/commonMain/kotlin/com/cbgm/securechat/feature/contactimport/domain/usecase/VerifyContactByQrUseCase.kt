package com.cbgm.securechat.feature.contactimport.domain.usecase

import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.IdentityImportTrust
import com.cbgm.securechat.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.securechat.feature.contacts.domain.usecase.ImportContactUseCase
import com.cbgm.securechat.feature.contacts.domain.usecase.VerifyContactUseCase
import com.cbgm.securechat.feature.identity.domain.repository.IdentityShareRepository

class VerifyContactByQrUseCase(
    private val identityShareRepository: IdentityShareRepository,
    private val importContact: ImportContactUseCase,
    private val verifyContact: VerifyContactUseCase
) {
    suspend operator fun invoke(
        contactId: String,
        encodedIdentity: String
    ): Result<Contact> =
        runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            val sharedIdentity = identityShareRepository.decode(encodedIdentity).getOrThrow()

            val importedContact =
                importContact(
                    request =
                        ImportContactRequest(
                            contactId = contactId,
                            displayName = null,
                            phoneNumber = null,
                            encryptionPublicKey = sharedIdentity.encryptionPublicKey.copyOf(),
                            signingPublicKey = sharedIdentity.signingPublicKey.copyOf(),
                            identityImportTrust = IdentityImportTrust.VERIFIED_IN_PERSON
                        )
                ).getOrThrow()

            verifyContact(contactId = importedContact.id).getOrThrow()
        }
}
