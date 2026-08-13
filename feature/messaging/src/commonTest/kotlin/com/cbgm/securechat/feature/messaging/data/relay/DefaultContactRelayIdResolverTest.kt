package com.cbgm.securechat.feature.messaging.data.relay

import com.cbgm.securechat.data.database.dao.ContactRelayIdDao
import com.cbgm.securechat.data.database.entity.ContactRelayIdEntity
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.securechat.feature.contacts.domain.model.ImportDeviceContactRequest
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.model.SecureChatIdentity
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.securechat.feature.transport.relay.identity.RelayIdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultContactRelayIdResolverTest {
    @Test
    fun currentPersistedMappingIsReturnedAfterCheckingTheSigningIdentity() =
        runTest {
            val contactRepository = FakeContactRepository(contact = createContact())
            val relayIdDao =
                FakeContactRelayIdDao(
                    relayIdByContactId = mutableMapOf("contact-1" to "stored-relay-id")
                )
            val relayIdGenerator = RecordingRelayIdGenerator(result = "stored-relay-id")
            val resolver =
                DefaultContactRelayIdResolver(
                    getContact = GetContactUseCase(contactRepository),
                    contactRelayIdDao = relayIdDao,
                    relayIdGenerator = relayIdGenerator
                )

            val relayId = resolver.resolve("contact-1").getOrThrow()

            assertEquals("stored-relay-id", relayId)
            assertEquals(1, contactRepository.getContactCallCount)
            assertEquals(1, relayIdGenerator.callCount)
            assertTrue(relayIdDao.upsertedEntities.isEmpty())
        }

    @Test
    fun signingIdentityIsDerivedWithoutReplacingBootstrapMapping() =
        runTest {
            val contact = createContact(signingPublicKey = byteArrayOf(1, 2, 3))
            val relayIdDao = FakeContactRelayIdDao()
            val relayIdGenerator = RecordingRelayIdGenerator()
            val resolver =
                DefaultContactRelayIdResolver(
                    getContact = GetContactUseCase(FakeContactRepository(contact)),
                    contactRelayIdDao = relayIdDao,
                    relayIdGenerator = relayIdGenerator
                )

            val relayId = resolver.resolve("contact-1").getOrThrow()

            assertEquals("derived-relay-id", relayId)
            assertTrue(byteArrayOf(1, 2, 3).contentEquals(relayIdGenerator.signingPublicKey))
            assertTrue(relayIdDao.upsertedEntities.isEmpty())
        }

    @Test
    fun stalePhoneDerivedMappingIsPreservedForHandshakeReplies() =
        runTest {
            val relayIdDao =
                FakeContactRelayIdDao(
                    relayIdByContactId = mutableMapOf("contact-1" to "scphone1_legacy")
                )
            val relayIdGenerator = RecordingRelayIdGenerator()
            val resolver =
                DefaultContactRelayIdResolver(
                    getContact = GetContactUseCase(FakeContactRepository(createContact())),
                    contactRelayIdDao = relayIdDao,
                    relayIdGenerator = relayIdGenerator
                )

            val relayId = resolver.resolve("contact-1").getOrThrow()

            assertEquals("derived-relay-id", relayId)
            assertTrue(relayIdDao.upsertedEntities.isEmpty())
            assertEquals("scphone1_legacy", relayIdDao.findRelayIdByContactId("contact-1"))
        }

    @Test
    fun contactWithoutSigningIdentityCannotCreateRelayMapping() =
        runTest {
            val relayIdDao = FakeContactRelayIdDao()
            val relayIdGenerator = RecordingRelayIdGenerator()
            val resolver =
                DefaultContactRelayIdResolver(
                    getContact =
                        GetContactUseCase(
                            FakeContactRepository(
                                createContact(signingPublicKey = null)
                            )
                        ),
                    contactRelayIdDao = relayIdDao,
                    relayIdGenerator = relayIdGenerator
                )

            val result = resolver.resolve("contact-1")

            assertTrue(result.isFailure)
            assertEquals(0, relayIdGenerator.callCount)
            assertTrue(relayIdDao.upsertedEntities.isEmpty())
        }

    private fun createContact(
        signingPublicKey: ByteArray? = byteArrayOf(1, 2, 3)
    ): Contact =
        Contact(
            id = "contact-1",
            displayName = "Alice",
            phoneNumbers = emptyList(),
            preferredPhoneNumberId = null,
            deviceContactId = null,
            deviceContactLinkStatus = DeviceContactLinkStatus.NOT_LINKED,
            secureChatIdentity =
                signingPublicKey?.let { key ->
                    SecureChatIdentity(
                        encryptionPublicKey = byteArrayOf(4, 5, 6),
                        signingPublicKey = key,
                        verificationStatus = ContactVerificationStatus.UNVERIFIED,
                        keyExchangeStatus = KeyExchangeStatus.MUTUAL,
                        updatedAtEpochMilliseconds = 1L
                    )
                },
            createdAtEpochMilliseconds = 1L,
            updatedAtEpochMilliseconds = 1L
        )

    private class FakeContactRelayIdDao(
        private val contactIdByRelayId: MutableMap<String, String> = mutableMapOf(),
        private val relayIdByContactId: MutableMap<String, String> = mutableMapOf()
    ) : ContactRelayIdDao {
        val upsertedEntities = mutableListOf<ContactRelayIdEntity>()

        override suspend fun findContactIdByRelayId(relayId: String): String? = contactIdByRelayId[relayId]

        override suspend fun findRelayIdByContactId(contactId: String): String? = relayIdByContactId[contactId]

        override suspend fun deleteOtherContactMapping(
            relayId: String,
            contactId: String
        ) {
            val owner = contactIdByRelayId[relayId]
            if (owner != null && owner != contactId) {
                contactIdByRelayId.remove(relayId)
                relayIdByContactId.remove(owner)
            }
        }

        override suspend fun upsert(entity: ContactRelayIdEntity) {
            upsertedEntities += entity
            contactIdByRelayId[entity.relayId] = entity.contactId
            relayIdByContactId[entity.contactId] = entity.relayId
        }
    }

    private class RecordingRelayIdGenerator(
        private val result: String = "derived-relay-id"
    ) : RelayIdGenerator {
        var callCount: Int = 0
        var signingPublicKey: ByteArray? = null

        override fun deriveFromPhoneNumber(phoneNumber: String): Result<String> =
            Result.success("scphone1_test")

        override fun deriveFromSigningPublicKey(signingPublicKey: ByteArray): Result<String> {
            callCount += 1
            this.signingPublicKey = signingPublicKey
            return Result.success(result)
        }
    }

    private class FakeContactRepository(
        private val contact: Contact?
    ) : ContactRepository {
        var getContactCallCount: Int = 0

        override suspend fun importDeviceContact(
            request: ImportDeviceContactRequest
        ): Result<Contact> = Result.failure(UnsupportedOperationException())

        override suspend fun importContact(
            request: ImportContactRequest
        ): Result<Contact> = Result.failure(UnsupportedOperationException())

        override suspend fun getContact(contactId: String): Result<Contact?> {
            getContactCallCount += 1
            return Result.success(contact?.takeIf { it.id == contactId })
        }

        override suspend fun findBySigningPublicKey(
            signingPublicKey: ByteArray
        ): Result<Contact?> = Result.success(null)

        override suspend fun findOrCreateByPhoneNumber(
            phoneNumber: String
        ): Result<Contact> = Result.failure(UnsupportedOperationException())

        override fun observeContacts(): Flow<List<Contact>> = flowOf(listOfNotNull(contact))

        override suspend fun updateContactDetails(
            contactId: String,
            displayName: String?,
            phoneNumber: String?
        ): Result<Contact> = Result.failure(UnsupportedOperationException())

        override suspend fun markVerified(
            contactId: String
        ): Result<Contact> = Result.failure(UnsupportedOperationException())

        override suspend fun markKeyExchangeMutual(
            contactId: String
        ): Result<Contact> = Result.failure(UnsupportedOperationException())

        override suspend fun resetKeyExchange(
            contactId: String
        ): Result<Contact> = Result.failure(UnsupportedOperationException())

        override suspend fun updateDeviceContactLinkStatus(
            deviceContactId: String,
            status: DeviceContactLinkStatus
        ): Result<Contact?> = Result.failure(UnsupportedOperationException())
    }
}
