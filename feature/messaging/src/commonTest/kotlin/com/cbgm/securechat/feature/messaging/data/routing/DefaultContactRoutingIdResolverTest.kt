package com.cbgm.securechat.feature.messaging.data.routing

import com.cbgm.securechat.data.database.dao.ContactRoutingIdDao
import com.cbgm.securechat.data.database.entity.ContactRoutingIdEntity
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.securechat.feature.contacts.domain.model.ImportDeviceContactRequest
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.model.SecureChatIdentity
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContactUseCase
import com.cbgm.securechat.feature.transport.routing.RoutingIdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultContactRoutingIdResolverTest {
    @Test
    fun currentPersistedMappingIsReturnedAfterCheckingTheSigningIdentity() =
        runTest {
            val contactRepository = FakeContactRepository(contact = createContact())
            val routingIdDao =
                FakeContactRoutingIdDao(
                    routingIdByContactId = mutableMapOf("contact-1" to "stored-routing-id")
                )
            val routingIdGenerator = RecordingRoutingIdGenerator(result = "stored-routing-id")
            val resolver =
                DefaultContactRoutingIdResolver(
                    getContact = GetContactUseCase(contactRepository),
                    contactRoutingIdDao = routingIdDao,
                    routingIdGenerator = routingIdGenerator
                )

            val routingId = resolver.resolve("contact-1").getOrThrow()

            assertEquals("stored-routing-id", routingId)
            assertEquals(1, contactRepository.getContactCallCount)
            assertEquals(1, routingIdGenerator.callCount)
            assertTrue(routingIdDao.upsertedEntities.isEmpty())
        }

    @Test
    fun signingIdentityIsDerivedWithoutReplacingBootstrapMapping() =
        runTest {
            val contact = createContact(signingPublicKey = byteArrayOf(1, 2, 3))
            val routingIdDao = FakeContactRoutingIdDao()
            val routingIdGenerator = RecordingRoutingIdGenerator()
            val resolver =
                DefaultContactRoutingIdResolver(
                    getContact = GetContactUseCase(FakeContactRepository(contact)),
                    contactRoutingIdDao = routingIdDao,
                    routingIdGenerator = routingIdGenerator
                )

            val routingId = resolver.resolve("contact-1").getOrThrow()

            assertEquals("derived-routing-id", routingId)
            assertTrue(byteArrayOf(1, 2, 3).contentEquals(routingIdGenerator.signingPublicKey))
            assertTrue(routingIdDao.upsertedEntities.isEmpty())
        }

    @Test
    fun stalePhoneDerivedMappingIsPreservedForHandshakeReplies() =
        runTest {
            val routingIdDao =
                FakeContactRoutingIdDao(
                    routingIdByContactId = mutableMapOf("contact-1" to "scphone1_legacy")
                )
            val routingIdGenerator = RecordingRoutingIdGenerator()
            val resolver =
                DefaultContactRoutingIdResolver(
                    getContact = GetContactUseCase(FakeContactRepository(createContact())),
                    contactRoutingIdDao = routingIdDao,
                    routingIdGenerator = routingIdGenerator
                )

            val routingId = resolver.resolve("contact-1").getOrThrow()

            assertEquals("derived-routing-id", routingId)
            assertTrue(routingIdDao.upsertedEntities.isEmpty())
            assertEquals("scphone1_legacy", routingIdDao.findRoutingIdByContactId("contact-1"))
        }

    @Test
    fun contactWithoutSigningIdentityCannotCreateRoutingMapping() =
        runTest {
            val routingIdDao = FakeContactRoutingIdDao()
            val routingIdGenerator = RecordingRoutingIdGenerator()
            val resolver =
                DefaultContactRoutingIdResolver(
                    getContact =
                        GetContactUseCase(
                            FakeContactRepository(
                                createContact(signingPublicKey = null)
                            )
                        ),
                    contactRoutingIdDao = routingIdDao,
                    routingIdGenerator = routingIdGenerator
                )

            val result = resolver.resolve("contact-1")

            assertTrue(result.isFailure)
            assertEquals(0, routingIdGenerator.callCount)
            assertTrue(routingIdDao.upsertedEntities.isEmpty())
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

    private class FakeContactRoutingIdDao(
        private val contactIdByRoutingId: MutableMap<String, String> = mutableMapOf(),
        private val routingIdByContactId: MutableMap<String, String> = mutableMapOf()
    ) : ContactRoutingIdDao {
        val upsertedEntities = mutableListOf<ContactRoutingIdEntity>()

        override suspend fun findContactIdByRoutingId(routingId: String): String? = contactIdByRoutingId[routingId]

        override suspend fun findRoutingIdByContactId(contactId: String): String? = routingIdByContactId[contactId]

        override suspend fun deleteOtherContactMapping(
            routingId: String,
            contactId: String
        ) {
            val owner = contactIdByRoutingId[routingId]
            if (owner != null && owner != contactId) {
                contactIdByRoutingId.remove(routingId)
                routingIdByContactId.remove(owner)
            }
        }

        override suspend fun upsert(entity: ContactRoutingIdEntity) {
            upsertedEntities += entity
            contactIdByRoutingId[entity.routingId] = entity.contactId
            routingIdByContactId[entity.contactId] = entity.routingId
        }
    }

    private class RecordingRoutingIdGenerator(
        private val result: String = "derived-routing-id"
    ) : RoutingIdGenerator {
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
