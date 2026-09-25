package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityImportTrust
import com.cbgm.sparrow.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.device.AddDeviceContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.device.AddDeviceContactResult
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.DeviceContactWriterRepository
import com.cbgm.sparrow.feature.identity.domain.model.RemoteIdentityOrigin
import com.cbgm.sparrow.feature.identity.domain.usecase.CancelIdentityExchangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.FindRemoteIdentityPeerIdUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ImportRemoteIdentityUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.StartManualIdentityExchangeUseCase

class ImportContactUseCase(
    private val contactRepository: ContactRepository,
    private val cancelIdentityExchange: CancelIdentityExchangeUseCase,
    private val importRemoteIdentity: ImportRemoteIdentityUseCase,
    private val findRemoteIdentityPeerId: FindRemoteIdentityPeerIdUseCase,
    private val startManualIdentityExchange: StartManualIdentityExchangeUseCase,
    private val deviceContactWriterRepository: DeviceContactWriterRepository,
    private val getContact: GetContactUseCase
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
                    phoneNumber = request.phoneNumber?.trim()?.takeIf { it.isNotEmpty() },
                    matchedIdentityContactId =
                        if (request.contactId == null) {
                            findRemoteIdentityPeerId(request.signingPublicKey).getOrThrow()
                        } else {
                            null
                        }
                )

            val persistedContact =
                contactRepository
                    .upsertImportedContact(normalizedRequest)
                    .getOrThrow()

            importRemoteIdentity(
                peerId = persistedContact.id,
                encryptionPublicKey = normalizedRequest.encryptionPublicKey,
                signingPublicKey = normalizedRequest.signingPublicKey,
                origin = if (normalizedRequest.identityImportTrust == IdentityImportTrust.VERIFIED_IN_PERSON) {
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
