package com.cbgm.sparrow.feature.contacts.data.repository

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.cbgm.sparrow.core.protocol.phone.DefaultPhoneNumberNormalizer
import com.cbgm.sparrow.data.database.SparrowDatabase
import com.cbgm.sparrow.data.database.entity.ContactEntity
import com.cbgm.sparrow.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.sparrow.feature.contacts.data.datasource.ContactLocalDataSource
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityImportTrust
import com.cbgm.sparrow.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.ImportDeviceContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.ImportDevicePhoneNumber
import com.cbgm.sparrow.feature.contacts.domain.model.SparrowIdentity
import com.cbgm.sparrow.feature.identity.data.repository.RemoteIdentityImportRepositoryImpl
import com.cbgm.sparrow.feature.identity.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.identity.domain.model.RemoteIdentityOrigin
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
    private lateinit var database: SparrowDatabase

    private lateinit var repository: ContactRepositoryImpl
    private lateinit var remoteIdentities: RemoteIdentityImportRepositoryImpl

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database =
            Room
                .inMemoryDatabaseBuilder<SparrowDatabase>(context = context)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()

        val contactDao = database.contactDao()
        val phoneNumberNormalizer = DefaultPhoneNumberNormalizer()

        remoteIdentities = RemoteIdentityImportRepositoryImpl(database.remoteIdentityDao())

        repository =
            ContactRepositoryImpl(
                contactDataSource = ContactLocalDataSource(contactDao),
                phoneNumberNormalizer = phoneNumberNormalizer
            )
    }

    /** The old repository tests exercise the complete import across the two owning modules. */
    private suspend fun ContactRepositoryImpl.importContact(request: ImportContactRequest): Result<Contact> =
        runCatching {
            val resolvedRequest = request.copy(
                matchedIdentityContactId =
                    if (request.contactId == null) {
                        database.remoteIdentityDao().findBySigningPublicKey(request.signingPublicKey)?.contactId
                    } else {
                        null
                    }
            )
            val contact = upsertImportedContact(resolvedRequest).getOrThrow()
            remoteIdentities.storeRemoteIdentity(
                contactId = contact.id,
                encryptionPublicKey = request.encryptionPublicKey,
                signingPublicKey = request.signingPublicKey,
                origin = if (request.identityImportTrust == IdentityImportTrust.VERIFIED_IN_PERSON) {
                    RemoteIdentityOrigin.TRUSTED_QR_IMPORT
                } else {
                    RemoteIdentityOrigin.LOCAL_IMPORT
                }
            ).getOrThrow()
            val loaded = getContact(contact.id).getOrThrow() ?: error("Imported contact was not found")
            loaded.copy(sparrowIdentity = storedIdentity(loaded.id))
        }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun staleIdentityMatchDoesNotCreateASecondContact() =
        runBlocking {
            val result = repository.upsertImportedContact(
                ImportContactRequest(
                    displayName = "Alice",
                    phoneNumber = null,
                    encryptionPublicKey = testKey(seed = 17),
                    signingPublicKey = testKey(seed = 117),
                    matchedIdentityContactId = "deleted-contact-id"
                )
            )
            assertTrue(result.isFailure)
            assertTrue(repository.observeContacts().first().isEmpty())
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

            val sparrowIdentity = requireSparrowIdentity(contact.sparrowIdentity)

            assertContentEquals(
                expected = encryptionPublicKey,
                actual = sparrowIdentity.encryptionPublicKey
            )

            assertContentEquals(
                expected = signingPublicKey,
                actual = sparrowIdentity.signingPublicKey
            )

            assertEquals(
                expected = ContactVerificationStatus.UNVERIFIED,
                actual = sparrowIdentity.verificationStatus
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

            val sparrowIdentity = requireSparrowIdentity(contact.sparrowIdentity)

            assertContentEquals(
                expected = encryptionPublicKey,
                actual = sparrowIdentity.encryptionPublicKey
            )

            assertContentEquals(
                expected = signingPublicKey,
                actual = sparrowIdentity.signingPublicKey
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

            val sparrowIdentity = requireSparrowIdentity(secondContact.sparrowIdentity)

            assertContentEquals(
                expected = secondEncryptionPublicKey,
                actual = sparrowIdentity.encryptionPublicKey
            )

            assertContentEquals(
                expected = signingPublicKey,
                actual = sparrowIdentity.signingPublicKey
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

            val sparrowIdentity = requireSparrowIdentity(updatedContact.sparrowIdentity)

            assertContentEquals(
                expected = testKey(seed = 6),
                actual = sparrowIdentity.encryptionPublicKey
            )
        }

    @Test
    fun newSparrowIdentityStartsUnverified() =
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

            val sparrowIdentity = requireSparrowIdentity(contact.sparrowIdentity)

            assertEquals(
                expected = ContactVerificationStatus.UNVERIFIED,
                actual = sparrowIdentity.verificationStatus
            )
        }

    @Test
    fun identityOwnerVerificationIsVisibleInContacts() =
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

            remoteIdentities.storeRemoteIdentity(
                contactId = importedContact.id,
                encryptionPublicKey = testKey(seed = 8),
                signingPublicKey = testKey(seed = 108),
                origin = RemoteIdentityOrigin.TRUSTED_QR_IMPORT
            ).getOrThrow()
            val verifiedContact = repository.getContact(importedContact.id).getOrThrow()!!

            val verifiedIdentity = storedIdentity(verifiedContact.id)

            assertEquals(
                expected = ContactVerificationStatus.VERIFIED,
                actual = verifiedIdentity.verificationStatus
            )

            val loadedContact = repository.getContact(contactId = importedContact.id).getOrThrow()

            assertNotNull(actual = loadedContact)

            val loadedIdentity = storedIdentity(loadedContact.id)

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

            assertNull(actual = contact.sparrowIdentity)
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
                    existingContact.sparrowIdentity
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

            val sparrowIdentity =
                requireSparrowIdentity(
                    importedContact.sparrowIdentity
                )

            assertContentEquals(
                expected = encryptionPublicKey,
                actual =
                    sparrowIdentity.encryptionPublicKey
            )

            assertContentEquals(
                expected = signingPublicKey,
                actual =
                    sparrowIdentity.signingPublicKey
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
    fun importingDeviceContactCanLinkExistingSparrowContact() =
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

            assertNotNull(storedIdentity(linkedContact.id))
            assertNull(linkedContact.sparrowIdentity)

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

            val sparrowIdentity =
                storedIdentity(loadedContact.id)

            assertContentEquals(
                expected = firstSigningKey,
                actual = sparrowIdentity.signingPublicKey
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
    fun contactOnlyMetadataUpdatesPreserveVerifiedIdentityAndPreferredNumber() =
        runBlocking {
            val imported = repository.importContact(
                ImportContactRequest(
                    encryptionPublicKey = testKey(seed = 53),
                    signingPublicKey = testKey(seed = 153),
                    displayName = "Verified Person",
                    phoneNumber = "+49151515151",
                    identityImportTrust = IdentityImportTrust.VERIFIED_IN_PERSON
                )
            ).getOrThrow()
            val initialIdentity = requireSparrowIdentity(imported.sparrowIdentity)
            val preferredPhoneId = imported.preferredPhoneNumberId

            val updated = repository.updateContactDetails(
                contactId = imported.id,
                displayName = "Renamed Person",
                phoneNumber = null
            ).getOrThrow()
            assertEquals("Renamed Person", updated.displayName)
            assertEquals(preferredPhoneId, updated.preferredPhoneNumberId)
            assertEquals(
                ContactVerificationStatus.VERIFIED,
                storedIdentity(updated.id).verificationStatus
            )
            assertContentEquals(
                initialIdentity.signingPublicKey,
                storedIdentity(updated.id).signingPublicKey
            )

            // Selecting an existing phone number uses a Contacts-only lookup and then
            // enriches the public result without altering the pinned Identity row.
            val byNumber = repository.findOrCreateByPhoneNumber("+49151515151").getOrThrow()
            assertEquals(imported.id, byNumber.id)
            assertContentEquals(
                initialIdentity.signingPublicKey,
                storedIdentity(byNumber.id).signingPublicKey
            )
        }

    @Test
    fun deviceContactLookupWithoutIdentityPreservesLinkedRecord() = runBlocking {
        val imported = repository.importDeviceContact(
            ImportDeviceContactRequest(
                deviceContactId = "device-contact-contact-only-lookup",
                displayName = "Linked Person",
                phoneNumbers = listOf(devicePhoneNumber(value = "+49161616161"))
            )
        ).getOrThrow()
        val record = database.contactDao()
            .findContactWithPhoneNumbersByDeviceContactId("device-contact-contact-only-lookup")
        assertEquals(imported.id, assertNotNull(record).contact.id)
        assertEquals(1, record.phoneNumbers.size)
        assertEquals("+49161616161", record.phoneNumbers.single().value)

        val missing = repository.updateDeviceContactLinkStatus(
            deviceContactId = "device-contact-contact-only-lookup",
            status = DeviceContactLinkStatus.MISSING
        ).getOrThrow()
        assertEquals(imported.id, assertNotNull(missing).id)
        assertEquals(DeviceContactLinkStatus.MISSING, missing.deviceContactLinkStatus)
        assertNull(missing.sparrowIdentity)
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

            assertNull(actual = loadedContact.sparrowIdentity)
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

    private suspend fun storedIdentity(contactId: String): SparrowIdentity {
        val row = assertNotNull(database.remoteIdentityDao().findByPeerId(contactId))
        return SparrowIdentity(
            encryptionPublicKey = row.encryptionPublicKey.copyOf(),
            signingPublicKey = row.signingPublicKey.copyOf(),
            verificationStatus = ContactVerificationStatus.valueOf(row.verificationStatus),
            verifiedByContact = row.verifiedByContact,
            locallyImported = row.locallyImported,
            keyExchangeStatus = com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus.valueOf(row.keyExchangeStatus),
            updatedAtEpochMilliseconds = row.updatedAtEpochMilliseconds
        )
    }

    private fun requireSparrowIdentity(sparrowIdentity: SparrowIdentity?): SparrowIdentity =
        assertNotNull(
            actual = sparrowIdentity,
            message = "Expected contact to have a Sparrow identity"
        )

    private fun testKey(seed: Int): ByteArray =
        ByteArray(size = 32) { index ->
            (seed + index).mod(256).toByte()
        }
}
