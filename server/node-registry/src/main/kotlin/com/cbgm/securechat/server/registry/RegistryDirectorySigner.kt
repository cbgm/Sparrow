package com.cbgm.securechat.server.registry

import com.cbgm.securechat.server.protocol.NodeDirectory
import com.cbgm.securechat.server.protocol.RegistryAuthorityCertificate
import com.cbgm.securechat.server.protocol.SignedNodeDirectory
import com.cbgm.securechat.server.security.NodeIdentity
import com.cbgm.securechat.server.security.NodeIdentityStore
import com.cbgm.securechat.server.security.ProtocolSignatures
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
    private val rootIdentity: NodeIdentity,
    private val identityDirectory: Path,
    private val config: RegistrySigningConfig,
    private val now: () -> Long = System::currentTimeMillis
) : RegistryDirectorySigner {
    private var cachedVersion: Long? = null
    private var cachedAuthority: CertifiedRegistryAuthority? = null

    override fun sign(directory: NodeDirectory): SignedNodeDirectory {
        val currentTime = now()
        val version = config.keyVersion(currentTime)
        val authority = currentAuthority(version, currentTime)
        return ProtocolSignatures.signDirectory(
            directory = directory,
            identity = authority.identity,
            certificate = authority.certificate
        )
    }

    private fun currentAuthority(
        version: Long,
        currentTime: Long
    ): CertifiedRegistryAuthority {
        val existing = cachedAuthority
        if (version == cachedVersion && existing != null) {
            return existing
        }

        val identity =
            NodeIdentityStore(
                identityDirectory.resolve("registry-signing-v$version.identity")
            ).loadOrCreate()
        val certificate =
            ProtocolSignatures.signAuthorityCertificate(
                rootIdentity = rootIdentity,
                authorityIdentity = identity,
                keyVersion = version,
                validFromEpochMilliseconds = currentTime - CERTIFICATE_CLOCK_SKEW_MILLISECONDS,
                validUntilEpochMilliseconds =
                    currentTime +
                        config.rotationIntervalMilliseconds +
                        config.certificateOverlapMilliseconds
            )
        return CertifiedRegistryAuthority(
            identity = identity,
            certificate = certificate
        ).also { authority ->
            cachedVersion = version
            cachedAuthority = authority
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

    companion object {
        private const val DEFAULT_ROTATION_INTERVAL_MILLISECONDS =
            30L * 24L * 60L * 60L * 1_000L
        private const val DEFAULT_CERTIFICATE_OVERLAP_MILLISECONDS =
            7L * 24L * 60L * 60L * 1_000L
    }
}

private data class CertifiedRegistryAuthority(
    val identity: NodeIdentity,
    val certificate: RegistryAuthorityCertificate
)
