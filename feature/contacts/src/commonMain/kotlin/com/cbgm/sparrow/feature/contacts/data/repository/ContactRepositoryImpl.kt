package com.cbgm.sparrow.feature.contacts.data.repository

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.entity.ContactEntity
import com.cbgm.sparrow.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.sparrow.feature.contacts.data.datasource.ContactLocalDataSource
import com.cbgm.sparrow.feature.contacts.data.mapper.toContact
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.sparrow.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.ImportDeviceContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.ImportDevicePhoneNumber
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ContactRepositoryImpl(
    private val contactDataSource: ContactLocalDataSource,
    private val phoneNumberNormalizer: PhoneNumberNormalizer
) : ContactRepository {
    override suspend fun upsertImportedContact(request: ImportContactRequest): Result<Contact> =
        safeSuspendCall {
            require(request.encryptionPublicKey.isNotEmpty()) {
                "Encryption public key must not be empty"
            }
            require(request.signingPublicKey.isNotEmpty()) {
                "Signing public key must not be empty"
            }

            val normalizedDisplayName =
                request.displayName
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }

            val normalizedPhoneNumber =
                request.phoneNumber
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { value -> phoneNumberNormalizer.normalize(value).getOrThrow() }

            val now = SystemClock.nowEpochMilliseconds()

            val resolvedContact =
                resolveContactForSecureIdentityImport(
                    requestedContactId = request.contactId,
                    matchedIdentityContactId = request.matchedIdentityContactId,
                    normalizedPhoneNumber = normalizedPhoneNumber
                )

            val contactId = resolvedContact.contactId

            if (resolvedContact.isNewContact) {
                contactDataSource.upsertContact(
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
                    contactDataSource.findContactOnlyById(contactId)
                        ?: error("Matched contact could not be loaded")
                contactDataSource.upsertContact(
                    existingContact.contact.copy(
                        displayName = normalizedDisplayName ?: existingContact.contact.displayName,
                        updatedAtEpochMilliseconds = now
                    )
                )
            }

            val contactBeforePhoneNumberUpdate =
                contactDataSource.findContactOnlyById(contactId)
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
                contactDataSource.findContactOnlyById(contactId)
                    ?: error("Contact could not be loaded after saving phone number")

            contactDataSource.upsertContact(
                contactAfterPhoneNumber.contact.copy(
                    preferredPhoneNumberId = preferredPhoneNumberId,
                    updatedAtEpochMilliseconds = now
                )
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
                contactDataSource.upsertContact(
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
                contactDataSource.findContactOnlyById(
                    contactId = contactId
                ) ?: error("Device contact could not be loaded")

            contactDataSource.upsertContact(
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

            contactDataSource
                .findContactOnlyById(contactId)
                ?.toContact()
        }

    override suspend fun usePhoneNumberAsDisplayNameWhenMissing(
        contactId: String,
        phoneNumber: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Unit> = safeSuspendCall {
        require(contactId.isNotBlank())
        require(phoneNumber.isNotBlank())
        contactDataSource.usePhoneNumberAsDisplayNameWhenMissing(
            contactId = contactId,
            phoneNumber = phoneNumber,
            updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
        )
    }

    override suspend fun resolveAuthenticatedPeerContact(
        senderContactId: String?,
        phoneNumber: String?
    ): Result<String> = safeSuspendCall {
        if (senderContactId != null) {
            phoneNumber?.trim()?.takeIf(String::isNotEmpty)?.let { number ->
                val normalized = phoneNumberNormalizer.normalize(number).getOrThrow()
                val existing = contactDataSource.findContactOnlyById(senderContactId)
                if (existing != null) {
                    val now = SystemClock.nowEpochMilliseconds()
                    val phoneNumberId = existing.contact.preferredPhoneNumberId ?: IdGenerator.generate()
                    contactDataSource.upsertContact(
                        existing.contact.copy(
                            preferredPhoneNumberId = phoneNumberId,
                            updatedAtEpochMilliseconds = now
                        )
                    )
                    contactDataSource.usePhoneNumberAsDisplayNameWhenMissing(
                        contactId = senderContactId,
                        phoneNumber = number,
                        updatedAtEpochMilliseconds = now
                    )
                    contactDataSource.upsertPhoneNumbers(
                        listOf(
                            ContactPhoneNumberEntity(
                                id = phoneNumberId,
                                contactId = senderContactId,
                                value = number,
                                normalizedValue = normalized,
                                type = ContactPhoneNumberType.MOBILE.name,
                                label = null,
                                updatedAtEpochMilliseconds = now
                            )
                        )
                    )
                }
            }
            return@safeSuspendCall senderContactId
        }
        val number = phoneNumber?.trim()?.takeIf(String::isNotEmpty)
            ?: error("Group member has no phone number")
        val normalized = phoneNumberNormalizer.normalize(number).getOrThrow()
        contactDataSource.findContactOnlyByNormalizedPhoneNumber(normalized)?.let { existing ->
            return@safeSuspendCall existing.contact.id
        }
        val now = SystemClock.nowEpochMilliseconds()
        val contactId = IdGenerator.generate()
        val phoneNumberId = IdGenerator.generate()
        contactDataSource.upsertContact(
            ContactEntity(
                id = contactId,
                displayName = number,
                deviceContactId = null,
                deviceContactLinkStatus = DeviceContactLinkStatus.NOT_LINKED.name,
                preferredPhoneNumberId = phoneNumberId,
                createdAtEpochMilliseconds = now,
                updatedAtEpochMilliseconds = now
            )
        )
        contactDataSource.upsertPhoneNumbers(
            listOf(
                ContactPhoneNumberEntity(
                    id = phoneNumberId,
                    contactId = contactId,
                    value = number,
                    normalizedValue = normalized,
                    type = ContactPhoneNumberType.MOBILE.name,
                    label = null,
                    updatedAtEpochMilliseconds = now
                )
            )
        )
        contactId
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

            contactDataSource
                .findContactOnlyByNormalizedPhoneNumber(phoneNumber = normalizedValue)
                ?.let { contact ->
                    return@safeSuspendCall loadContactOrThrow(
                        contactId = contact.contact.id,
                        message = "Matched contact could not be loaded"
                    )
                }

            val now = SystemClock.nowEpochMilliseconds()
            val contactId = IdGenerator.generate()
            val phoneNumberId = IdGenerator.generate()

            contactDataSource.upsertContact(
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
            contactDataSource.upsertPhoneNumbers(
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
        contactDataSource.observeContactsOnly().map { contacts ->
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
                contactDataSource.findContactOnlyById(
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

            contactDataSource.upsertContact(
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

    override suspend fun updateDeviceContactLinkStatus(
        deviceContactId: String,
        status: DeviceContactLinkStatus
    ): Result<Contact?> {
        return safeSuspendCall {
            require(deviceContactId.isNotBlank()) {
                "Device contact ID must not be blank"
            }

            val existing =
                contactDataSource.findContactOnlyByDeviceContactId(
                    deviceContactId = deviceContactId
                ) ?: return@safeSuspendCall null

            contactDataSource.upsertContact(
                contact =
                    existing.contact.copy(
                        deviceContactLinkStatus = status.name,
                        updatedAtEpochMilliseconds =
                            SystemClock.nowEpochMilliseconds()
                    )
            )

            contactDataSource
                .findContactOnlyById(existing.contact.id)
                ?.toContact()
        }
    }

    private suspend fun resolveContactForSecureIdentityImport(
        requestedContactId: String?,
        matchedIdentityContactId: String?,
        normalizedPhoneNumber: String?
    ): ResolvedContactImportDto {
        if (requestedContactId != null) {
            val selectedContact =
                contactDataSource.findContactOnlyById(
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
                matchedIdentityContactId = matchedIdentityContactId,
                phoneNumber = normalizedPhoneNumber
            )

        if (!mergeResult.isNewContact) {
            contactDataSource.findContactOnlyById(
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
        matchedIdentityContactId: String?,
        phoneNumber: String?
    ): ResolvedContactImportDto {
        val normalizedPhoneNumber =
            phoneNumber
                ?.takeIf { it.isNotBlank() }
                ?.let { value -> phoneNumberNormalizer.normalize(value).getOrThrow() }

        if (normalizedPhoneNumber != null) {
            contactDataSource.findContactOnlyByNormalizedPhoneNumber(normalizedPhoneNumber)?.let { contact ->
                return ResolvedContactImportDto(contact.contact.id, isNewContact = false)
            }
        }

        matchedIdentityContactId?.let { contactId ->
            contactDataSource.findContactOnlyById(contactId)?.let { contact ->
                return ResolvedContactImportDto(contact.contact.id, isNewContact = false)
            }
            // A stale Identity index is not a reason to manufacture a new contact silently.
            error("Identity-matched contact was not found: $contactId")
        }

        return ResolvedContactImportDto(IdGenerator.generate(), isNewContact = true)
    }

    private suspend fun findOrCreateForDeviceContact(
        deviceContactId: String,
        phoneNumbers: List<ImportDevicePhoneNumber>
    ): ResolvedContactImportDto {
        contactDataSource.findContactOnlyByDeviceContactId(deviceContactId)?.let { contact ->
            return ResolvedContactImportDto(contact.contact.id, isNewContact = false)
        }

        phoneNumbers.forEach { phoneNumber ->
            val normalized =
                phoneNumberNormalizer.normalize(phoneNumber.value).getOrNull()
                    ?: return@forEach
            contactDataSource.findContactOnlyByNormalizedPhoneNumber(normalized)?.let { contact ->
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
        contactDataSource.deletePhoneNumbersForContact(
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

        contactDataSource.upsertPhoneNumbers(
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

        contactDataSource.upsertPhoneNumbers(
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
        contactDataSource
            .findContactOnlyById(contactId)
            ?.toContact() ?: error(message)

    private data class ResolvedContactImportDto(
        val contactId: String,
        val isNewContact: Boolean
    )
}
