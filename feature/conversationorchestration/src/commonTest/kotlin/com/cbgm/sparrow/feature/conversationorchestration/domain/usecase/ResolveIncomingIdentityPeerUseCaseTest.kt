package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.identity.IdentityPeerMerge
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityPeerRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.identity.InspectContactPeerUseCase
import com.cbgm.sparrow.feature.conversationorchestration.domain.error.RemoteIdentityReplacementRequiredException
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
        )
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
        val result = resolve(contacts, identities)("incoming", null, byteArrayOf(1), byteArrayOf(2))
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
                resolve(contacts, identities)("incoming", null, byteArrayOf(9), byteArrayOf(2))
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
            resolve(contacts, identities)("incoming", "+491234567890", byteArrayOf(9), byteArrayOf(8))
        }
        assertEquals("existing", conflict.peerId)
    }

    @Test
    fun evenUnverifiedPhoneContactRequiresExplicitIdentityReplacement(): Unit = runBlocking {
        val contacts = FakeContacts(existingPeerIds = setOf("incoming", "existing"), phonePeerId = "existing")
        val identities = FakeIdentities(records = mapOf("existing" to identity("existing")))

        assertFailsWith<RemoteIdentityReplacementRequiredException> {
            resolve(contacts, identities)("incoming", "+491234567890", byteArrayOf(9), byteArrayOf(8))
        }
    }

    @Test
    fun knownPhoneContactWithSameKeysKeepsItsStableContactId() = runBlocking {
        val contacts = FakeContacts(existingPeerIds = setOf("incoming", "existing"), phonePeerId = "existing")
        val identities = FakeIdentities(
            keyPeerId = "existing",
            records = mapOf("existing" to identity("existing", KeyExchangeStatus.MUTUAL))
        )
        val resolution = resolve(contacts, identities)("incoming", "+491234567890", byteArrayOf(1), byteArrayOf(2))
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
        val result = resolve(contacts, identities)("incoming", null, byteArrayOf(9), byteArrayOf(2))
        assertEquals("existing", result.peerId)
        assertEquals(emptyList(), result.merges)
    }

    @Test
    fun phonePeerTakesPrecedenceOverIdentityPeer() = runBlocking {
        val contacts = FakeContacts(existingPeerIds = setOf("incoming", "phone", "identity"), phonePeerId = "phone")
        val result = resolve(contacts, FakeIdentities(keyPeerId = "identity"))(
            "incoming",
            "+491234567890",
            byteArrayOf(1),
            byteArrayOf(2)
        )
        assertEquals("phone", result.peerId)
    }

    private fun resolve(contacts: FakeContacts, identities: FakeIdentities) =
        ResolveIncomingIdentityPeerUseCase(
            InspectContactPeerUseCase(contacts),
            FindRemoteIdentityPeerIdUseCase(identities),
            GetRemoteIdentityUseCase(identities)
        )

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
        val phonePeerId: String? = null
    ) : IdentityPeerRepository {
        override suspend fun getDisplayName(peerId: String): String? = null

        override suspend fun containsPeer(peerId: String): Boolean = peerId in existingPeerIds

        override suspend fun isKnownContact(peerId: String): Boolean = false

        override suspend fun findEquivalentPhonePeerId(phoneNumber: String): String? = phonePeerId

        override suspend fun canMergeRoutingDuplicate(peerId: String, remotePhoneNumber: String?): Boolean =
            peerId in existingPeerIds

        override suspend fun applyMerge(merge: IdentityPeerMerge) = Unit

        override suspend fun updateIncomingMetadata(peerId: String, phoneNumber: String, updatedAtEpochMilliseconds: Long) = Unit
    }
}
