package com.cbgm.securechat.feature.contacts.data.merge

import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.feature.contacts.data.model.ContactMergeResult
import com.cbgm.securechat.feature.contacts.domain.model.ImportDevicePhoneNumber

interface ContactMergeService {
    suspend fun findOrCreateForSecureChatIdentity(
        signingPublicKey: ByteArray,
        phoneNumber: String?
    ): ContactMergeResult

    suspend fun findOrCreateForDeviceContact(
        deviceContactId: String,
        phoneNumbers: List<ImportDevicePhoneNumber>
    ): ContactMergeResult
}

class ContactMergeServiceImpl(
    private val contactDao: ContactDao,
    private val phoneNumberNormalizer: PhoneNumberNormalizer
) : ContactMergeService {
    override suspend fun findOrCreateForSecureChatIdentity(
        signingPublicKey: ByteArray,
        phoneNumber: String?
    ): ContactMergeResult {
        require(signingPublicKey.isNotEmpty()) {
            "Signing public key must not be empty"
        }

        val normalizedPhoneNumber =
            phoneNumber
                ?.takeIf { it.isNotBlank() }
                ?.let { value ->
                    phoneNumberNormalizer.normalize(value).getOrThrow()
                }

        if (normalizedPhoneNumber != null) {
            val byPhoneNumber = contactDao.findByNormalizedPhoneNumber(normalizedPhoneNumber = normalizedPhoneNumber)

            if (byPhoneNumber != null) {
                return ContactMergeResult(
                    contactId = byPhoneNumber.contact.id,
                    isNewContact = false
                )
            }
        }

        val bySigningPublicKey =
            contactDao.findBySigningPublicKey(signingPublicKey = signingPublicKey)

        if (bySigningPublicKey != null) {
            return ContactMergeResult(contactId = bySigningPublicKey.contact.id, isNewContact = false)
        }

        return ContactMergeResult(contactId = IdGenerator.generate(), isNewContact = true)
    }

    override suspend fun findOrCreateForDeviceContact(
        deviceContactId: String,
        phoneNumbers: List<ImportDevicePhoneNumber>
    ): ContactMergeResult {
        require(deviceContactId.isNotBlank()) {
            "Device contact ID must not be blank"
        }

        val byDeviceContact = contactDao.findByDeviceContactId(deviceContactId = deviceContactId)

        if (byDeviceContact != null) {
            return ContactMergeResult(contactId = byDeviceContact.contact.id, isNewContact = false)
        }

        phoneNumbers.forEach { phoneNumber ->
            val normalized = phoneNumberNormalizer.normalize(phoneNumber.value).getOrNull() ?: return@forEach

            val byPhone = contactDao.findByNormalizedPhoneNumber(normalizedPhoneNumber = normalized)

            if (byPhone != null) {
                return ContactMergeResult(contactId = byPhone.contact.id, isNewContact = false)
            }
        }

        return ContactMergeResult(contactId = IdGenerator.generate(), isNewContact = true)
    }
}
