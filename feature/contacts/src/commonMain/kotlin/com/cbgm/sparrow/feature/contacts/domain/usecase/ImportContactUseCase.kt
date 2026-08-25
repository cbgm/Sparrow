package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.device.AddDeviceContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.device.AddDeviceContactResult
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.DeviceContactWriterRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityExchangeRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityInvitationRepository

class ImportContactUseCase(
    private val contactRepository: ContactRepository,
    private val identityInvitationRepository: IdentityInvitationRepository,
    private val identityExchangeRepository: IdentityExchangeRepository,
    private val deviceContactWriterRepository: DeviceContactWriterRepository
) {
    private val logger = SparrowLog.withTag("ImportContactUseCase")

    suspend operator fun invoke(request: ImportContactRequest): Result<Contact> =
        runCatching {
            require(request.encryptionPublicKey.isNotEmpty()) {
                "Encryption public key must not be empty"
            }
            require(request.signingPublicKey.isNotEmpty()) {
                "Signing public key must not be empty"
            }

            val normalizedRequest =
                request.copy(
                    displayName = request.displayName?.trim()?.takeIf { it.isNotEmpty() },
                    phoneNumber = request.phoneNumber?.trim()?.takeIf { it.isNotEmpty() }
                )

            val persistedContact =
                contactRepository
                    .importContact(normalizedRequest)
                    .getOrThrow()

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

            normalizedRequest.phoneNumber?.let { phoneNumber ->
                when (
                    val result =
                        deviceContactWriterRepository.addIfNotExists(
                            AddDeviceContactRequest(
                                displayName = normalizedRequest.displayName ?: importedContact.displayName,
                                phoneNumber = phoneNumber
                            )
                        )
                ) {
                    AddDeviceContactResult.Added ->
                        logger.debug { "Device contact created for imported contact: contactId=${importedContact.id}" }

                    AddDeviceContactResult.AlreadyExists ->
                        logger.debug { "Device contact already exists for imported contact: contactId=${importedContact.id}" }

                    AddDeviceContactResult.PermissionDenied ->
                        logger.warn { "Device contact was not created because write permission is missing" }

                    AddDeviceContactResult.InvalidPhoneNumber ->
                        logger.warn { "Device contact was not created because the phone number is invalid" }

                    is AddDeviceContactResult.Failure ->
                        logger.error(result.throwable) { "Device contact creation failed" }
                }
            }

            importedContact
        }
}
