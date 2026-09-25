package com.cbgm.sparrow.feature.contacts.data.repository

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.sparrow.data.database.entity.ContactRoutingIdEntity
import com.cbgm.sparrow.feature.contacts.data.datasource.ContactLocalDataSource
import com.cbgm.sparrow.feature.contacts.data.datasource.ContactRoutingIdDataSource
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.sparrow.feature.contacts.domain.model.identity.IdentityPeerMerge
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityPeerRepository
import kotlinx.coroutines.flow.first

internal class IdentityPeerRepositoryImpl(
    private val contactDataSource: ContactLocalDataSource,
    private val contactRoutingIdDataSource: ContactRoutingIdDataSource,
    private val phoneNumberNormalizer: PhoneNumberNormalizer
) : IdentityPeerRepository {
    override suspend fun getDisplayName(peerId: String): String? =
        contactDataSource.findContactOnlyById(peerId)?.contact?.displayName

    override suspend fun containsPeer(peerId: String): Boolean =
        contactDataSource.findContactOnlyById(peerId) != null

    override suspend fun applyMerge(merge: IdentityPeerMerge) {
        if (merge.moveBootstrapRouting) {
            val routingId = contactRoutingIdDataSource.findRoutingIdByContactId(merge.fromPeerId)
            if (routingId?.startsWith(BOOTSTRAP_ROUTING_ID_PREFIX) == true) {
                contactRoutingIdDataSource.deleteOtherContactMapping(
                    routingId = routingId,
                    contactId = merge.toPeerId
                )
                contactRoutingIdDataSource.upsert(
                    ContactRoutingIdEntity(
                        contactId = merge.toPeerId,
                        routingId = routingId
                    )
                )
            }
        }
        contactDataSource.deleteById(merge.fromPeerId)
    }

    override suspend fun updateIncomingMetadata(
        peerId: String,
        phoneNumber: String,
        updatedAtEpochMilliseconds: Long
    ) {
        contactDataSource.usePhoneNumberAsDisplayNameWhenMissing(
            contactId = peerId,
            phoneNumber = phoneNumber,
            updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
        )

        val contact = contactDataSource.findContactOnlyById(peerId) ?: return
        if (contact.phoneNumbers.any { phoneNumbersEquivalent(it.value, phoneNumber) }) return
        if (contactDataSource.findContactOnlyByNormalizedPhoneNumber(phoneNumber) != null) return

        val phoneNumberId = IdGenerator.generate()
        contactDataSource.upsertPhoneNumbers(
            listOf(
                ContactPhoneNumberEntity(
                    id = phoneNumberId,
                    contactId = peerId,
                    value = phoneNumber,
                    normalizedValue = phoneNumber,
                    type = CONTACT_PHONE_NUMBER_TYPE_MOBILE,
                    label = null,
                    updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
                )
            )
        )
        if (contact.contact.preferredPhoneNumberId == null) {
            contactDataSource.upsertContact(
                contact.contact.copy(
                    preferredPhoneNumberId = phoneNumberId,
                    updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
                )
            )
        }
    }

    override suspend fun isKnownContact(peerId: String): Boolean {
        val contact = contactDataSource.findContactOnlyById(peerId) ?: return false
        return contact.contact.deviceContactId != null || contact.phoneNumbers.isNotEmpty()
    }

    override suspend fun findEquivalentPhonePeerId(phoneNumber: String): String? {
        val contacts = contactDataSource.observeContactsOnly().first()
        val matches =
            contacts.filter { contact ->
                contact.phoneNumbers.any { storedPhoneNumber ->
                    phoneNumbersEquivalent(storedPhoneNumber.value, phoneNumber) ||
                        phoneNumbersEquivalent(storedPhoneNumber.normalizedValue, phoneNumber)
                }
            }
        if (matches.isEmpty()) return null

        return matches.firstOrNull { contact ->
            contact.contact.deviceContactLinkStatus == DeviceContactLinkStatus.LINKED.name ||
                contact.contact.deviceContactId != null
        }?.contact?.id
            ?: matches.firstOrNull { contact ->
                contact.phoneNumbers.any { storedPhoneNumber ->
                    phoneNumberNormalizer.normalize(storedPhoneNumber.normalizedValue).getOrNull() == phoneNumber
                }
            }?.contact?.id
            ?: matches.first().contact.id
    }

    override suspend fun canMergeRoutingDuplicate(
        peerId: String,
        remotePhoneNumber: String?
    ): Boolean {
        val contact = contactDataSource.findContactOnlyById(peerId) ?: return false
        if (
            contact.contact.deviceContactId != null ||
            contact.contact.deviceContactLinkStatus == DeviceContactLinkStatus.LINKED.name
        ) {
            return false
        }

        if (remotePhoneNumber == null) return contact.phoneNumbers.isEmpty()
        return contact.phoneNumbers.all { stored ->
            phoneNumbersEquivalent(stored.value, remotePhoneNumber) ||
                phoneNumbersEquivalent(stored.normalizedValue, remotePhoneNumber)
        }
    }

    private fun phoneNumbersEquivalent(first: String, second: String): Boolean {
        val firstNormalized = phoneNumberNormalizer.normalize(first).getOrNull() ?: return false
        val secondNormalized = phoneNumberNormalizer.normalize(second).getOrNull() ?: return false
        if (firstNormalized == secondNormalized) return true

        val firstInternational = firstNormalized.startsWith('+')
        val secondInternational = secondNormalized.startsWith('+')
        if (firstInternational == secondInternational) return false
        val international = if (firstInternational) firstNormalized else secondNormalized
        val domestic = if (firstInternational) secondNormalized else firstNormalized
        val internationalDigits = international.filter(Char::isDigit)
        val nationalDigits = domestic.filter(Char::isDigit).removePrefix("0")
        if (nationalDigits.length < MINIMUM_NATIONAL_NUMBER_DIGITS) return false
        if (!internationalDigits.endsWith(nationalDigits)) return false
        val countryCodeLength = internationalDigits.length - nationalDigits.length
        return countryCodeLength in MINIMUM_COUNTRY_CODE_DIGITS..MAXIMUM_COUNTRY_CODE_DIGITS
    }

    private companion object {
        const val BOOTSTRAP_ROUTING_ID_PREFIX = "scphone1_"
        const val CONTACT_PHONE_NUMBER_TYPE_MOBILE = "MOBILE"
        const val MINIMUM_NATIONAL_NUMBER_DIGITS = 7
        const val MINIMUM_COUNTRY_CODE_DIGITS = 1
        const val MAXIMUM_COUNTRY_CODE_DIGITS = 3
    }
}
