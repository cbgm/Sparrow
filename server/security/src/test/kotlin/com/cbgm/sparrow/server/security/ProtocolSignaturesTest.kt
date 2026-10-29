package com.cbgm.sparrow.server.security

import com.cbgm.sparrow.server.protocol.NodeCapability
import com.cbgm.sparrow.server.protocol.NodeDirectory
import com.cbgm.sparrow.server.protocol.SparrowNodeDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProtocolSignaturesTest {
    @Test
    fun signedDescriptorCanBeVerifiedAndCannotBeModified() {
        val identity = NodeIdentity.generate()
        val unsigned =
            SparrowNodeDescriptor(
                nodeId = identity.nodeId,
                clientEndpoint = "ws://node-a:8080/v1/gateway",
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
        val authorityIdentity = NodeIdentity.generate()
        val firstSigner = NodeIdentity.generate()
        val secondSigner = NodeIdentity.generate()
        val directory =
            NodeDirectory(
                generatedAtEpochMilliseconds = 1_000L,
                validUntilEpochMilliseconds = 2_000L,
                nodes = emptyList()
            )
        val authorityCertificate =
            RegistryCertificateSignatures.signAuthorityCertificate(
                rootIdentity = rootIdentity,
                authorityIdentity = authorityIdentity,
                keyVersion = 1L,
                validFromEpochMilliseconds = 500L,
                validUntilEpochMilliseconds = 10_000L
            )
        val firstSigningCertificate =
            RegistryCertificateSignatures.signSigningCertificate(
                authorityIdentity = authorityIdentity,
                signingIdentity = firstSigner,
                keyVersion = 1L,
                validFromEpochMilliseconds = 500L,
                validUntilEpochMilliseconds = 5_000L
            )
        val secondSigningCertificate =
            RegistryCertificateSignatures.signSigningCertificate(
                authorityIdentity = authorityIdentity,
                signingIdentity = secondSigner,
                keyVersion = 2L,
                validFromEpochMilliseconds = 500L,
                validUntilEpochMilliseconds = 5_000L
            )
        val firstDirectory =
            ProtocolSignatures.signDirectory(
                directory = directory,
                identity = firstSigner,
                certificate = authorityCertificate,
                signingCertificate = firstSigningCertificate
            )
        val secondDirectory =
            ProtocolSignatures.signDirectory(
                directory = directory,
                identity = secondSigner,
                certificate = authorityCertificate,
                signingCertificate = secondSigningCertificate
            )

        assertTrue(RegistryCertificateSignatures.verifyAuthorityCertificate(authorityCertificate))
        assertTrue(RegistryCertificateSignatures.verifySigningCertificate(firstSigningCertificate))
        assertTrue(RegistryCertificateSignatures.verifySigningCertificate(secondSigningCertificate))
        assertTrue(ProtocolSignatures.verifyDirectory(firstDirectory))
        assertTrue(ProtocolSignatures.verifyDirectory(secondDirectory))
        assertEquals(rootIdentity.nodeId, authorityCertificate.rootNodeId)
        assertEquals(authorityIdentity.nodeId, firstSigningCertificate.authorityNodeId)
        assertEquals(authorityIdentity.nodeId, secondSigningCertificate.authorityNodeId)
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

    @Test
    fun gatewaySessionContentMatchesTheSharedWireVector() {
        val content =
            ProtocolSignatures.gatewaySessionContent(
                GatewaySessionBinding(
                    routingId = "r",
                    connectionId = "c",
                    nodeId = "n",
                    nonce = byteArrayOf(1, 2)
                )
            )

        // The app builds the same bytes in feature/transport; both tests pin this vector.
        assertEquals(GATEWAY_SESSION_VECTOR, content.toHexString())
    }

    @Test
    fun gatewaySessionSignatureIsBoundToEveryField() {
        val identity = NodeIdentity.generate()
        val session =
            GatewaySessionBinding(
                routingId = "ab",
                connectionId = "c",
                nodeId = "node-a",
                nonce = ByteArray(32) { 1 }
            )
        val signature = Signatures.sign(ProtocolSignatures.gatewaySessionContent(session), identity.privateKey)

        assertTrue(ProtocolSignatures.verifyGatewaySession(session, identity.encodedPublicKey, signature))
        listOf(
            GatewaySessionBinding("a", "bc", session.nodeId, session.nonce),
            GatewaySessionBinding(session.routingId, session.connectionId, "node-b", session.nonce),
            GatewaySessionBinding(session.routingId, session.connectionId, session.nodeId, ByteArray(32))
        ).forEach { other ->
            assertFalse(ProtocolSignatures.verifyGatewaySession(other, identity.encodedPublicKey, signature))
        }
        assertFalse(
            ProtocolSignatures.verifyGatewaySession(session, NodeIdentity.generate().encodedPublicKey, signature)
        )
    }

    @Test
    fun pushDeviceContentMatchesTheSharedWireVector() {
        val content =
            ProtocolSignatures.pushDeviceContent(
                PushDeviceBinding(
                    routingId = "r",
                    token = "t",
                    platform = "p",
                    issuedAtEpochMilliseconds = 258L
                )
            )

        // The app builds the same bytes in feature/transport; both tests pin this vector.
        assertEquals(PUSH_DEVICE_VECTOR, content.toHexString())
    }

    @Test
    fun pushDeviceSignatureIsBoundToEveryField() {
        val identity = NodeIdentity.generate()
        val device = PushDeviceBinding("routing", "token", "ANDROID", 1_000L)
        val signature = Signatures.sign(ProtocolSignatures.pushDeviceContent(device), identity.privateKey)

        assertTrue(ProtocolSignatures.verifyPushDevice(device, identity.encodedPublicKey, signature))
        listOf(
            PushDeviceBinding("other", device.token, device.platform, device.issuedAtEpochMilliseconds),
            PushDeviceBinding(device.routingId, "other", device.platform, device.issuedAtEpochMilliseconds),
            PushDeviceBinding(device.routingId, device.token, "IOS", device.issuedAtEpochMilliseconds),
            PushDeviceBinding(device.routingId, device.token, device.platform, 1_001L)
        ).forEach { other ->
            assertFalse(ProtocolSignatures.verifyPushDevice(other, identity.encodedPublicKey, signature))
        }
        assertFalse(
            ProtocolSignatures.verifyPushDevice(device, NodeIdentity.generate().encodedPublicKey, signature)
        )
    }

    private companion object {
        const val PUSH_DEVICE_VECTOR =
            "0000001673706172726f772d707573682d6465766963652d7631" +
                "000000017200000001740000000170000000080000000000000102"
        const val GATEWAY_SESSION_VECTOR =
            "0000001a73706172726f772d676174657761792d73657373696f6e2d7631" +
                "00000001720000000163000000016e000000020102"
    }
}
