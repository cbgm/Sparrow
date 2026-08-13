package com.cbgm.securechat.feature.contacts.data.repository

import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.data.database.entity.ContactEntity
import com.cbgm.securechat.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.securechat.feature.contacts.data.mapper.toDomain
import com.cbgm.securechat.feature.contacts.data.merge.ContactMergeService
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.domain.model.IdentityImportTrust
import com.cbgm.securechat.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.securechat.feature.contacts.domain.model.ImportDeviceContactRequest
import com.cbgm.securechat.feature.contacts.domain.model.ImportDevicePhoneNumber
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.model.RemoteIdentityOrigin
import com.cbgm.securechat.feature.contacts.domain.model.device.AddDeviceContactRequest
import com.cbgm.securechat.feature.contacts.domain.model.device.AddDeviceContactResult
import com.cbgm.securechat.feature.contacts.domain.repository.ContactKeyExchangeRepository
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import com.cbgm.securechat.feature.contacts.domain.repository.DeviceContactWriterRepository
import com.cbgm.securechat.feature.contacts.domain.repository.IdentityExchangeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ContactRepositoryImpl(
    private val contactDao: ContactDao,
    private val mergeService: ContactMergeService,
    private val contactKeyExchangeRepository: ContactKeyExchangeRepository,
    private val identityExchangeRepository: IdentityExchangeRepository,
    private val phoneNumberNormalizer: PhoneNumberNormalizer,
    private val deviceContactWriterRepository: DeviceContactWriterRepository
) : ContactRepository {
    private val logger = SecureChatLog.withTag("ContactRepositoryImpl")

    override suspend fun importContact(request: ImportContactRequest): Result<Contact> =
        runCatching {
            require(request.encryptionPublicKey.isNotEmpty()) {
                "Encryption public key must not be empty"
            }

            require(request.signingPublicKey.isNotEmpty()) {
                "Signing public key must not be empty"
            }

            val requestedContactId =
                request.contactId
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }

            val normalizedDisplayName =
                request.displayName
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }

            val normalizedPhoneNumber =
                request.phoneNumber
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }

            val now = SystemClock.nowEpochMilliseconds()

            val resolvedContact =
                resolveContactForSecureIdentityImport(
                    requestedContactId = requestedContactId,
                    signingPublicKey = request.signingPublicKey,
                    normalizedPhoneNumber = normalizedPhoneNumber
                )

            val contactId = resolvedContact.contactId

            if (resolvedContact.isNewContact) {
                contactDao.upsertContact(
                    contact =
                        ContactEntity(
                            id = contactId,
                            displayName = normalizedDisplayName,
                            deviceContactId = null,
                            deviceContactLinkStatus = DeviceContactLinkStatus.NOT_LINKED.name,
                            preferredPhoneNumberId = null,
                            createdAtEpochMilliseconds = now,
                            updatedAtEpochMilliseconds = now
                        )
                )
            } else {
                val existingContact =
                    contactDao.findById(
                        contactId = contactId
                    ) ?: error("Matched contact could not be loaded")

                contactDao.upsertContact(
                    contact =
                        existingContact.contact.copy(
                            displayName =
                                normalizedDisplayName
                                    ?: existingContact.contact.displayName,
                            updatedAtEpochMilliseconds = now
                        )
                )
            }

            val contactBeforePhoneNumberUpdate =
                contactDao.findById(
                    contactId = contactId
                ) ?: error(
                    "Contact could not be loaded after saving"
                )

            val preferredPhoneNumberId =
                if (normalizedPhoneNumber == null) {
                    contactBeforePhoneNumberUpdate.contact.preferredPhoneNumberId
                } else {
                    ensurePhoneNumberExists(
                        contactId = contactId,
                        existingPhoneNumbers =
                            contactBeforePhoneNumberUpdate.phoneNumbers,
                        value = normalizedPhoneNumber,
                        type = ContactPhoneNumberType.MOBILE,
                        label = null,
                        now = now
                    )
                }

            val contactAfterPhoneNumber =
                contactDao.findById(
                    contactId = contactId
                ) ?: error(
                    "Contact could not be loaded after saving phone number"
                )

            contactDao.upsertContact(
                contact =
                    contactAfterPhoneNumber.contact.copy(
                        preferredPhoneNumberId = preferredPhoneNumberId,
                        updatedAtEpochMilliseconds = now
                    )
            )

            contactKeyExchangeRepository
                .storeRemoteIdentity(
                    contactId = contactId,
                    encryptionPublicKey = request.encryptionPublicKey,
                    signingPublicKey = request.signingPublicKey,
                    origin =
                        when (request.identityImportTrust) {
                            IdentityImportTrust.UNVERIFIED -> RemoteIdentityOrigin.LOCAL_IMPORT
                            IdentityImportTrust.VERIFIED_IN_PERSON -> RemoteIdentityOrigin.TRUSTED_QR_IMPORT
                        }
                ).getOrThrow()

            identityExchangeRepository
                .startManualExchange(
                    contactId = contactId
                ).getOrThrow()

            val importedContact =
                loadContactOrThrow(
                    contactId = contactId,
                    message = "Imported contact could not be loaded"
                )

            if (normalizedPhoneNumber != null) {
                when (
                    val result =
                        deviceContactWriterRepository.addIfNotExists(
                            request =
                                AddDeviceContactRequest(
                                    displayName =
                                        normalizedDisplayName
                                            ?: importedContact.displayName,
                                    phoneNumber = normalizedPhoneNumber
                                )
                        )
                ) {
                    AddDeviceContactResult.Added -> {
                        logger.debug { "Device contact created for imported contact: contactId=$contactId" }
                    }
                    AddDeviceContactResult.AlreadyExists -> {
                        logger.debug { "Device contact already exists for imported contact: contactId=$contactId" }
                    }
                    AddDeviceContactResult.PermissionDenied -> {
                        logger.warn { "Device contact was not created because write permission is missing" }
                    }
                    AddDeviceContactResult.InvalidPhoneNumber -> {
                        logger.warn { "Device contact was not created because the phone number is invalid" }
                    }

                    is AddDeviceContactResult.Failure -> {
                        logger.error(result.throwable) { "Device contact creation failed" }
                    }
                }
            }

            importedContact
        }

    override suspend fun importDeviceContact(request: ImportDeviceContactRequest): Result<Contact> =
        runCatching {
            require(request.deviceContactId.isNotBlank()) {
                "Device contact ID must not be blank"
            }

            val normalizedDisplayName =
                request.displayName
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }

            val normalizedPhoneNumbers =
                normalizeDevicePhoneNumbers(
                    phoneNumbers = request.phoneNumbers
                )

            require(normalizedPhoneNumbers.isNotEmpty()) {
                "Device contact must contain at least one phone number"
            }

            val now = SystemClock.nowEpochMilliseconds()

            val mergeResult =
                mergeService.findOrCreateForDeviceContact(
                    deviceContactId = request.deviceContactId,
                    phoneNumbers = normalizedPhoneNumbers
                )

            val contactId = mergeResult.contactId

            if (mergeResult.isNewContact) {
                contactDao.upsertContact(
                    contact =
                        ContactEntity(
                            id = contactId,
                            displayName = normalizedDisplayName,
                            deviceContactId = request.deviceContactId,
                            deviceContactLinkStatus =
                                DeviceContactLinkStatus.LINKED.name,
                            preferredPhoneNumberId = null,
                            createdAtEpochMilliseconds = now,
                            updatedAtEpochMilliseconds = now
                        )
                )
            }

            val preferredPhoneNumberId =
                replaceDevicePhoneNumbers(
                    contactId = contactId,
                    phoneNumbers = normalizedPhoneNumbers,
                    now = now
                )

            val current =
                contactDao.findById(
                    contactId = contactId
                ) ?: error("Device contact could not be loaded")

            contactDao.upsertContact(
                contact =
                    current.contact.copy(
                        displayName =
                            normalizedDisplayName
                                ?: current.contact.displayName,
                        deviceContactId = request.deviceContactId,
                        deviceContactLinkStatus =
                            DeviceContactLinkStatus.LINKED.name,
                        preferredPhoneNumberId = preferredPhoneNumberId,
                        updatedAtEpochMilliseconds = now
                    )
            )

            loadContactOrThrow(
                contactId = contactId,
                message = "Imported device contact could not be loaded"
            )
        }

    override suspend fun getContact(contactId: String): Result<Contact?> =
        runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            contactDao
                .findById(
                    contactId = contactId
                )?.toDomain()
        }

    override suspend fun findBySigningPublicKey(signingPublicKey: ByteArray): Result<Contact?> =
        runCatching {
            require(signingPublicKey.isNotEmpty()) {
                "Signing public key must not be empty"
            }

            contactDao
                .findBySigningPublicKey(
                    signingPublicKey = signingPublicKey
                )?.toDomain()
        }

    override suspend fun findOrCreateByPhoneNumber(phoneNumber: String): Result<Contact> =
        runCatching {
            val value =
                phoneNumber
                    .trim()
                    .takeIf { it.isNotEmpty() }
                    ?: error("Phone number must not be blank")
            val normalizedValue =
                phoneNumberNormalizer
                    .normalize(value)
                    .getOrThrow()

            contactDao
                .findByNormalizedPhoneNumber(normalizedPhoneNumber = normalizedValue)
                ?.toDomain()
                ?.let { contact -> return@runCatching contact }

            val now = SystemClock.nowEpochMilliseconds()
            val contactId = IdGenerator.generate()
            val phoneNumberId = IdGenerator.generate()

            contactDao.upsertContact(
                contact =
                    ContactEntity(
                        id = contactId,
                        displayName = null,
                        deviceContactId = null,
                        deviceContactLinkStatus = DeviceContactLinkStatus.NOT_LINKED.name,
                        preferredPhoneNumberId = phoneNumberId,
                        createdAtEpochMilliseconds = now,
                        updatedAtEpochMilliseconds = now
                    )
            )
            contactDao.upsertPhoneNumbers(
                phoneNumbers =
                    listOf(
                        ContactPhoneNumberEntity(
                            id = phoneNumberId,
                            contactId = contactId,
                            value = value,
                            normalizedValue = normalizedValue,
                            type = ContactPhoneNumberType.MOBILE.name,
                            label = null,
                            updatedAtEpochMilliseconds = now
                        )
                    )
            )

            loadContactOrThrow(
                contactId = contactId,
                message = "Blocked phone number contact could not be loaded"
            )
        }

    override fun observeContacts(): Flow<List<Contact>> =
        contactDao.observeAll().map { contacts ->
            contacts.map { contact ->
                contact.toDomain()
            }
        }

    override suspend fun updateContactDetails(
        contactId: String,
        displayName: String?,
        phoneNumber: String?
    ): Result<Contact> =
        runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            val existing =
                contactDao.findById(
                    contactId = contactId
                ) ?: error("Contact not found: $contactId")

            val normalizedDisplayName =
                displayName
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }

            val normalizedPhoneNumber =
                phoneNumber
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }

            val now = SystemClock.nowEpochMilliseconds()

            val preferredPhoneNumberId =
                if (normalizedPhoneNumber == null) {
                    existing.contact.preferredPhoneNumberId
                } else {
                    ensurePhoneNumberExists(
                        contactId = contactId,
                        existingPhoneNumbers =
                            existing.phoneNumbers,
                        value = normalizedPhoneNumber,
                        type = ContactPhoneNumberType.MOBILE,
                        label = null,
                        now = now
                    )
                }

            contactDao.upsertContact(
                contact =
                    existing.contact.copy(
                        displayName = normalizedDisplayName,
                        preferredPhoneNumberId =
                        preferredPhoneNumberId,
                        updatedAtEpochMilliseconds = now
                    )
            )

            loadContactOrThrow(
                contactId = contactId,
                message = "Updated contact could not be loaded"
            )
        }

    override suspend fun markVerified(contactId: String): Result<Contact> =
        runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            val existing =
                contactDao.findById(
                    contactId = contactId
                ) ?: error("Contact not found: $contactId")

            val publicIdentity =
                existing.publicIdentity
                    ?: error("Contact has no SecureChat identity")

            check(
                publicIdentity.keyExchangeStatus ==
                    KeyExchangeStatus.MUTUAL.name
            ) {
                "Contact identity cannot be verified before mutual key exchange"
            }

            val updatedRows =
                contactDao.updateVerificationStatusIfKeysMatch(
                    contactId = contactId,
                    expectedEncryptionPublicKey = publicIdentity.encryptionPublicKey,
                    expectedSigningPublicKey = publicIdentity.signingPublicKey,
                    verificationStatus = ContactVerificationStatus.VERIFIED.name,
                    updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                )

            check(updatedRows == 1) {
                "Contact identity changed before verification was saved"
            }

            loadContactOrThrow(
                contactId = contactId,
                message = "Verified contact could not be loaded"
            )
        }

    override suspend fun markKeyExchangeMutual(contactId: String): Result<Contact> =
        runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            val existing =
                contactDao.findById(
                    contactId = contactId
                ) ?: error("Contact not found: $contactId")

            val publicIdentity =
                existing.publicIdentity
                    ?: error("Contact has no SecureChat identity")

            contactKeyExchangeRepository
                .markMutual(
                    contactId = contactId,
                    expectedRemoteEncryptionPublicKey =
                        publicIdentity.encryptionPublicKey,
                    expectedRemoteSigningPublicKey =
                        publicIdentity.signingPublicKey
                ).getOrThrow()

            loadContactOrThrow(
                contactId = contactId,
                message = "Contact could not be loaded after key exchange"
            )
        }

    override suspend fun resetKeyExchange(contactId: String): Result<Contact> =
        runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            val existing =
                contactDao.findById(
                    contactId = contactId
                ) ?: error("Contact not found: $contactId")

            existing.publicIdentity
                ?: error("Contact has no SecureChat identity")

            val now = SystemClock.nowEpochMilliseconds()

            contactDao.updateKeyExchangeStatus(
                contactId = contactId,
                status = KeyExchangeStatus.ONE_WAY.name,
                updatedAt = now
            )

            contactDao.updateVerificationStatus(
                contactId = contactId,
                status = ContactVerificationStatus.UNVERIFIED.name,
                updatedAt = now
            )

            contactDao.clearVerifiedByContact(
                contactId = contactId,
                updatedAt = now
            )

            loadContactOrThrow(
                contactId = contactId,
                message = "Contact could not be loaded after reset"
            )
        }

    override suspend fun updateDeviceContactLinkStatus(
        deviceContactId: String,
        status: DeviceContactLinkStatus
    ): Result<Contact?> {
        return runCatching {
            require(deviceContactId.isNotBlank()) {
                "Device contact ID must not be blank"
            }

            val existing =
                contactDao.findByDeviceContactId(
                    deviceContactId = deviceContactId
                ) ?: return@runCatching null

            contactDao.upsertContact(
                contact =
                    existing.contact.copy(
                        deviceContactLinkStatus = status.name,
                        updatedAtEpochMilliseconds =
                            SystemClock.nowEpochMilliseconds()
                    )
            )

            contactDao
                .findById(
                    contactId = existing.contact.id
                )?.toDomain()
        }
    }

    private suspend fun resolveContactForSecureIdentityImport(
        requestedContactId: String?,
        signingPublicKey: ByteArray,
        normalizedPhoneNumber: String?
    ): ResolvedContactImport {
        if (requestedContactId != null) {
            val selectedContact =
                contactDao.findById(
                    contactId = requestedContactId
                ) ?: error(
                    "Selected contact was not found: $requestedContactId"
                )

            return ResolvedContactImport(
                contactId = selectedContact.contact.id,
                isNewContact = false
            )
        }

        val mergeResult =
            mergeService.findOrCreateForSecureChatIdentity(
                signingPublicKey = signingPublicKey,
                phoneNumber = normalizedPhoneNumber
            )

        if (!mergeResult.isNewContact) {
            contactDao.findById(
                contactId = mergeResult.contactId
            ) ?: error(
                "Matched contact could not be loaded"
            )
        }

        return ResolvedContactImport(
            contactId = mergeResult.contactId,
            isNewContact = mergeResult.isNewContact
        )
    }

    private suspend fun replaceDevicePhoneNumbers(
        contactId: String,
        phoneNumbers: List<ImportDevicePhoneNumber>,
        now: Long
    ): String? {
        contactDao.deletePhoneNumbersForContact(
            contactId = contactId
        )

        if (phoneNumbers.isEmpty()) {
            return null
        }

        val entities =
            phoneNumbers.map { phoneNumber ->
                ContactPhoneNumberEntity(
                    id = IdGenerator.generate(),
                    contactId = contactId,
                    value = phoneNumber.value,
                    normalizedValue =
                        phoneNumberNormalizer
                            .normalize(phoneNumber.value)
                            .getOrThrow(),
                    type = phoneNumber.type.name,
                    label = phoneNumber.label,
                    updatedAtEpochMilliseconds = now
                )
            }

        contactDao.upsertPhoneNumbers(
            phoneNumbers = entities
        )

        return entities
            .minByOrNull { entity ->
                phoneNumberPriority(type = entity.type)
            }?.id
    }

    private suspend fun ensurePhoneNumberExists(
        contactId: String,
        existingPhoneNumbers: List<ContactPhoneNumberEntity>,
        value: String,
        type: ContactPhoneNumberType,
        label: String?,
        now: Long
    ): String {
        val normalizedValue =
            phoneNumberNormalizer
                .normalize(value)
                .getOrThrow()

        val existing =
            existingPhoneNumbers.firstOrNull { phoneNumber ->
                phoneNumber.normalizedValue == normalizedValue
            }

        if (existing != null) {
            return existing.id
        }

        val entity =
            ContactPhoneNumberEntity(
                id = IdGenerator.generate(),
                contactId = contactId,
                value = value,
                normalizedValue = normalizedValue,
                type = type.name,
                label = label,
                updatedAtEpochMilliseconds = now
            )

        contactDao.upsertPhoneNumbers(
            phoneNumbers = listOf(entity)
        )

        return entity.id
    }

    private fun normalizeDevicePhoneNumbers(phoneNumbers: List<ImportDevicePhoneNumber>): List<ImportDevicePhoneNumber> {
        return phoneNumbers
            .mapNotNull { phoneNumber ->
                val normalizedValue =
                    phoneNumber.value
                        .trim()
                        .takeIf { it.isNotEmpty() }
                        ?: return@mapNotNull null

                phoneNumber.copy(
                    value = normalizedValue,
                    label =
                        phoneNumber.label
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                )
            }.distinctBy { phoneNumber ->
                phoneNumber.value to phoneNumber.type
            }
    }

    private fun phoneNumberPriority(type: String): Int =
        when (type) {
            ContactPhoneNumberType.MOBILE.name -> 0
            ContactPhoneNumberType.WORK_MOBILE.name -> 1
            ContactPhoneNumberType.MAIN.name -> 2
            ContactPhoneNumberType.HOME.name -> 3
            ContactPhoneNumberType.WORK.name -> 4
            ContactPhoneNumberType.CUSTOM.name -> 5
            ContactPhoneNumberType.OTHER.name -> 6
            else -> Int.MAX_VALUE
        }

    private suspend fun loadContactOrThrow(
        contactId: String,
        message: String
    ): Contact =
        contactDao
            .findById(
                contactId = contactId
            )?.toDomain() ?: error(message)

    private data class ResolvedContactImport(
        val contactId: String,
        val isNewContact: Boolean
    )
}
