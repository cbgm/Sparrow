package com.cbgm.sparrow.feature.transport.discovery

import com.cbgm.sparrow.core.crypto.hash.DefaultCryptoHash
import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.feature.transport.gateway.codec.createGatewayJson
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class NodeDirectoryVerifierTest {
    private val cryptoHash = DefaultCryptoHash()
    private val verifier =
        NodeDirectoryVerifier(
            signatureCrypto = AcceptingSignatureCrypto,
            cryptoHash = cryptoHash,
            json = createGatewayJson()
        )

    @Test
    fun rootCertifiedRegistryAuthorityIsAccepted() =
        runTest {
            val directory = signedDirectory()

            val result =
                verifier.verify(
                    signedDirectory = directory,
                    trustedRootNodeId = checkNotNull(directory.authorityCertificate).rootNodeId,
                    supportedProtocolVersion = 1,
                    nowEpochMilliseconds = NOW
                )

            assertTrue(result.isSuccess)
        }

    @Test
    fun changedRegistrySigningKeyIsAcceptedWhenRootCertified() =
        runTest {
            val first = signedDirectory(authoritySeed = 2, keyVersion = 1L)
            val rotated = signedDirectory(authoritySeed = 8, keyVersion = 2L)
            val trustedRoot = checkNotNull(first.authorityCertificate).rootNodeId

            val firstResult =
                verifier.verify(
                    signedDirectory = first,
                    trustedRootNodeId = trustedRoot,
                    supportedProtocolVersion = 1,
                    nowEpochMilliseconds = NOW
                )
            val rotatedResult =
                verifier.verify(
                    signedDirectory = rotated,
                    trustedRootNodeId = trustedRoot,
                    supportedProtocolVersion = 1,
                    nowEpochMilliseconds = NOW
                )

            assertTrue(firstResult.isSuccess)
            assertTrue(rotatedResult.isSuccess)
        }

    @Test
    fun rotatingSignerCertifiedByRegistryAuthorityIsAccepted() =
        runTest {
            val directory = signedDirectory(signingSeed = 10)

            val result =
                verifier.verify(
                    signedDirectory = directory,
                    trustedRootNodeId = checkNotNull(directory.authorityCertificate).rootNodeId,
                    supportedProtocolVersion = 1,
                    nowEpochMilliseconds = NOW
                )

            assertTrue(result.isSuccess)
        }

    @Test
    fun differentControlPlaneAuthoritiesAreAcceptedUnderTheSameRoot() =
        runTest {
            val first = signedDirectory(authoritySeed = 2, signingSeed = 10)
            val second = signedDirectory(authoritySeed = 8, signingSeed = 11)
            val trustedRoot = checkNotNull(first.authorityCertificate).rootNodeId

            val firstResult =
                verifier.verify(
                    signedDirectory = first,
                    trustedRootNodeId = trustedRoot,
                    supportedProtocolVersion = 1,
                    nowEpochMilliseconds = NOW
                )
            val secondResult =
                verifier.verify(
                    signedDirectory = second,
                    trustedRootNodeId = trustedRoot,
                    supportedProtocolVersion = 1,
                    nowEpochMilliseconds = NOW
                )

            assertTrue(firstResult.isSuccess)
            assertTrue(secondResult.isSuccess)
        }

    @Test
    fun untrustedRegistryRootIsRejected() =
        runTest {
            val result =
                verifier.verify(
                    signedDirectory = signedDirectory(),
                    trustedRootNodeId = "different-root",
                    supportedProtocolVersion = 1,
                    nowEpochMilliseconds = NOW
                )

            assertTrue(result.isFailure)
        }

    @Test
    fun directorySignerMustMatchRootCertificate() =
        runTest {
            val directory = signedDirectory()
            val differentAuthorityKey = encodedPublicKey(seed = 9)
            val tampered =
                directory.copy(
                    authorityNodeId = nodeId(differentAuthorityKey),
                    authorityPublicKey = differentAuthorityKey
                )

            val result =
                verifier.verify(
                    signedDirectory = tampered,
                    trustedRootNodeId = checkNotNull(directory.authorityCertificate).rootNodeId,
                    supportedProtocolVersion = 1,
                    nowEpochMilliseconds = NOW
                )

            assertTrue(result.isFailure)
        }

    @Test
    fun expiredRegistryAuthorityCertificateIsRejected() =
        runTest {
            val directory = signedDirectory(certificateValidUntil = NOW)

            val result =
                verifier.verify(
                    signedDirectory = directory,
                    trustedRootNodeId = checkNotNull(directory.authorityCertificate).rootNodeId,
                    supportedProtocolVersion = 1,
                    nowEpochMilliseconds = NOW
                )

            assertTrue(result.isFailure)
        }

    @Test
    fun legacyDirectorySignedDirectlyByTrustedRootIsAccepted() =
        runTest {
            val directory = signedDirectory(legacyRootSigning = true)

            val result =
                verifier.verify(
                    signedDirectory = directory,
                    trustedRootNodeId = directory.authorityNodeId,
                    supportedProtocolVersion = 1,
                    nowEpochMilliseconds = NOW
                )

            assertTrue(result.isSuccess)
        }

    @Test
    fun invalidSignatureIsRejected() =
        runTest {
            val directory = signedDirectory()
            val rejectingVerifier =
                NodeDirectoryVerifier(
                    signatureCrypto = RejectingSignatureCrypto,
                    cryptoHash = cryptoHash,
                    json = createGatewayJson()
                )

            val result =
                rejectingVerifier.verify(
                    signedDirectory = directory,
                    trustedRootNodeId = checkNotNull(directory.authorityCertificate).rootNodeId,
                    supportedProtocolVersion = 1,
                    nowEpochMilliseconds = NOW
                )

            assertTrue(result.isFailure)
        }

    @Test
    fun expiredDirectoryRequiresAnExplicitCacheGracePeriod() =
        runTest {
            val directory = signedDirectory(directoryValidUntil = NOW - 1L)
            val rootNodeId = checkNotNull(directory.authorityCertificate).rootNodeId

            val expiredResult =
                verifier.verify(
                    signedDirectory = directory,
                    trustedRootNodeId = rootNodeId,
                    supportedProtocolVersion = 1,
                    nowEpochMilliseconds = NOW
                )
            val cachedResult =
                verifier.verify(
                    signedDirectory = directory,
                    trustedRootNodeId = rootNodeId,
                    supportedProtocolVersion = 1,
                    nowEpochMilliseconds = NOW,
                    allowDirectoryExpiredUntilEpochMilliseconds = NOW + 1_000L
                )

            assertTrue(expiredResult.isFailure)
            assertTrue(cachedResult.isSuccess)
        }

    @Test
    fun directoryWithoutCompatibleGatewayIsRejected() =
        runTest {
            val directory =
                signedDirectory(
                    protocolVersions = setOf(2),
                    capabilities = setOf(NodeCapability.MAILBOX)
                )

            val result =
                verifier.verify(
                    signedDirectory = directory,
                    trustedRootNodeId = checkNotNull(directory.authorityCertificate).rootNodeId,
                    supportedProtocolVersion = 1,
                    nowEpochMilliseconds = NOW
                )

            assertTrue(result.isFailure)
        }

    private fun signedDirectory(
        directoryValidUntil: Long = NOW + 60_000L,
        protocolVersions: Set<Int> = setOf(1),
        capabilities: Set<NodeCapability> = setOf(NodeCapability.GATEWAY),
        authoritySeed: Int = 2,
        keyVersion: Long = 1L,
        certificateValidUntil: Long = NOW + 120_000L,
        legacyRootSigning: Boolean = false,
        signingSeed: Int? = null
    ): SignedNodeDirectory {
        val rootKey = encodedPublicKey(seed = 1)
        val certifiedAuthorityKey =
            if (legacyRootSigning) rootKey else encodedPublicKey(seed = authoritySeed)
        val directorySigningKey =
            signingSeed?.let(::encodedPublicKey) ?: certifiedAuthorityKey
        val nodeKey = encodedPublicKey(seed = 3)
        val certificate =
            if (legacyRootSigning) {
                null
            } else {
                RegistryAuthorityCertificate(
                    rootNodeId = nodeId(rootKey),
                    rootPublicKey = rootKey,
                    authorityNodeId = nodeId(certifiedAuthorityKey),
                    authorityPublicKey = certifiedAuthorityKey,
                    keyVersion = keyVersion,
                    validFromEpochMilliseconds = NOW - 60_000L,
                    validUntilEpochMilliseconds = certificateValidUntil,
                    signature = byteArrayOf(9)
                )
            }
        val signingCertificate =
            signingSeed?.let {
                RegistrySigningCertificate(
                    authorityNodeId = nodeId(certifiedAuthorityKey),
                    authorityPublicKey = certifiedAuthorityKey,
                    signingNodeId = nodeId(directorySigningKey),
                    signingPublicKey = directorySigningKey,
                    keyVersion = keyVersion,
                    validFromEpochMilliseconds = NOW - 60_000L,
                    validUntilEpochMilliseconds = certificateValidUntil,
                    signature = byteArrayOf(8)
                )
            }
        val node =
            SparrowNodeDescriptor(
                nodeId = nodeId(nodeKey),
                clientEndpoint = "wss://node.example/v1/gateway",
                federationEndpoint = "https://node.example/federation",
                mailboxEndpoint = "https://node.example/mailbox",
                identityPublicKey = nodeKey,
                protocolVersions = protocolVersions,
                capabilities = capabilities,
                validUntilEpochMilliseconds = NOW + 120_000L,
                signature = byteArrayOf(2)
            )
        return SignedNodeDirectory(
            directory =
                NodeDirectory(
                    generatedAtEpochMilliseconds = NOW - 1_000L,
                    validUntilEpochMilliseconds = directoryValidUntil,
                    nodes = listOf(node)
                ),
            authorityNodeId = nodeId(directorySigningKey),
            authorityPublicKey = directorySigningKey,
            authorityCertificate = certificate,
            signingCertificate = signingCertificate,
            signature = byteArrayOf(1)
        )
    }

    private fun encodedPublicKey(seed: Int): ByteArray =
        X509_ED25519_PREFIX + ByteArray(32) { index -> (index + seed).toByte() }

    private fun nodeId(publicKey: ByteArray): String =
        cryptoHash
            .sha256(publicKey)
            .joinToString(separator = "") { byte ->
                (byte.toInt() and 0xff).toString(radix = 16).padStart(length = 2, padChar = '0')
            }

    private object AcceptingSignatureCrypto : DetachedSignatureCrypto {
        override suspend fun sign(
            payload: ByteArray,
            signingPrivateKey: ByteArray
        ): Result<ByteArray> = Result.failure(UnsupportedOperationException())

        override suspend fun verify(
            payload: ByteArray,
            signingPublicKey: ByteArray,
            signature: ByteArray
        ): Result<Unit> = Result.success(Unit)
    }

    private object RejectingSignatureCrypto : DetachedSignatureCrypto {
        override suspend fun sign(
            payload: ByteArray,
            signingPrivateKey: ByteArray
        ): Result<ByteArray> = Result.failure(UnsupportedOperationException())

        override suspend fun verify(
            payload: ByteArray,
            signingPublicKey: ByteArray,
            signature: ByteArray
        ): Result<Unit> = Result.failure(IllegalArgumentException("invalid signature"))
    }

    private companion object {
        const val NOW = 1_000_000L

        val X509_ED25519_PREFIX =
            byteArrayOf(
                0x30,
                0x2a,
                0x30,
                0x05,
                0x06,
                0x03,
                0x2b,
                0x65,
                0x70,
                0x03,
                0x21,
                0x00
            )
    }
}
