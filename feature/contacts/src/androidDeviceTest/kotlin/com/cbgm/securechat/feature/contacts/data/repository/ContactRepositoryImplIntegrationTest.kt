package com.cbgm.securechat.feature.contacts.data.repository

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.cbgm.securechat.core.protocol.phone.DefaultPhoneNumberNormalizer
import com.cbgm.securechat.data.database.SecureChatDatabase
import com.cbgm.securechat.data.database.entity.ContactEntity
import com.cbgm.securechat.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.securechat.feature.contacts.data.merge.ContactMergeServiceImpl
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.securechat.feature.contacts.domain.model.ImportDeviceContactRequest
import com.cbgm.securechat.feature.contacts.domain.model.ImportDevicePhoneNumber
import com.cbgm.securechat.feature.contacts.domain.model.SecureChatIdentity
import com.cbgm.securechat.feature.contacts.domain.model.device.AddDeviceContactRequest
import com.cbgm.securechat.feature.contacts.domain.model.device.AddDeviceContactResult
import com.cbgm.securechat.feature.contacts.domain.repository.DeviceContactWriterRepository
import com.cbgm.securechat.feature.contacts.domain.repository.IdentityExchangeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContactRepositoryImplIntegrationTest {
    private lateinit var database: SecureChatDatabase

    private lateinit var repository: ContactRepositoryImpl

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database =
            Room
                .inMemoryDatabaseBuilder<SecureChatDatabase>(context = context)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()

        val contactDao = database.contactDao()
        val phoneNumberNormalizer = DefaultPhoneNumberNormalizer()

        repository =
            ContactRepositoryImpl(
                contactDao = contactDao,
                mergeService =
                    ContactMergeServiceImpl(
                        contactDao = contactDao,
                        phoneNumberNormalizer = phoneNumberNormalizer
                    ),
                contactKeyExchangeRepository = ContactKeyExchangeRepositoryImpl(contactDao = contactDao),
                identityExchangeRepository = TestIdentityExchangeRepository,
                phoneNumberNormalizer = phoneNumberNormalizer,
                deviceContactWriterRepository = TestDeviceContactWriterRepository
            )
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun importKeysOnlyStoresValidContact() =
        runBlocking {
            val encryptionPublicKey = testKey(seed = 1)

            val signingPublicKey = testKey(seed = 101)

            val result =
                repository.importContact(
                    request =
                        ImportContactRequest(
                            encryptionPublicKey = encryptionPublicKey,
                            signingPublicKey = signingPublicKey,
                            displayName = null,
                            phoneNumber = null
                        )
                )

            assertTrue(
                actual = result.isSuccess,
                message = "Import failed: " + result.exceptionOrNull()?.message
            )

            val contact = result.getOrThrow()

            assertNull(actual = contact.displayName)

            assertTrue(actual = contact.phoneNumbers.isEmpty())

            assertNull(actual = contact.preferredPhoneNumber)

            assertNull(actual = contact.deviceContactId)

            assertEquals(
                expected = DeviceContactLinkStatus.NOT_LINKED,
                actual = contact.deviceContactLinkStatus
            )

            val secureChatIdentity = requireSecureChatIdentity(contact.secureChatIdentity)

            assertContentEquals(
                expected = encryptionPublicKey,
                actual = secureChatIdentity.encryptionPublicKey
            )

            assertContentEquals(
                expected = signingPublicKey,
                actual = secureChatIdentity.signingPublicKey
            )

            assertEquals(
                expected = ContactVerificationStatus.UNVERIFIED,
                actual = secureChatIdentity.verificationStatus
            )
        }

    @Test
    fun importKeysAndContactDetailsStoresAllFields() =
        runBlocking {
            val encryptionPublicKey = testKey(seed = 2)

            val signingPublicKey = testKey(seed = 102)

            val contact =
                repository
                    .importContact(
                        request =
                            ImportContactRequest(
                                encryptionPublicKey = encryptionPublicKey,
                                signingPublicKey = signingPublicKey,
                                displayName = "Alice",
                                phoneNumber = "+49123456789"
                            )
                    ).getOrThrow()

            assertEquals(
                expected = "Alice",
                actual = contact.displayName
            )

            assertEquals(
                expected = "+49123456789",
                actual = contact.preferredPhoneNumber?.value
            )

            assertEquals(
                expected = 1,
                actual = contact.phoneNumbers.size
            )

            assertNull(actual = contact.deviceContactId)

            assertEquals(
                expected = DeviceContactLinkStatus.NOT_LINKED,
                actual = contact.deviceContactLinkStatus
            )

            val secureChatIdentity = requireSecureChatIdentity(contact.secureChatIdentity)

            assertContentEquals(
                expected = encryptionPublicKey,
                actual = secureChatIdentity.encryptionPublicKey
            )

            assertContentEquals(
                expected = signingPublicKey,
                actual = secureChatIdentity.signingPublicKey
            )
        }

    @Test
    fun importingSameSigningKeyTwiceUpdatesExistingContact() =
        runBlocking {
            val signingPublicKey = testKey(seed = 103)

            val firstEncryptionPublicKey = testKey(seed = 3)

            val secondEncryptionPublicKey = testKey(seed = 4)

            val firstContact =
                repository
                    .importContact(
                        request =
                            ImportContactRequest(
                                encryptionPublicKey = firstEncryptionPublicKey,
                                signingPublicKey = signingPublicKey,
                                displayName = "Alice",
                                phoneNumber = null
                            )
                    ).getOrThrow()

            val secondContact =
                repository
                    .importContact(
                        request =
                            ImportContactRequest(
                                encryptionPublicKey = secondEncryptionPublicKey,
                                signingPublicKey = signingPublicKey,
                                displayName = "Alice Updated",
                                phoneNumber = "+49111111111"
                            )
                    ).getOrThrow()

            assertEquals(
                expected = firstContact.id,
                actual = secondContact.id
            )

            assertEquals(
                expected = "Alice Updated",
                actual = secondContact.displayName
            )

            assertEquals(
                expected = "+49111111111",
                actual = secondContact.preferredPhoneNumber?.value
            )

            val secureChatIdentity = requireSecureChatIdentity(secondContact.secureChatIdentity)

            assertContentEquals(
                expected = secondEncryptionPublicKey,
                actual = secureChatIdentity.encryptionPublicKey
            )

            assertContentEquals(
                expected = signingPublicKey,
                actual = secureChatIdentity.signingPublicKey
            )

            val contacts = repository.observeContacts().first()

            assertEquals(
                expected = 1,
                actual = contacts.size
            )
        }

    @Test
    fun reimportWithoutMetadataDoesNotEraseExistingDetails() =
        runBlocking {
            val signingPublicKey = testKey(seed = 104)

            repository
                .importContact(
                    request =
                        ImportContactRequest(
                            encryptionPublicKey = testKey(seed = 5),
                            signingPublicKey = signingPublicKey,
                            displayName = "Bob",
                            phoneNumber = "+49222222222"
                        )
                ).getOrThrow()

            val updatedContact =
                repository
                    .importContact(
                        request =
                            ImportContactRequest(
                                encryptionPublicKey = testKey(seed = 6),
                                signingPublicKey = signingPublicKey,
                                displayName = null,
                                phoneNumber = null
                            )
                    ).getOrThrow()

            assertEquals(
                expected = "Bob",
                actual = updatedContact.displayName
            )

            assertEquals(
                expected = "+49222222222",
                actual = updatedContact.preferredPhoneNumber?.value
            )

            val secureChatIdentity = requireSecureChatIdentity(updatedContact.secureChatIdentity)

            assertContentEquals(
                expected = testKey(seed = 6),
                actual = secureChatIdentity.encryptionPublicKey
            )
        }

    @Test
    fun newSecureChatIdentityStartsUnverified() =
        runBlocking {
            val contact =
                repository
                    .importContact(
                        request =
                            ImportContactRequest(
                                encryptionPublicKey = testKey(seed = 7),
                                signingPublicKey = testKey(seed = 107),
                                displayName = null,
                                phoneNumber = null
                            )
                    ).getOrThrow()

            val secureChatIdentity = requireSecureChatIdentity(contact.secureChatIdentity)

            assertEquals(
                expected = ContactVerificationStatus.UNVERIFIED,
                actual = secureChatIdentity.verificationStatus
            )
        }

    @Test
    fun markVerifiedPersistsVerificationState() =
        runBlocking {
            val importedContact =
                repository
                    .importContact(
                        request =
                            ImportContactRequest(
                                encryptionPublicKey = testKey(seed = 8),
                                signingPublicKey = testKey(seed = 108),
                                displayName = "Charlie",
                                phoneNumber = null
                            )
                    ).getOrThrow()

            val verifiedContact = repository.markVerified(contactId = importedContact.id).getOrThrow()

            val verifiedIdentity = requireSecureChatIdentity(verifiedContact.secureChatIdentity)

            assertEquals(
                expected = ContactVerificationStatus.VERIFIED,
                actual = verifiedIdentity.verificationStatus
            )

            val loadedContact = repository.getContact(contactId = importedContact.id).getOrThrow()

            assertNotNull(actual = loadedContact)

            val loadedIdentity = requireSecureChatIdentity(loadedContact.secureChatIdentity)

            assertEquals(
                expected = ContactVerificationStatus.VERIFIED,
                actual = loadedIdentity.verificationStatus
            )
        }

    @Test
    fun importDeviceContactCreatesContactWithoutKeys() =
        runBlocking {
            val contact =
                repository
                    .importDeviceContact(
                        request =
                            ImportDeviceContactRequest(
                                deviceContactId = "device-contact-42",
                                displayName = "Dana",
                                phoneNumbers = listOf(devicePhoneNumber(value = "+49333333333"))
                            )
                    ).getOrThrow()

            assertEquals(
                expected = "Dana",
                actual = contact.displayName
            )

            assertEquals(
                expected = "+49333333333",
                actual = contact.preferredPhoneNumber?.value
            )

            assertEquals(
                expected = 1,
                actual = contact.phoneNumbers.size
            )

            assertEquals(
                expected = "device-contact-42",
                actual = contact.deviceContactId
            )

            assertEquals(
                expected = DeviceContactLinkStatus.LINKED,
                actual = contact.deviceContactLinkStatus
            )

            assertNull(actual = contact.secureChatIdentity)
        }

    @Test
    fun importDeviceContactStoresAllPhoneNumbers() =
        runBlocking {
            val contact =
                repository
                    .importDeviceContact(
                        request =
                            ImportDeviceContactRequest(
                                deviceContactId = "device-contact-multiple",
                                displayName = "Multiple Numbers",
                                phoneNumbers =
                                    listOf(
                                        devicePhoneNumber(
                                            value = "+49111111111",
                                            type = ContactPhoneNumberType.HOME
                                        ),
                                        devicePhoneNumber(
                                            value = "+49222222222",
                                            type = ContactPhoneNumberType.MOBILE
                                        ),
                                        devicePhoneNumber(
                                            value = "+49333333333",
                                            type = ContactPhoneNumberType.WORK
                                        )
                                    )
                            )
                    ).getOrThrow()

            assertEquals(
                expected = 3,
                actual = contact.phoneNumbers.size
            )

            assertEquals(
                expected = "+49222222222",
                actual = contact.preferredPhoneNumber?.value
            )

            assertEquals(
                expected = ContactPhoneNumberType.MOBILE,
                actual = contact.preferredPhoneNumber?.type
            )
        }

    @Test
    fun importingSameDeviceContactUpdatesExistingContact() =
        runBlocking {
            val firstContact =
                repository
                    .importDeviceContact(
                        request =
                            ImportDeviceContactRequest(
                                deviceContactId = "device-contact-43",
                                displayName = "Erin",
                                phoneNumbers =
                                    listOf(
                                        devicePhoneNumber(
                                            value =
                                                "+49444444444"
                                        )
                                    )
                            )
                    ).getOrThrow()

            val secondContact =
                repository
                    .importDeviceContact(
                        request =
                            ImportDeviceContactRequest(
                                deviceContactId =
                                    "device-contact-43",
                                displayName =
                                    "Erin Updated",
                                phoneNumbers =
                                    listOf(
                                        devicePhoneNumber(
                                            value =
                                                "+49555555555"
                                        )
                                    )
                            )
                    ).getOrThrow()

            assertEquals(
                expected = firstContact.id,
                actual = secondContact.id
            )

            assertEquals(
                expected = "Erin Updated",
                actual = secondContact.displayName
            )

            assertEquals(
                expected = "+49555555555",
                actual =
                    secondContact
                        .preferredPhoneNumber
                        ?.value
            )

            assertEquals(
                expected = 1,
                actual = secondContact.phoneNumbers.size
            )

            assertEquals(
                expected = "device-contact-43",
                actual = secondContact.deviceContactId
            )

            assertEquals(
                expected =
                    DeviceContactLinkStatus.LINKED,
                actual =
                    secondContact.deviceContactLinkStatus
            )

            val contacts =
                repository
                    .observeContacts()
                    .first()

            assertEquals(
                expected = 1,
                actual = contacts.size
            )
        }

    @Test
    fun incompleteDeviceContactNameDoesNotEraseStoredName() =
        runBlocking {
            repository
                .importDeviceContact(
                    request =
                        ImportDeviceContactRequest(
                            deviceContactId =
                                "device-contact-44",
                            displayName =
                                "Frank",
                            phoneNumbers =
                                listOf(
                                    devicePhoneNumber(
                                        value =
                                            "+49666666666"
                                    )
                                )
                        )
                ).getOrThrow()

            val updatedContact =
                repository
                    .importDeviceContact(
                        request =
                            ImportDeviceContactRequest(
                                deviceContactId =
                                    "device-contact-44",
                                displayName = null,
                                phoneNumbers =
                                    listOf(
                                        devicePhoneNumber(
                                            value =
                                                "+49666666666"
                                        )
                                    )
                            )
                    ).getOrThrow()

            assertEquals(
                expected = "Frank",
                actual = updatedContact.displayName
            )

            assertEquals(
                expected = "+49666666666",
                actual =
                    updatedContact
                        .preferredPhoneNumber
                        ?.value
            )
        }

    @Test
    fun deviceContactWithoutPhoneNumbersFails() =
        runBlocking {
            val result =
                repository.importDeviceContact(
                    request =
                        ImportDeviceContactRequest(
                            deviceContactId =
                                "device-contact-empty",
                            displayName =
                                "No Number",
                            phoneNumbers =
                                emptyList()
                        )
                )

            assertTrue(
                actual = result.isFailure
            )

            val contacts =
                repository
                    .observeContacts()
                    .first()

            assertTrue(
                actual = contacts.isEmpty()
            )
        }

    @Test
    fun keysCanBeAttachedToExistingDeviceContact() =
        runBlocking {
            val existingContact =
                repository
                    .importDeviceContact(
                        request =
                            ImportDeviceContactRequest(
                                deviceContactId =
                                    "device-contact-45",
                                displayName =
                                    "Grace",
                                phoneNumbers =
                                    listOf(
                                        devicePhoneNumber(
                                            value =
                                                "+49777777777"
                                        )
                                    )
                            )
                    ).getOrThrow()

            assertNull(
                actual =
                    existingContact.secureChatIdentity
            )

            val encryptionPublicKey =
                testKey(seed = 9)

            val signingPublicKey =
                testKey(seed = 109)

            val importedContact =
                repository
                    .importContact(
                        request =
                            ImportContactRequest(
                                encryptionPublicKey =
                                encryptionPublicKey,
                                signingPublicKey =
                                signingPublicKey,
                                displayName = null,
                                phoneNumber =
                                    "+49777777777"
                            )
                    ).getOrThrow()

            assertEquals(
                expected = existingContact.id,
                actual = importedContact.id
            )

            assertEquals(
                expected = "Grace",
                actual = importedContact.displayName
            )

            assertEquals(
                expected = "+49777777777",
                actual =
                    importedContact
                        .preferredPhoneNumber
                        ?.value
            )

            assertEquals(
                expected = "device-contact-45",
                actual = importedContact.deviceContactId
            )

            assertEquals(
                expected =
                    DeviceContactLinkStatus.LINKED,
                actual =
                    importedContact
                        .deviceContactLinkStatus
            )

            val secureChatIdentity =
                requireSecureChatIdentity(
                    importedContact.secureChatIdentity
                )

            assertContentEquals(
                expected = encryptionPublicKey,
                actual =
                    secureChatIdentity.encryptionPublicKey
            )

            assertContentEquals(
                expected = signingPublicKey,
                actual =
                    secureChatIdentity.signingPublicKey
            )

            val allContacts =
                repository
                    .observeContacts()
                    .first()

            assertEquals(
                expected = 1,
                actual = allContacts.size
            )
        }

    @Test
    fun importingDeviceContactCanLinkExistingSecureChatContact() =
        runBlocking {
            val contactWithIdentity =
                repository
                    .importContact(
                        request =
                            ImportContactRequest(
                                encryptionPublicKey =
                                    testKey(seed = 10),
                                signingPublicKey =
                                    testKey(seed = 110),
                                displayName =
                                    "Helen",
                                phoneNumber =
                                    "+49888888888"
                            )
                    ).getOrThrow()

            val linkedContact =
                repository
                    .importDeviceContact(
                        request =
                            ImportDeviceContactRequest(
                                deviceContactId =
                                    "device-contact-46",
                                displayName =
                                    "Helen Device",
                                phoneNumbers =
                                    listOf(
                                        devicePhoneNumber(
                                            value =
                                                "+49888888888"
                                        )
                                    )
                            )
                    ).getOrThrow()

            assertEquals(
                expected = contactWithIdentity.id,
                actual = linkedContact.id
            )

            assertNotNull(
                actual =
                    linkedContact.secureChatIdentity
            )

            assertEquals(
                expected = "device-contact-46",
                actual = linkedContact.deviceContactId
            )

            assertEquals(
                expected =
                    DeviceContactLinkStatus.LINKED,
                actual =
                    linkedContact.deviceContactLinkStatus
            )
        }

    @Test
    fun updateDeviceContactLinkStatusCanMarkContactMissing() =
        runBlocking {
            val importedContact =
                repository
                    .importDeviceContact(
                        request =
                            ImportDeviceContactRequest(
                                deviceContactId =
                                    "device-contact-47",
                                displayName =
                                    "Ian",
                                phoneNumbers =
                                    listOf(
                                        devicePhoneNumber(
                                            value =
                                                "+49999999999"
                                        )
                                    )
                            )
                    ).getOrThrow()

            val updatedContact =
                repository
                    .updateDeviceContactLinkStatus(
                        deviceContactId =
                            "device-contact-47",
                        status =
                            DeviceContactLinkStatus.MISSING
                    ).getOrThrow()

            assertNotNull(
                actual = updatedContact
            )

            assertEquals(
                expected = importedContact.id,
                actual = updatedContact.id
            )

            assertEquals(
                expected =
                    DeviceContactLinkStatus.MISSING,
                actual =
                    updatedContact.deviceContactLinkStatus
            )

            assertEquals(
                expected = "device-contact-47",
                actual =
                    updatedContact.deviceContactId
            )
        }

    @Test
    fun missingDeviceContactCanBecomeLinkedAgain() =
        runBlocking {
            repository
                .importDeviceContact(
                    request =
                        ImportDeviceContactRequest(
                            deviceContactId =
                                "device-contact-48",
                            displayName =
                                "Julia",
                            phoneNumbers =
                                listOf(
                                    devicePhoneNumber(
                                        value =
                                            "+49101010101"
                                    )
                                )
                        )
                ).getOrThrow()

            repository
                .updateDeviceContactLinkStatus(
                    deviceContactId =
                        "device-contact-48",
                    status =
                        DeviceContactLinkStatus.MISSING
                ).getOrThrow()

            val linkedAgain =
                repository
                    .updateDeviceContactLinkStatus(
                        deviceContactId =
                            "device-contact-48",
                        status =
                            DeviceContactLinkStatus.LINKED
                    ).getOrThrow()

            assertNotNull(actual = linkedAgain)

            assertEquals(
                expected = DeviceContactLinkStatus.LINKED,
                actual = linkedAgain.deviceContactLinkStatus
            )
        }

    @Test
    fun updatingUnknownDeviceContactLinkReturnsNull() =
        runBlocking {
            val result =
                repository
                    .updateDeviceContactLinkStatus(
                        deviceContactId =
                            "does-not-exist",
                        status =
                            DeviceContactLinkStatus.MISSING
                    ).getOrThrow()

            assertNull(actual = result)
        }

    @Test
    fun differentSigningKeyDoesNotReplaceExistingIdentity() =
        runBlocking {
            val phoneNumber = "+49111112222"

            val firstSigningKey = testKey(seed = 111)

            val secondSigningKey = testKey(seed = 112)

            val firstContact =
                repository
                    .importContact(
                        request =
                            ImportContactRequest(
                                encryptionPublicKey = testKey(seed = 11),
                                signingPublicKey = firstSigningKey,
                                displayName = "Karl",
                                phoneNumber = phoneNumber
                            )
                    ).getOrThrow()

            val replacementResult =
                repository.importContact(
                    request =
                        ImportContactRequest(
                            encryptionPublicKey = testKey(seed = 12),
                            signingPublicKey = secondSigningKey,
                            displayName = "Karl",
                            phoneNumber = phoneNumber
                        )
                )

            assertTrue(actual = replacementResult.isFailure)

            val loadedContact =
                repository.getContact(contactId = firstContact.id).getOrThrow()

            assertNotNull(actual = loadedContact)

            val secureChatIdentity =
                requireSecureChatIdentity(
                    loadedContact.secureChatIdentity
                )

            assertContentEquals(
                expected = firstSigningKey,
                actual = secureChatIdentity.signingPublicKey
            )
        }

    @Test
    fun manualContactDetailsUpdateCanChangePreferredNumber() =
        runBlocking {
            val contact =
                repository
                    .importDeviceContact(
                        request =
                            ImportDeviceContactRequest(
                                deviceContactId = "device-contact-edit",
                                displayName = "Laura",
                                phoneNumbers =
                                    listOf(
                                        devicePhoneNumber(
                                            value = "+49131313131",
                                            type = ContactPhoneNumberType.HOME
                                        ),
                                        devicePhoneNumber(
                                            value = "+49141414141",
                                            type = ContactPhoneNumberType.MOBILE
                                        )
                                    )
                            )
                    ).getOrThrow()

            assertEquals(
                expected = "+49141414141",
                actual = contact.preferredPhoneNumber?.value
            )

            val updated =
                repository
                    .updateContactDetails(
                        contactId = contact.id,
                        displayName = "Laura Updated",
                        phoneNumber = "+49131313131"
                    ).getOrThrow()

            assertEquals(
                expected = "Laura Updated",
                actual = updated.displayName
            )

            assertEquals(
                expected = "+49131313131",
                actual =
                    updated.preferredPhoneNumber?.value
            )

            assertEquals(
                expected = 2,
                actual = updated.phoneNumbers.size
            )
        }

    @Test
    fun manuallyInsertedLinkedDeviceContactIsMappedCorrectly() =
        runBlocking {
            val contactId = "manually-inserted-contact"

            val phoneNumberId = "manually-inserted-phone"

            database
                .contactDao()
                .upsertContact(
                    contact =
                        ContactEntity(
                            id = contactId,
                            displayName = "Laura",
                            deviceContactId = "device-contact-49",
                            deviceContactLinkStatus = DeviceContactLinkStatus.LINKED.name,
                            preferredPhoneNumberId = phoneNumberId,
                            createdAtEpochMilliseconds = 1_000L,
                            updatedAtEpochMilliseconds = 1_000L
                        )
                )

            database
                .contactDao()
                .upsertPhoneNumbers(
                    phoneNumbers =
                        listOf(
                            ContactPhoneNumberEntity(
                                id = phoneNumberId,
                                contactId = contactId,
                                value = "+49131313131",
                                normalizedValue = "+491701234567",
                                type = ContactPhoneNumberType.MOBILE.name,
                                label = null,
                                updatedAtEpochMilliseconds = 1_000L
                            )
                        )
                )

            val loadedContact = repository.getContact(contactId = contactId).getOrThrow()

            assertNotNull(
                actual = loadedContact
            )

            assertEquals(
                expected = "device-contact-49",
                actual = loadedContact.deviceContactId
            )

            assertEquals(
                expected = DeviceContactLinkStatus.LINKED,
                actual = loadedContact.deviceContactLinkStatus
            )

            assertEquals(
                expected = "+49131313131",
                actual = loadedContact.preferredPhoneNumber?.value
            )

            assertNull(actual = loadedContact.secureChatIdentity)
        }

    private fun devicePhoneNumber(
        value: String,
        type: ContactPhoneNumberType = ContactPhoneNumberType.MOBILE,
        label: String? = null
    ): ImportDevicePhoneNumber =
        ImportDevicePhoneNumber(
            value = value,
            type = type,
            label = label
        )

    private fun requireSecureChatIdentity(secureChatIdentity: SecureChatIdentity?): SecureChatIdentity =
        assertNotNull(
            actual = secureChatIdentity,
            message = "Expected contact to have a SecureChat identity"
        )

    private fun testKey(seed: Int): ByteArray =
        ByteArray(size = 32) { index ->
            (seed + index).mod(256).toByte()
        }
}

private object TestIdentityExchangeRepository : IdentityExchangeRepository {
    override suspend fun ensureStarted(contactId: String): Result<Unit> = Result.success(Unit)

    override suspend fun startManualExchange(contactId: String): Result<Unit> = Result.success(Unit)
}

private object TestDeviceContactWriterRepository : DeviceContactWriterRepository {
    override suspend fun addIfNotExists(request: AddDeviceContactRequest): AddDeviceContactResult = AddDeviceContactResult.AlreadyExists
}
