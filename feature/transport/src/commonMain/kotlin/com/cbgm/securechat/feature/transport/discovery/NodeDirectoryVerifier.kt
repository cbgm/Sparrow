package com.cbgm.securechat.feature.transport.discovery

import com.cbgm.securechat.core.crypto.hash.CryptoHash
import com.cbgm.securechat.core.crypto.signature.DetachedSignatureCrypto
import kotlinx.serialization.json.Json

class NodeDirectoryVerifier(
    private val signatureCrypto: DetachedSignatureCrypto,
    private val cryptoHash: CryptoHash,
    private val json: Json
) {
    suspend fun verify(
        signedDirectory: SignedNodeDirectory,
        trustedRootNodeId: String,
        supportedProtocolVersion: Int,
        nowEpochMilliseconds: Long,
        allowDirectoryExpiredUntilEpochMilliseconds: Long? = null,
        descriptorExpiryGraceMilliseconds: Long = 0L
    ): Result<Unit> =
        runCatching {
            require(descriptorExpiryGraceMilliseconds >= 0L) {
                "Node descriptor expiry grace must not be negative"
            }
            verifyAuthority(
                signedDirectory = signedDirectory,
                trustedRootNodeId = trustedRootNodeId,
                nowEpochMilliseconds = nowEpochMilliseconds
            )
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

    fun rootNodeId(signedDirectory: SignedNodeDirectory): Result<String> =
        runCatching {
            val certificate = signedDirectory.authorityCertificate
            if (certificate == null) {
                val derivedNodeId = nodeId(signedDirectory.authorityPublicKey)
                require(derivedNodeId == signedDirectory.authorityNodeId) {
                    "Registry authority ID does not match its public key"
                }
                derivedNodeId
            } else {
                require(nodeId(certificate.rootPublicKey) == certificate.rootNodeId) {
                    "Registry root ID does not match its public key"
                }
                certificate.rootNodeId
            }
        }

    private suspend fun verifyAuthority(
        signedDirectory: SignedNodeDirectory,
        trustedRootNodeId: String,
        nowEpochMilliseconds: Long
    ) {
        val authorityCertificate = signedDirectory.authorityCertificate
        if (authorityCertificate == null) {
            verifyDirectRoot(signedDirectory, trustedRootNodeId)
            return
        }

        verifyAuthorityCertificate(
            certificate = authorityCertificate,
            trustedRootNodeId = trustedRootNodeId,
            nowEpochMilliseconds = nowEpochMilliseconds
        )

        val signingCertificate = signedDirectory.signingCertificate
        if (signingCertificate == null) {
            verifyDirectorySignerMatchesAuthority(signedDirectory, authorityCertificate)
            return
        }

        verifySigningCertificate(
            signedDirectory = signedDirectory,
            authorityCertificate = authorityCertificate,
            signingCertificate = signingCertificate,
            nowEpochMilliseconds = nowEpochMilliseconds
        )
    }

    private fun verifyDirectRoot(
        signedDirectory: SignedNodeDirectory,
        trustedRootNodeId: String
    ) {
        require(signedDirectory.signingCertificate == null) {
            "Registry signing certificate requires an authority certificate"
        }
        require(trustedRootNodeId == signedDirectory.authorityNodeId) {
            "Node directory was signed by an untrusted registry root"
        }
        require(nodeId(signedDirectory.authorityPublicKey) == signedDirectory.authorityNodeId) {
            "Registry root ID does not match its public key"
        }
    }

    private suspend fun verifyAuthorityCertificate(
        certificate: RegistryAuthorityCertificate,
        trustedRootNodeId: String,
        nowEpochMilliseconds: Long
    ) {
        require(certificate.rootNodeId == trustedRootNodeId) {
            "Registry authority certificate was signed by an untrusted root"
        }
        require(nodeId(certificate.rootPublicKey) == certificate.rootNodeId) {
            "Registry root ID does not match its public key"
        }
        require(nodeId(certificate.authorityPublicKey) == certificate.authorityNodeId) {
            "Registry authority ID does not match its public key"
        }
        requireCertificateTimeIsValid(
            validFromEpochMilliseconds = certificate.validFromEpochMilliseconds,
            validUntilEpochMilliseconds = certificate.validUntilEpochMilliseconds,
            nowEpochMilliseconds = nowEpochMilliseconds,
            label = "Registry authority certificate"
        )
        signatureCrypto
            .verify(
                payload = json.encodeToString(certificate.unsigned()).encodeToByteArray(),
                signingPublicKey = rawEd25519PublicKey(certificate.rootPublicKey),
                signature = certificate.signature
            ).getOrThrow()
    }

    private fun verifyDirectorySignerMatchesAuthority(
        signedDirectory: SignedNodeDirectory,
        certificate: RegistryAuthorityCertificate
    ) {
        require(certificate.authorityNodeId == signedDirectory.authorityNodeId) {
            "Directory signer does not match its registry authority certificate"
        }
        require(certificate.authorityPublicKey.contentEquals(signedDirectory.authorityPublicKey)) {
            "Directory signer key does not match its registry authority certificate"
        }
    }

    private suspend fun verifySigningCertificate(
        signedDirectory: SignedNodeDirectory,
        authorityCertificate: RegistryAuthorityCertificate,
        signingCertificate: RegistrySigningCertificate,
        nowEpochMilliseconds: Long
    ) {
        require(signingCertificate.authorityNodeId == authorityCertificate.authorityNodeId) {
            "Registry signing certificate was issued by a different authority"
        }
        require(
            signingCertificate.authorityPublicKey.contentEquals(
                authorityCertificate.authorityPublicKey
            )
        ) {
            "Registry signing certificate authority key does not match"
        }
        require(nodeId(signingCertificate.signingPublicKey) == signingCertificate.signingNodeId) {
            "Registry signing ID does not match its public key"
        }
        require(signingCertificate.signingNodeId == signedDirectory.authorityNodeId) {
            "Directory signer does not match its signing certificate"
        }
        require(signingCertificate.signingPublicKey.contentEquals(signedDirectory.authorityPublicKey)) {
            "Directory signer key does not match its signing certificate"
        }
        require(
            signingCertificate.validFromEpochMilliseconds >=
                authorityCertificate.validFromEpochMilliseconds
        ) {
            "Registry signing certificate begins before authority validity"
        }
        require(
            signingCertificate.validUntilEpochMilliseconds <=
                authorityCertificate.validUntilEpochMilliseconds
        ) {
            "Registry signing certificate exceeds authority validity"
        }
        requireCertificateTimeIsValid(
            validFromEpochMilliseconds = signingCertificate.validFromEpochMilliseconds,
            validUntilEpochMilliseconds = signingCertificate.validUntilEpochMilliseconds,
            nowEpochMilliseconds = nowEpochMilliseconds,
            label = "Registry signing certificate"
        )
        signatureCrypto
            .verify(
                payload = json.encodeToString(signingCertificate.unsigned()).encodeToByteArray(),
                signingPublicKey = rawEd25519PublicKey(signingCertificate.authorityPublicKey),
                signature = signingCertificate.signature
            ).getOrThrow()
    }

    private fun requireCertificateTimeIsValid(
        validFromEpochMilliseconds: Long,
        validUntilEpochMilliseconds: Long,
        nowEpochMilliseconds: Long,
        label: String
    ) {
        require(validFromEpochMilliseconds <= nowEpochMilliseconds + MAX_CLOCK_SKEW_MILLISECONDS) {
            "$label is not valid yet"
        }
        require(nowEpochMilliseconds < validUntilEpochMilliseconds) {
            "$label has expired"
        }
    }

    private suspend fun verifyDescriptor(
        descriptor: SecureChatNodeDescriptor,
        nowEpochMilliseconds: Long,
        expiryGraceMilliseconds: Long
    ) {
        require(nodeId(descriptor.identityPublicKey) == descriptor.nodeId) {
            "Node ID does not match its public key"
        }
        require(descriptor.activeConnections == null || descriptor.activeConnections >= 0) {
            "Node connection load must not be negative"
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
