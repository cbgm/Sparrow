package com.cbgm.sparrow.feature.contactimport.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityImportTrust
import com.cbgm.sparrow.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.device.AddDeviceContactRequest
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.DeviceContactWriterRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.sparrow.feature.identity.domain.model.RemoteIdentityOrigin
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityShareRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.CancelIdentityExchangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.FindRemoteIdentityPeerIdUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ImportRemoteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.StartManualIdentityExchangeUseCase

class ImportSharedIdentityUseCase(
    private val identityShareRepository: IdentityShareRepository,
    private val contactRepository: ContactRepository,
    private val cancelIdentityExchange: CancelIdentityExchangeUseCase,
    private val importRemoteIdentity: ImportRemoteIdentityUseCase,
    private val findRemoteIdentityPeerId: FindRemoteIdentityPeerIdUseCase,
    private val startManualIdentityExchange: StartManualIdentityExchangeUseCase,
    private val deviceContactWriterRepository: DeviceContactWriterRepository,
    private val getContact: GetContactUseCase
) {
    suspend operator fun invoke(
        encodedIdentity: String,
        contactId: String? = null,
        identityImportTrust: IdentityImportTrust = IdentityImportTrust.UNVERIFIED
    ): Result<Contact> =
        runCatching {
            val sharedIdentity =
                identityShareRepository
                    .decode(encodedIdentity)
                    .getOrThrow()

            val phoneNumber =
                sharedIdentity
                    .contactDetails
                    .phoneNumber
                    .trim()
                    .takeIf { it.isNotEmpty() }
                    ?: error("Shared identity does not contain a phone number")

            val displayName =
                sharedIdentity
                    .contactDetails
                    .displayName
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }

            val persistedContact =
                contactRepository
                    .upsertImportedContact(
                        ImportContactRequest(
                            contactId = contactId,
                            matchedIdentityContactId =
                                if (contactId == null) {
                                    findRemoteIdentityPeerId(sharedIdentity.signingPublicKey).getOrThrow()
                                } else {
                                    null
                                },
                            encryptionPublicKey = sharedIdentity.encryptionPublicKey.copyOf(),
                            signingPublicKey = sharedIdentity.signingPublicKey.copyOf(),
                            displayName = displayName,
                            phoneNumber = phoneNumber,
                            identityImportTrust = identityImportTrust
                        )
                    ).getOrThrow()

            importRemoteIdentity(
                peerId = persistedContact.id,
                encryptionPublicKey = sharedIdentity.encryptionPublicKey,
                signingPublicKey = sharedIdentity.signingPublicKey,
                origin = if (identityImportTrust == IdentityImportTrust.VERIFIED_IN_PERSON) {
                    RemoteIdentityOrigin.TRUSTED_QR_IMPORT
                } else {
                    RemoteIdentityOrigin.LOCAL_IMPORT
                }
            ).getOrThrow()

            cancelIdentityExchange(persistedContact.id).getOrThrow()
            startManualIdentityExchange(persistedContact.id).getOrThrow()

            val importedContact =
                getContact(persistedContact.id).getOrThrow()
                    ?: error("Imported contact could not be loaded")

            deviceContactWriterRepository.addIfNotExists(
                AddDeviceContactRequest(
                    displayName = displayName ?: importedContact.displayName,
                    phoneNumber = phoneNumber
                )
            )

            importedContact
        }
}
