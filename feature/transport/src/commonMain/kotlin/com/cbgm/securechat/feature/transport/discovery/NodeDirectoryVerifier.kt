package com.cbgm.securechat.feature.transport.discovery

import com.cbgm.securechat.core.crypto.hash.CryptoHash
import com.cbgm.securechat.core.crypto.signature.DetachedSignatureCrypto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class NodeDirectoryVerifier(
    private val signatureCrypto: DetachedSignatureCrypto,
    private val cryptoHash: CryptoHash,
    private val json: Json
) {
    suspend fun verify(
        signedDirectory: SignedNodeDirectory,
        trustedAuthorityNodeId: String,
        supportedProtocolVersion: Int,
        nowEpochMilliseconds: Long,
        allowDirectoryExpiredUntilEpochMilliseconds: Long? = null,
        descriptorExpiryGraceMilliseconds: Long = 0L
    ): Result<Unit> =
        runCatching {
            require(descriptorExpiryGraceMilliseconds >= 0L) {
                "Node descriptor expiry grace must not be negative"
            }
            require(trustedAuthorityNodeId == signedDirectory.authorityNodeId) {
                "Node directory was signed by an untrusted registry authority"
            }
            require(nodeId(signedDirectory.authorityPublicKey) == signedDirectory.authorityNodeId) {
                "Registry authority ID does not match its public key"
            }
            require(
                signedDirectory.directory.generatedAtEpochMilliseconds <=
                    nowEpochMilliseconds + MAX_CLOCK_SKEW_MILLISECONDS
            ) {
                "Node directory was generated in the future"
            }

            val acceptedDirectoryExpiry =
                allowDirectoryExpiredUntilEpochMilliseconds
                    ?: signedDirectory.directory.validUntilEpochMilliseconds
            require(nowEpochMilliseconds < acceptedDirectoryExpiry) {
                "Node directory has expired"
            }
            require(
                signedDirectory.directory.validUntilEpochMilliseconds >
                    signedDirectory.directory.generatedAtEpochMilliseconds
            ) {
                "Node directory validity window is invalid"
            }

            signatureCrypto
                .verify(
                    payload = json.encodeToString(signedDirectory.directory).encodeToByteArray(),
                    signingPublicKey = rawEd25519PublicKey(signedDirectory.authorityPublicKey),
                    signature = signedDirectory.signature
                ).getOrThrow()

            require(signedDirectory.directory.nodes.isNotEmpty()) {
                "Node directory does not contain a compatible gateway"
            }

            signedDirectory.directory.nodes.forEach { descriptor ->
                verifyDescriptor(
                    descriptor = descriptor,
                    nowEpochMilliseconds = nowEpochMilliseconds,
                    expiryGraceMilliseconds = descriptorExpiryGraceMilliseconds
                )
            }

            require(
                signedDirectory.directory.nodes.any { descriptor ->
                    supportedProtocolVersion in descriptor.protocolVersions &&
                        NodeCapability.GATEWAY in descriptor.capabilities
                }
            ) {
                "Node directory does not contain a compatible gateway"
            }
        }

    fun authorityNodeId(signedDirectory: SignedNodeDirectory): Result<String> =
        runCatching {
            val derivedNodeId = nodeId(signedDirectory.authorityPublicKey)
            require(derivedNodeId == signedDirectory.authorityNodeId) {
                "Registry authority ID does not match its public key"
            }
            derivedNodeId
        }

    private suspend fun verifyDescriptor(
        descriptor: SecureChatNodeDescriptor,
        nowEpochMilliseconds: Long,
        expiryGraceMilliseconds: Long
    ) {
        require(nodeId(descriptor.identityPublicKey) == descriptor.nodeId) {
            "Node ID does not match its public key"
        }
        val acceptedDescriptorExpiry =
            descriptor.validUntilEpochMilliseconds + expiryGraceMilliseconds
        require(nowEpochMilliseconds < acceptedDescriptorExpiry) {
            "Node descriptor has expired"
        }
        require(
            descriptor.clientEndpoint.startsWith("ws://") ||
                descriptor.clientEndpoint.startsWith("wss://")
        ) {
            "Node gateway endpoint must use ws:// or wss://"
        }

        signatureCrypto
            .verify(
                payload = json.encodeToString(descriptor.unsigned()).encodeToByteArray(),
                signingPublicKey = rawEd25519PublicKey(descriptor.identityPublicKey),
                signature = descriptor.signature
            ).getOrThrow()
    }

    private fun nodeId(encodedPublicKey: ByteArray): String =
        cryptoHash
            .sha256(encodedPublicKey)
            .joinToString(separator = "") { byte ->
                (byte.toInt() and 0xff).toString(radix = 16).padStart(length = 2, padChar = '0')
            }

    private fun rawEd25519PublicKey(encodedPublicKey: ByteArray): ByteArray {
        require(encodedPublicKey.size == X509_ED25519_PREFIX.size + ED25519_PUBLIC_KEY_SIZE) {
            "Node public key is not a supported Ed25519 X.509 key"
        }
        require(encodedPublicKey.copyOfRange(0, X509_ED25519_PREFIX.size).contentEquals(X509_ED25519_PREFIX)) {
            "Node public key has an invalid Ed25519 X.509 prefix"
        }
        return encodedPublicKey.copyOfRange(X509_ED25519_PREFIX.size, encodedPublicKey.size)
    }

    private companion object {
        const val ED25519_PUBLIC_KEY_SIZE = 32
        const val MAX_CLOCK_SKEW_MILLISECONDS = 5L * 60L * 1_000L

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
