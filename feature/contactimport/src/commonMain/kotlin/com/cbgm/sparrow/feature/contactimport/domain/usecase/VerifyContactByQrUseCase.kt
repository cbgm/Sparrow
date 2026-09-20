package com.cbgm.sparrow.feature.contactimport.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityImportTrust
import com.cbgm.sparrow.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.sparrow.feature.identity.domain.model.RemoteIdentityOrigin
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityShareRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.CancelIdentityExchangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ImportRemoteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.StartManualIdentityExchangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.VerifyRemoteIdentityUseCase

class VerifyContactByQrUseCase(
    private val identityShareRepository: IdentityShareRepository,
    private val contactRepository: ContactRepository,
    private val cancelIdentityExchange: CancelIdentityExchangeUseCase,
    private val importRemoteIdentity: ImportRemoteIdentityUseCase,
    private val startManualIdentityExchange: StartManualIdentityExchangeUseCase,
    private val verifyRemoteIdentity: VerifyRemoteIdentityUseCase,
    private val getContact: GetContactUseCase
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
                    .upsertImportedContact(
                        ImportContactRequest(
                            contactId = contactId,
                            displayName = null,
                            phoneNumber = null,
                            encryptionPublicKey = sharedIdentity.encryptionPublicKey.copyOf(),
                            signingPublicKey = sharedIdentity.signingPublicKey.copyOf(),
                            identityImportTrust = IdentityImportTrust.VERIFIED_IN_PERSON
                        )
                    ).getOrThrow()

            importRemoteIdentity(
                peerId = persistedContact.id,
                encryptionPublicKey = sharedIdentity.encryptionPublicKey,
                signingPublicKey = sharedIdentity.signingPublicKey,
                origin = RemoteIdentityOrigin.TRUSTED_QR_IMPORT
            ).getOrThrow()

            cancelIdentityExchange(persistedContact.id).getOrThrow()
            startManualIdentityExchange(persistedContact.id).getOrThrow()

            verifyRemoteIdentity(persistedContact.id)
                .getOrThrow()

            getContact(persistedContact.id).getOrThrow()
                ?: error("Contact not found: ${persistedContact.id}")
        }
}
