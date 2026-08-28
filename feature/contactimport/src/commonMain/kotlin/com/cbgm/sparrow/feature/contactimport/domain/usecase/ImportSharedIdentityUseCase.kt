package com.cbgm.sparrow.feature.contactimport.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityImportTrust
import com.cbgm.sparrow.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.device.AddDeviceContactRequest
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.DeviceContactWriterRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityExchangeRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityInvitationRepository
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityShareRepository

class ImportSharedIdentityUseCase(
    private val identityShareRepository: IdentityShareRepository,
    private val contactRepository: ContactRepository,
    private val identityInvitationRepository: IdentityInvitationRepository,
    private val identityExchangeRepository: IdentityExchangeRepository,
    private val deviceContactWriterRepository: DeviceContactWriterRepository
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
                    .importContact(
                        ImportContactRequest(
                            contactId = contactId,
                            encryptionPublicKey = sharedIdentity.encryptionPublicKey.copyOf(),
                            signingPublicKey = sharedIdentity.signingPublicKey.copyOf(),
                            displayName = displayName,
                            phoneNumber = phoneNumber,
                            identityImportTrust = identityImportTrust
                        )
                    ).getOrThrow()

            identityInvitationRepository
                .cancelForManualSetup(persistedContact.id)
                .getOrThrow()

            identityExchangeRepository
                .startManualExchange(persistedContact.id)
                .getOrThrow()

            val importedContact =
                contactRepository
                    .getContact(persistedContact.id)
                    .getOrThrow()
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
