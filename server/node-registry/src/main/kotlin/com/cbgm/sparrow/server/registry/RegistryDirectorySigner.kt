package com.cbgm.sparrow.server.registry

import com.cbgm.sparrow.server.protocol.NodeDirectory
import com.cbgm.sparrow.server.protocol.RegistryAuthorityCertificate
import com.cbgm.sparrow.server.protocol.RegistrySigningCertificate
import com.cbgm.sparrow.server.protocol.SignedNodeDirectory
import com.cbgm.sparrow.server.security.NodeIdentity
import com.cbgm.sparrow.server.security.NodeIdentityStore
import com.cbgm.sparrow.server.security.ProtocolSignatures
import com.cbgm.sparrow.server.security.RegistryCertificateSignatures
import java.nio.file.Path

interface RegistryDirectorySigner {
    fun sign(directory: NodeDirectory): SignedNodeDirectory
}

class DirectRegistryDirectorySigner(
    private val identity: NodeIdentity
) : RegistryDirectorySigner {
    override fun sign(directory: NodeDirectory): SignedNodeDirectory =
        ProtocolSignatures.signDirectory(
            directory = directory,
            identity = identity
        )
}

class RotatingRegistryDirectorySigner(
    private val authorityIdentity: NodeIdentity,
    private val authorityCertificate: RegistryAuthorityCertificate,
    private val identityDirectory: Path,
    private val config: RegistrySigningConfig,
    private val now: () -> Long = System::currentTimeMillis
) : RegistryDirectorySigner {
    private var cachedVersion: Long? = null
    private var cachedSigner: CertifiedRegistrySigner? = null

    init {
        require(RegistryCertificateSignatures.verifyAuthorityCertificate(authorityCertificate)) {
            "Registry authority certificate signature is invalid"
        }
        require(authorityCertificate.authorityNodeId == authorityIdentity.nodeId) {
            "Registry authority identity does not match its certificate"
        }
        require(
            authorityCertificate.authorityPublicKey.contentEquals(
                authorityIdentity.encodedPublicKey
            )
        ) {
            "Registry authority public key does not match its certificate"
        }
    }

    override fun sign(directory: NodeDirectory): SignedNodeDirectory {
        val currentTime = now()
        require(currentTime < authorityCertificate.validUntilEpochMilliseconds) {
            "Registry authority certificate has expired"
        }
        val version = config.keyVersion(currentTime)
        val signer = currentSigner(version, currentTime)
        return ProtocolSignatures.signDirectory(
            directory = directory,
            identity = signer.identity,
            certificate = authorityCertificate,
            signingCertificate = signer.certificate
        )
    }

    private fun currentSigner(
        version: Long,
        currentTime: Long
    ): CertifiedRegistrySigner {
        val existing = cachedSigner
        if (version == cachedVersion && existing != null) {
            return existing
        }

        val identity =
            NodeIdentityStore(
                identityDirectory.resolve("registry-signing-v$version.identity")
            ).loadOrCreate()
        val validFrom =
            maxOf(
                config.versionStart(version) - CERTIFICATE_CLOCK_SKEW_MILLISECONDS,
                authorityCertificate.validFromEpochMilliseconds
            )
        val validUntil =
            minOf(
                config.versionEnd(version) + config.certificateOverlapMilliseconds,
                authorityCertificate.validUntilEpochMilliseconds
            )
        require(validUntil > currentTime) {
            "Registry authority certificate expires before a signing certificate can be issued"
        }
        val certificate =
            RegistryCertificateSignatures.signSigningCertificate(
                authorityIdentity = authorityIdentity,
                signingIdentity = identity,
                keyVersion = version,
                validFromEpochMilliseconds = validFrom,
                validUntilEpochMilliseconds = validUntil
            )
        return CertifiedRegistrySigner(
            identity = identity,
            certificate = certificate
        ).also { signer ->
            cachedVersion = version
            cachedSigner = signer
        }
    }

    private companion object {
        const val CERTIFICATE_CLOCK_SKEW_MILLISECONDS = 5L * 60L * 1_000L
    }
}

data class RegistrySigningConfig(
    val rotationIntervalMilliseconds: Long = DEFAULT_ROTATION_INTERVAL_MILLISECONDS,
    val certificateOverlapMilliseconds: Long = DEFAULT_CERTIFICATE_OVERLAP_MILLISECONDS
) {
    init {
        require(rotationIntervalMilliseconds > 0L)
        require(certificateOverlapMilliseconds >= 0L)
    }

    fun keyVersion(nowEpochMilliseconds: Long): Long =
        nowEpochMilliseconds / rotationIntervalMilliseconds

    fun versionStart(version: Long): Long = version * rotationIntervalMilliseconds

    fun versionEnd(version: Long): Long = (version + 1L) * rotationIntervalMilliseconds

    companion object {
        private const val DEFAULT_ROTATION_INTERVAL_MILLISECONDS =
            30L * 24L * 60L * 60L * 1_000L
        private const val DEFAULT_CERTIFICATE_OVERLAP_MILLISECONDS =
            7L * 24L * 60L * 60L * 1_000L
    }
}

private data class CertifiedRegistrySigner(
    val identity: NodeIdentity,
    val certificate: RegistrySigningCertificate
)
