package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.identity.IdentityPeerMerge
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityPeerRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.identity.InspectContactPeerUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.error.RemoteIdentityReplacementRequiredException
import com.cbgm.sparrow.feature.identity.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.identity.domain.model.RemotePeerIdentity
import com.cbgm.sparrow.feature.identity.domain.repository.RemoteIdentityReadRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.FindRemoteIdentityPeerIdUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResolveIncomingIdentityPeerUseCaseTest {
    @Test
    fun orphanedIdentityDoesNotBecomeAnUnknownContactId() = runBlocking {
        val contacts = FakeContacts(existingPeerIds = setOf("incoming"))
        val result = resolve(contacts, FakeIdentities(keyPeerId = "orphan"))(
            "incoming",
            null,
            byteArrayOf(1),
            byteArrayOf(2)
        ).getOrThrow()
        assertEquals("incoming", result.peerId)
        assertFalse(result.wasKnownPeer)
    }

    @Test
    fun locallyImportedIdentityMakesAContactKnown() = runBlocking {
        val contacts = FakeContacts(existingPeerIds = setOf("incoming", "existing"))
        val identities = FakeIdentities(
            keyPeerId = "existing",
            records = mapOf("existing" to identity("existing", locallyImported = true))
        )
        val result = resolve(contacts, identities)("incoming", null, byteArrayOf(1), byteArrayOf(2)).getOrThrow()
        assertEquals("existing", result.peerId)
        assertTrue(result.wasKnownPeer)
        assertEquals(listOf(IdentityPeerMerge("incoming", "existing", true)), result.merges)
    }

    @Test
    fun pinnedIdentityCannotBeSilentlyReplacedDuringIncomingResolution() = runBlocking {
        for ((exchange, verified) in listOf(
            KeyExchangeStatus.MUTUAL to ContactVerificationStatus.UNVERIFIED,
            KeyExchangeStatus.ONE_WAY to ContactVerificationStatus.VERIFIED
        )) {
            val contacts = FakeContacts(existingPeerIds = setOf("incoming", "existing"))
            val identities = FakeIdentities(
                keyPeerId = "existing",
                records = mapOf("existing" to identity("existing", exchange, verified))
            )
            assertFailsWith<RemoteIdentityReplacementRequiredException> {
                resolve(contacts, identities)("incoming", null, byteArrayOf(9), byteArrayOf(2)).getOrThrow()
            }
        }
    }

    @Test
    fun previouslyMutualPhoneContactDoesNotMergeWithNewInstallation() = runBlocking {
        val contacts = FakeContacts(existingPeerIds = setOf("incoming", "existing"), phonePeerId = "existing")
        val identities = FakeIdentities(
            records = mapOf("existing" to identity("existing", KeyExchangeStatus.MUTUAL))
        )

        val conflict = assertFailsWith<RemoteIdentityReplacementRequiredException> {
            resolve(contacts, identities)("incoming", "+491234567890", byteArrayOf(9), byteArrayOf(8)).getOrThrow()
        }
        assertEquals("existing", conflict.peerId)
    }

    @Test
    fun evenUnverifiedPhoneContactRequiresExplicitIdentityReplacement(): Unit = runBlocking {
        val contacts = FakeContacts(existingPeerIds = setOf("incoming", "existing"), phonePeerId = "existing")
        val identities = FakeIdentities(records = mapOf("existing" to identity("existing")))

        assertFailsWith<RemoteIdentityReplacementRequiredException> {
            resolve(contacts, identities)("incoming", "+491234567890", byteArrayOf(9), byteArrayOf(8)).getOrThrow()
        }
    }

    @Test
    fun knownPhoneContactWithSameKeysKeepsItsStableContactId() = runBlocking {
        val contacts = FakeContacts(existingPeerIds = setOf("incoming", "existing"), phonePeerId = "existing")
        val identities = FakeIdentities(
            keyPeerId = "existing",
            records = mapOf("existing" to identity("existing", KeyExchangeStatus.MUTUAL))
        )
        val resolution = resolve(contacts, identities)("incoming", "+491234567890", byteArrayOf(1), byteArrayOf(2)).getOrThrow()
        assertEquals("existing", resolution.peerId)
        assertEquals(listOf(IdentityPeerMerge("incoming", "existing", true)), resolution.merges)
    }

    @Test
    fun mismatchedExistingResolvedPeerIdentityPreventsMerge() = runBlocking {
        val contacts = FakeContacts(existingPeerIds = setOf("incoming", "existing"))
        val identities = FakeIdentities(
            keyPeerId = "existing",
            records = mapOf("incoming" to identity("incoming"))
        )
        val result = resolve(contacts, identities)("incoming", null, byteArrayOf(9), byteArrayOf(2)).getOrThrow()
        assertEquals("existing", result.peerId)
        assertEquals(emptyList(), result.merges)
    }

    @Test
    fun keysTakePrecedenceOverTheClaimedPhoneNumber() = runBlocking {
        val contacts = FakeContacts(
            existingPeerIds = setOf("incoming", "phone", "identity"),
            phonePeerId = "phone",
            linkablePeerIds = setOf("phone")
        )
        val result = resolve(contacts, FakeIdentities(keyPeerId = "identity"))(
            "incoming",
            "+491234567890",
            byteArrayOf(1),
            byteArrayOf(2)
        ).getOrThrow()
        assertEquals("identity", result.peerId)
        assertEquals("phone", result.phoneContactPeerId)
    }

    @Test
    fun claimedPhoneNumberOnlyOffersThePhoneBookContactForLinking() = runBlocking {
        val contacts = FakeContacts(
            existingPeerIds = setOf("incoming", "alice"),
            phonePeerId = "alice",
            knownPeerIds = setOf("alice"),
            linkablePeerIds = setOf("alice")
        )
        val result = resolve(contacts, FakeIdentities())(
            "incoming",
            "+491234567890",
            byteArrayOf(1),
            byteArrayOf(2)
        ).getOrThrow()
        assertEquals("incoming", result.peerId)
        assertFalse(result.wasKnownPeer)
        assertEquals(emptyList(), result.merges)
        assertEquals("alice", result.phoneContactPeerId)
    }

    @Test
    fun phoneContactInUseIsNotOfferedForLinking() = runBlocking {
        val contacts = FakeContacts(existingPeerIds = setOf("incoming", "alice"), phonePeerId = "alice")
        val result = resolve(contacts, FakeIdentities())(
            "incoming",
            "+491234567890",
            byteArrayOf(1),
            byteArrayOf(2)
        ).getOrThrow()
        assertEquals("incoming", result.peerId)
        assertNull(result.phoneContactPeerId)
    }

    @Test
    fun contactKnownWithoutMatchingKeysDoesNotMakeTheSenderKnown() = runBlocking {
        val contacts = FakeContacts(existingPeerIds = setOf("incoming"), knownPeerIds = setOf("incoming"))
        val result = resolve(contacts, FakeIdentities())(
            "incoming",
            "+491234567890",
            byteArrayOf(1),
            byteArrayOf(2)
        ).getOrThrow()
        assertFalse(result.wasKnownPeer)
    }

    @Test
    fun knownContactProvenByKeysStaysKnown() = runBlocking {
        val contacts = FakeContacts(existingPeerIds = setOf("incoming", "bob"), knownPeerIds = setOf("bob"))
        val identities = FakeIdentities(keyPeerId = "bob", records = mapOf("bob" to identity("bob")))
        val result = resolve(contacts, identities)("incoming", null, byteArrayOf(1), byteArrayOf(2)).getOrThrow()
        assertEquals("bob", result.peerId)
        assertTrue(result.wasKnownPeer)
    }

    @Test
    fun claimingABlockedContactsNumberBlocksTheSender() = runBlocking {
        val contacts = FakeContacts(
            existingPeerIds = setOf("incoming", "carol"),
            phonePeerId = "carol",
            linkablePeerIds = setOf("carol")
        )
        val result = resolve(contacts, FakeIdentities(), blocked = setOf("carol"))(
            "incoming",
            "+491234567890",
            byteArrayOf(1),
            byteArrayOf(2)
        ).getOrThrow()
        assertEquals("incoming", result.peerId)
        assertTrue(result.inheritsBlock)
        assertNull(result.phoneContactPeerId)
    }

    @Test
    fun keysOfAKnownContactOutweighABlockedClaim() = runBlocking {
        val contacts = FakeContacts(existingPeerIds = setOf("incoming", "bob", "carol"), phonePeerId = "carol")
        val identities = FakeIdentities(keyPeerId = "bob", records = mapOf("bob" to identity("bob")))
        val result = resolve(contacts, identities, blocked = setOf("carol"))(
            "incoming",
            "+491234567890",
            byteArrayOf(1),
            byteArrayOf(2)
        ).getOrThrow()
        assertEquals("bob", result.peerId)
        assertFalse(result.inheritsBlock)
    }

    @Test
    fun mergingABlockedContactKeepsTheBlock() = runBlocking {
        val contacts = FakeContacts(existingPeerIds = setOf("incoming", "existing"))
        val identities = FakeIdentities(keyPeerId = "existing")
        val result = resolve(contacts, identities, blocked = setOf("incoming"))(
            "incoming",
            null,
            byteArrayOf(1),
            byteArrayOf(2)
        ).getOrThrow()
        assertEquals(listOf(IdentityPeerMerge("incoming", "existing", true)), result.merges)
        assertTrue(result.inheritsBlock)
    }

    private fun resolve(contacts: FakeContacts, identities: FakeIdentities, blocked: Set<String> = emptySet()) =
        ResolveIncomingIdentityPeerUseCase(
            InspectContactPeerUseCase(contacts),
            FindRemoteIdentityPeerIdUseCase(identities),
            GetRemoteIdentityUseCase(identities),
            FakeBlocklist(blocked)
        )

    private class FakeBlocklist(
        val blocked: Set<String>
    ) : ContactBlocklistRepository {
        override suspend fun isBlocked(contactId: String): Result<Boolean> = Result.success(contactId in blocked)

        override fun observeBlockUnknownContactInvites(): Flow<Boolean> = emptyFlow()

        override suspend fun getBlockUnknownContactInvites(): Result<Boolean> = Result.success(false)

        override suspend fun setBlockUnknownContactInvites(enabled: Boolean): Result<Unit> = Result.success(Unit)

        override fun observeBlockedContactIds(): Flow<Set<String>> = emptyFlow()

        override suspend fun getBlockedContactIds(): Result<Set<String>> = Result.success(blocked)

        override suspend fun block(contactId: String): Result<Unit> = Result.success(Unit)

        override suspend fun unblock(contactId: String): Result<Unit> = Result.success(Unit)
    }

    private fun identity(
        id: String,
        exchange: KeyExchangeStatus = KeyExchangeStatus.ONE_WAY,
        verification: ContactVerificationStatus = ContactVerificationStatus.UNVERIFIED,
        locallyImported: Boolean = false
    ) = RemotePeerIdentity(id, byteArrayOf(1), byteArrayOf(2), verification, exchange, false, locallyImported, 1L)

    private class FakeIdentities(
        val keyPeerId: String? = null,
        val records: Map<String, RemotePeerIdentity> = emptyMap()
    ) : RemoteIdentityReadRepository {
        override suspend fun get(peerId: String): Result<RemotePeerIdentity?> = Result.success(records[peerId])

        override suspend fun findPeerIdBySigningPublicKey(signingPublicKey: ByteArray): Result<String?> =
            Result.success(keyPeerId)

        override fun observeAll(): Flow<List<RemotePeerIdentity>> = emptyFlow()
    }

    private class FakeContacts(
        val existingPeerIds: Set<String>,
        val phonePeerId: String? = null,
        val knownPeerIds: Set<String> = emptySet(),
        val linkablePeerIds: Set<String> = emptySet()
    ) : IdentityPeerRepository {
        override suspend fun getDisplayName(peerId: String): Result<String?> = Result.success(null)

        override suspend fun containsPeer(peerId: String): Result<Boolean> =
            Result.success(peerId in existingPeerIds)

        override suspend fun isKnownContact(peerId: String): Result<Boolean> = Result.success(peerId in knownPeerIds)

        override suspend fun findEquivalentPhonePeerId(phoneNumber: String): Result<String?> =
            Result.success(phonePeerId)

        override suspend fun canMergeRoutingDuplicate(
            peerId: String,
            remotePhoneNumber: String?
        ): Result<Boolean> = Result.success(peerId in existingPeerIds)

        override suspend fun applyMerge(merge: IdentityPeerMerge): Result<Unit> = Result.success(Unit)

        override suspend fun isLinkablePhoneContact(peerId: String): Result<Boolean> =
            Result.success(peerId in linkablePeerIds)

        override suspend fun linkPhoneContact(
            peerId: String,
            phoneContactPeerId: String,
            updatedAtEpochMilliseconds: Long
        ): Result<Unit> = Result.success(Unit)

        override suspend fun updateIncomingMetadata(
            peerId: String,
            phoneNumber: String,
            storePhoneNumber: Boolean,
            updatedAtEpochMilliseconds: Long
        ): Result<Unit> = Result.success(Unit)
    }
}
