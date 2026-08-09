package com.cbgm.securechat.server.security

import com.cbgm.securechat.server.protocol.NodeCapability
import com.cbgm.securechat.server.protocol.NodeDirectory
import com.cbgm.securechat.server.protocol.SecureChatNodeDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProtocolSignaturesTest {
    @Test
    fun signedDescriptorCanBeVerifiedAndCannotBeModified() {
        val identity = NodeIdentity.generate()
        val unsigned =
            SecureChatNodeDescriptor(
                nodeId = identity.nodeId,
                clientEndpoint = "ws://node-a:8080/relay",
                federationEndpoint = "http://node-a:8081",
                mailboxEndpoint = "http://node-a:8082",
                identityPublicKey = identity.encodedPublicKey,
                protocolVersions = setOf(1),
                capabilities = NodeCapability.entries.toSet(),
                validUntilEpochMilliseconds = Long.MAX_VALUE,
                signature = byteArrayOf()
            )
        val signed = ProtocolSignatures.signDescriptor(unsigned, identity)

        assertTrue(ProtocolSignatures.verifyDescriptor(signed))
        assertFalse(ProtocolSignatures.verifyDescriptor(signed.copy(clientEndpoint = "ws://attacker")))
    }

    @Test
    fun registryAuthorityCertificateAllowsDirectorySigningKeyRotation() {
        val rootIdentity = NodeIdentity.generate()
        val firstAuthority = NodeIdentity.generate()
        val secondAuthority = NodeIdentity.generate()
        val directory =
            NodeDirectory(
                generatedAtEpochMilliseconds = 1_000L,
                validUntilEpochMilliseconds = 2_000L,
                nodes = emptyList()
            )
        val firstCertificate =
            ProtocolSignatures.signAuthorityCertificate(
                rootIdentity = rootIdentity,
                authorityIdentity = firstAuthority,
                keyVersion = 1L,
                validFromEpochMilliseconds = 500L,
                validUntilEpochMilliseconds = 5_000L
            )
        val secondCertificate =
            ProtocolSignatures.signAuthorityCertificate(
                rootIdentity = rootIdentity,
                authorityIdentity = secondAuthority,
                keyVersion = 2L,
                validFromEpochMilliseconds = 500L,
                validUntilEpochMilliseconds = 5_000L
            )
        val firstDirectory =
            ProtocolSignatures.signDirectory(
                directory = directory,
                identity = firstAuthority,
                certificate = firstCertificate
            )
        val secondDirectory =
            ProtocolSignatures.signDirectory(
                directory = directory,
                identity = secondAuthority,
                certificate = secondCertificate
            )

        assertTrue(ProtocolSignatures.verifyAuthorityCertificate(firstCertificate))
        assertTrue(ProtocolSignatures.verifyAuthorityCertificate(secondCertificate))
        assertTrue(ProtocolSignatures.verifyDirectory(firstDirectory))
        assertTrue(ProtocolSignatures.verifyDirectory(secondDirectory))
        assertEquals(rootIdentity.nodeId, firstCertificate.rootNodeId)
        assertEquals(rootIdentity.nodeId, secondCertificate.rootNodeId)
        assertFalse(firstDirectory.authorityNodeId == secondDirectory.authorityNodeId)
    }

    @Test
    fun rawEd25519ClientPublicKeyCanBeVerified() {
        val identity = NodeIdentity.generate()
        val content = "signed-client-route".encodeToByteArray()
        val signature = Signatures.sign(content, identity.privateKey)
        val rawPublicKey = identity.encodedPublicKey.takeLast(32).toByteArray()

        assertTrue(
            Signatures.verify(
                content = content,
                signature = signature,
                publicKey = Signatures.decodePublicKey(rawPublicKey)
            )
        )
    }

    @Test
    fun routingIdIsBoundToTheExactClientPublicKey() {
        val firstKey = ByteArray(32) { index -> index.toByte() }
        val secondKey = firstKey.copyOf().also { key -> key[0] = 99 }
        val routingId = ClientRoutingIds.fromSigningPublicKey(firstKey)

        assertTrue(routingId.startsWith("scrouting1_"))
        assertTrue(ClientRoutingIds.matchesSigningPublicKey(routingId, firstKey))
        assertFalse(ClientRoutingIds.matchesSigningPublicKey(routingId, secondKey))
    }

    @Test
    fun routingIdEncodingMatchesTheMultiplatformClient() {
        assertEquals(
            "scrouting1_A5BYxvLAy0ksUzsKTRTvd8wPeKvMztUofYShogEc-4E",
            ClientRoutingIds.fromSigningPublicKey(byteArrayOf(1, 2, 3))
        )
    }
}
