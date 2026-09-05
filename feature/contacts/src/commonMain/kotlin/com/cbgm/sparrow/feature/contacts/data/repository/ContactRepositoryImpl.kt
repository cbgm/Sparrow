package com.cbgm.sparrow.feature.contacts.data.repository

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.entity.ContactEntity
import com.cbgm.sparrow.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.sparrow.feature.contacts.data.datasource.ContactKeyExchangeDataSource
import com.cbgm.sparrow.feature.contacts.data.mapper.toContact
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.sparrow.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityImportTrust
import com.cbgm.sparrow.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.ImportDeviceContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.ImportDevicePhoneNumber
import com.cbgm.sparrow.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.contacts.domain.model.RemoteIdentityOrigin
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ContactRepositoryImpl(
    private val contactDao: ContactDao,
    private val contactKeyExchangeDataSource: ContactKeyExchangeDataSource,
    private val phoneNumberNormalizer: PhoneNumberNormalizer
) : ContactRepository {
    override suspend fun importContact(request: ImportContactRequest): Result<Contact> =
        safeSuspendCall {
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
                    contactDao.findById(contactId)
                        ?: error("Matched contact could not be loaded")
                contactDao.upsertContact(
                    existingContact.contact.copy(
                        displayName = normalizedDisplayName ?: existingContact.contact.displayName,
                        updatedAtEpochMilliseconds = now
                    )
                )
            }

            val contactBeforePhoneNumberUpdate =
                contactDao.findById(contactId)
                    ?: error("Contact could not be loaded after saving")
            val preferredPhoneNumberId =
                if (normalizedPhoneNumber == null) {
                    contactBeforePhoneNumberUpdate.contact.preferredPhoneNumberId
                } else {
                    ensurePhoneNumberExists(
                        contactId = contactId,
                        existingPhoneNumbers = contactBeforePhoneNumberUpdate.phoneNumbers,
                        value = normalizedPhoneNumber,
                        type = ContactPhoneNumberType.MOBILE,
                        label = null,
                        now = now
                    )
                }
            val contactAfterPhoneNumber =
                contactDao.findById(contactId)
                    ?: error("Contact could not be loaded after saving phone number")

            contactDao.upsertContact(
                contactAfterPhoneNumber.contact.copy(
                    preferredPhoneNumberId = preferredPhoneNumberId,
                    updatedAtEpochMilliseconds = now
                )
            )

            contactKeyExchangeDataSource
                .storeRemoteIdentity(
                    contactId = contactId,
                    encryptionPublicKey = request.encryptionPublicKey,
                    signingPublicKey = request.signingPublicKey,
                    origin =
                        when (request.identityImportTrust) {
                            IdentityImportTrust.UNVERIFIED -> RemoteIdentityOrigin.LOCAL_IMPORT
                            IdentityImportTrust.VERIFIED_IN_PERSON -> RemoteIdentityOrigin.TRUSTED_QR_IMPORT
                        }
                )

            loadContactOrThrow(
                contactId = contactId,
                message = "Imported contact could not be loaded"
            )
        }

    override suspend fun importDeviceContact(request: ImportDeviceContactRequest): Result<Contact> =
        safeSuspendCall {
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
                findOrCreateForDeviceContact(
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
        safeSuspendCall {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            contactDao
                .findById(
                    contactId = contactId
                )?.toContact()
        }

    override suspend fun findBySigningPublicKey(signingPublicKey: ByteArray): Result<Contact?> =
        safeSuspendCall {
            require(signingPublicKey.isNotEmpty()) {
                "Signing public key must not be empty"
            }

            contactDao
                .findBySigningPublicKey(
                    signingPublicKey = signingPublicKey
                )?.toContact()
        }

    override suspend fun findOrCreateByPhoneNumber(phoneNumber: String): Result<Contact> =
        safeSuspendCall {
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
                ?.toContact()
                ?.let { contact -> return@safeSuspendCall contact }

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
                contact.toContact()
            }
        }

    override suspend fun updateContactDetails(
        contactId: String,
        displayName: String?,
        phoneNumber: String?
    ): Result<Contact> =
        safeSuspendCall {
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
        safeSuspendCall {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            val existing =
                contactDao.findById(
                    contactId = contactId
                ) ?: error("Contact not found: $contactId")

            val publicIdentity =
                existing.publicIdentity
                    ?: error("Contact has no Sparrow identity")

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
        safeSuspendCall {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            val existing =
                contactDao.findById(contactId)
                    ?: error("Contact not found: $contactId")
            val publicIdentity =
                existing.publicIdentity
                    ?: error("Contact has no Sparrow identity")

            val updatedRows =
                contactDao.updateKeyExchangeStatusIfKeysMatch(
                    contactId = contactId,
                    expectedEncryptionPublicKey = publicIdentity.encryptionPublicKey,
                    expectedSigningPublicKey = publicIdentity.signingPublicKey,
                    keyExchangeStatus = KeyExchangeStatus.MUTUAL.name,
                    updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                )

            check(updatedRows == 1) {
                "Contact identity changed before acknowledgement was applied"
            }

            loadContactOrThrow(
                contactId = contactId,
                message = "Contact could not be loaded after key exchange"
            )
        }

    override suspend fun resetKeyExchange(contactId: String): Result<Contact> =
        safeSuspendCall {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            val existing =
                contactDao.findById(
                    contactId = contactId
                ) ?: error("Contact not found: $contactId")

            existing.publicIdentity
                ?: error("Contact has no Sparrow identity")

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
        return safeSuspendCall {
            require(deviceContactId.isNotBlank()) {
                "Device contact ID must not be blank"
            }

            val existing =
                contactDao.findByDeviceContactId(
                    deviceContactId = deviceContactId
                ) ?: return@safeSuspendCall null

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
                )?.toContact()
        }
    }

    private suspend fun resolveContactForSecureIdentityImport(
        requestedContactId: String?,
        signingPublicKey: ByteArray,
        normalizedPhoneNumber: String?
    ): ResolvedContactImportDto {
        if (requestedContactId != null) {
            val selectedContact =
                contactDao.findById(
                    contactId = requestedContactId
                ) ?: error(
                    "Selected contact was not found: $requestedContactId"
                )

            return ResolvedContactImportDto(
                contactId = selectedContact.contact.id,
                isNewContact = false
            )
        }

        val mergeResult =
            findOrCreateForSparrowIdentity(
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

        return ResolvedContactImportDto(
            contactId = mergeResult.contactId,
            isNewContact = mergeResult.isNewContact
        )
    }

    private suspend fun findOrCreateForSparrowIdentity(
        signingPublicKey: ByteArray,
        phoneNumber: String?
    ): ResolvedContactImportDto {
        val normalizedPhoneNumber =
            phoneNumber
                ?.takeIf { it.isNotBlank() }
                ?.let { value -> phoneNumberNormalizer.normalize(value).getOrThrow() }

        if (normalizedPhoneNumber != null) {
            contactDao.findByNormalizedPhoneNumber(normalizedPhoneNumber)?.let { contact ->
                return ResolvedContactImportDto(contact.contact.id, isNewContact = false)
            }
        }

        contactDao.findBySigningPublicKey(signingPublicKey)?.let { contact ->
            return ResolvedContactImportDto(contact.contact.id, isNewContact = false)
        }

        return ResolvedContactImportDto(IdGenerator.generate(), isNewContact = true)
    }

    private suspend fun findOrCreateForDeviceContact(
        deviceContactId: String,
        phoneNumbers: List<ImportDevicePhoneNumber>
    ): ResolvedContactImportDto {
        contactDao.findByDeviceContactId(deviceContactId)?.let { contact ->
            return ResolvedContactImportDto(contact.contact.id, isNewContact = false)
        }

        phoneNumbers.forEach { phoneNumber ->
            val normalized =
                phoneNumberNormalizer.normalize(phoneNumber.value).getOrNull()
                    ?: return@forEach
            contactDao.findByNormalizedPhoneNumber(normalized)?.let { contact ->
                return ResolvedContactImportDto(contact.contact.id, isNewContact = false)
            }
        }

        return ResolvedContactImportDto(IdGenerator.generate(), isNewContact = true)
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
            )?.toContact() ?: error(message)

    private data class ResolvedContactImportDto(
        val contactId: String,
        val isNewContact: Boolean
    )
}
