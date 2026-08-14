package com.cbgm.securechat.feature.contacts.data.repository

import android.content.ContentResolver
import android.provider.ContactsContract
import com.cbgm.securechat.feature.contacts.domain.model.device.DeviceContact
import com.cbgm.securechat.feature.contacts.domain.model.device.DevicePhoneNumber
import com.cbgm.securechat.feature.contacts.domain.model.device.DevicePhoneNumberType
import com.cbgm.securechat.feature.contacts.domain.repository.DeviceContactsRepository

/**
 * Reads contacts and all their usable phone numbers from Android.
 *
 * One [DeviceContact] is returned per Android contact, regardless
 * of how many phone numbers belong to that person.
 */
class AndroidDeviceContactsRepositoryImpl(
    private val contentResolver: ContentResolver
) : DeviceContactsRepository {
    override suspend fun getContacts(): Result<List<DeviceContact>> =
        runCatching {
            val contacts = mutableListOf<DeviceContact>()

            contentResolver
                .query(
                    ContactsContract.Contacts.CONTENT_URI,
                    arrayOf(
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.Contacts.HAS_PHONE_NUMBER
                    ),
                    null,
                    null,
                    ContactsContract.Contacts.DISPLAY_NAME
                )?.use { cursor ->

                    val idColumn = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)

                    val nameColumn =
                        cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)

                    val hasPhoneNumberColumn =
                        cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                    while (cursor.moveToNext()) {
                        val deviceContactId = cursor.getLong(idColumn).toString()

                        val displayName =
                            cursor.getString(nameColumn)?.trim()?.takeIf { it.isNotEmpty() }

                        val hasPhoneNumber = cursor.getInt(hasPhoneNumberColumn) > 0

                        val phoneNumbers =
                            if (hasPhoneNumber) {
                                loadPhoneNumbers(contactId = deviceContactId)
                            } else {
                                emptyList()
                            }

                    /*
                     * Contacts without any usable phone number are
                     * irrelevant to the current SecureChat/SMS flow.
                     */
                        if (phoneNumbers.isEmpty()) {
                            continue
                        }

                        contacts +=
                            DeviceContact(
                                id = deviceContactId,
                                displayName = displayName,
                                phoneNumbers = phoneNumbers
                            )
                    }
                }

            contacts
        }

    /**
     * Loads all usable phone numbers for one Android contact.
     *
     * Prefer Android's normalized number when the provider exposes one.
     * Fall back to the entered number while keeping its type and label.
     */
    private fun loadPhoneNumbers(contactId: String): List<DevicePhoneNumber> {
        val phoneNumbers = mutableListOf<DevicePhoneNumber>()

        contentResolver
            .query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.LABEL
                ),
                """
                ${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?
                """.trimIndent(),
                arrayOf(contactId),
                null
            )?.use { cursor ->

                val numberColumn = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val normalizedNumberColumn =
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)

                val typeColumn = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)

                val labelColumn = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL)

                while (cursor.moveToNext()) {
                    val number =
                        cursor
                            .getString(normalizedNumberColumn)
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?: cursor
                                .getString(numberColumn)
                                ?.trim()
                                ?.takeIf { it.isNotEmpty() }
                            ?: continue

                    val androidType = cursor.getInt(typeColumn)

                    val customLabel =
                        cursor
                            .getString(labelColumn)
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }

                    phoneNumbers +=
                        DevicePhoneNumber(
                            value = number,
                            type = androidType.toDevicePhoneNumberType(),
                            label = customLabel
                        )
                }
            }

        /*
         * Some providers may expose duplicate rows. Deduplicate by
         * number and type while preserving the original ordering.
         */
        return phoneNumbers.distinctBy { phoneNumber ->
            phoneNumber.value to phoneNumber.type
        }
    }

    private fun Int.toDevicePhoneNumberType(): DevicePhoneNumberType =
        when (this) {
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> {
                DevicePhoneNumberType.MOBILE
            }

            ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE -> {
                DevicePhoneNumberType.WORK_MOBILE
            }

            ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> {
                DevicePhoneNumberType.HOME
            }

            ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> {
                DevicePhoneNumberType.WORK
            }

            ContactsContract.CommonDataKinds.Phone.TYPE_MAIN,
            ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN
            -> {
                DevicePhoneNumberType.MAIN
            }

            ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM -> {
                DevicePhoneNumberType.CUSTOM
            }

            else -> {
                DevicePhoneNumberType.OTHER
            }
        }
}
