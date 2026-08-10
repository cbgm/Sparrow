package com.cbgm.securechat.server.registry

import com.cbgm.securechat.server.protocol.NodeDirectory
import com.cbgm.securechat.server.security.NodeIdentity
import com.cbgm.securechat.server.security.ProtocolSignatures
import com.cbgm.securechat.server.security.RegistryCertificateSignatures
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RegistryDirectorySignerTest {
    @Test
    fun automaticRotationChangesSignerAtBoundaryWithoutChangingTrustRoot() {
        val rootIdentity = NodeIdentity.generate()
        val authorityIdentity = NodeIdentity.generate()
        val identityDirectory = Files.createTempDirectory("securechat-registry-signer-test")
        val config =
            RegistrySigningConfig(
                rotationIntervalMilliseconds = ROTATION_INTERVAL_MILLISECONDS,
                certificateOverlapMilliseconds = CERTIFICATE_OVERLAP_MILLISECONDS
            )
        val authorityCertificate =
            RegistryCertificateSignatures.signAuthorityCertificate(
                rootIdentity = rootIdentity,
                authorityIdentity = authorityIdentity,
                keyVersion = 1L,
                validFromEpochMilliseconds = 0L,
                validUntilEpochMilliseconds = 10L * ROTATION_INTERVAL_MILLISECONDS
            )
        var currentTime = ROTATION_INTERVAL_MILLISECONDS - 1L
        val signer =
            RotatingRegistryDirectorySigner(
                authorityIdentity = authorityIdentity,
                authorityCertificate = authorityCertificate,
                identityDirectory = identityDirectory,
                config = config,
                now = { currentTime }
            )

        val first = signer.sign(directory(currentTime))
        currentTime = ROTATION_INTERVAL_MILLISECONDS
        val second = signer.sign(directory(currentTime))

        val firstAuthorityCertificate = checkNotNull(first.authorityCertificate)
        val secondAuthorityCertificate = checkNotNull(second.authorityCertificate)
        val firstSigningCertificate = checkNotNull(first.signingCertificate)
        val secondSigningCertificate = checkNotNull(second.signingCertificate)

        assertEquals(rootIdentity.nodeId, firstAuthorityCertificate.rootNodeId)
        assertEquals(rootIdentity.nodeId, secondAuthorityCertificate.rootNodeId)
        assertEquals(authorityIdentity.nodeId, firstSigningCertificate.authorityNodeId)
        assertEquals(authorityIdentity.nodeId, secondSigningCertificate.authorityNodeId)
        assertNotEquals(first.authorityNodeId, second.authorityNodeId)
        assertEquals(0L, firstSigningCertificate.keyVersion)
        assertEquals(1L, secondSigningCertificate.keyVersion)
        assertEquals(
            ROTATION_INTERVAL_MILLISECONDS + CERTIFICATE_OVERLAP_MILLISECONDS,
            firstSigningCertificate.validUntilEpochMilliseconds
        )
        assertEquals(
            2L * ROTATION_INTERVAL_MILLISECONDS + CERTIFICATE_OVERLAP_MILLISECONDS,
            secondSigningCertificate.validUntilEpochMilliseconds
        )
        assertTrue(ProtocolSignatures.verifyDirectory(first))
        assertTrue(ProtocolSignatures.verifyDirectory(second))
    }

    @Test
    fun automaticVersionIsDerivedOnlyFromTime() {
        val config =
            RegistrySigningConfig(
                rotationIntervalMilliseconds = 1_000L,
                certificateOverlapMilliseconds = 100L
            )

        assertEquals(4L, config.keyVersion(4_999L))
        assertEquals(5L, config.keyVersion(5_000L))
    }

    private fun directory(now: Long): NodeDirectory =
        NodeDirectory(
            generatedAtEpochMilliseconds = now,
            validUntilEpochMilliseconds = now + 60_000L,
            nodes = emptyList()
        )

    private companion object {
        const val ROTATION_INTERVAL_MILLISECONDS = 30L * 24L * 60L * 60L * 1_000L
        const val CERTIFICATE_OVERLAP_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
    }
}
