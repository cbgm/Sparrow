package com.cbgm.sparrow.server.registry

import com.cbgm.sparrow.server.persistence.ServiceEnvironment
import com.cbgm.sparrow.server.security.NodeIdentity
import com.cbgm.sparrow.server.security.NodeIdentityStore
import com.cbgm.sparrow.server.security.RegistryCertificateSignatures
import java.nio.file.Files
import java.nio.file.Path

internal data class RegistrySigningRuntime(
    val identity: NodeIdentity,
    val directorySigner: RegistryDirectorySigner
)

internal fun createRegistrySigningRuntime(): RegistrySigningRuntime {
    val authorityIdentityPath = System.getenv("REGISTRY_AUTHORITY_IDENTITY_PATH")?.takeIf(String::isNotBlank)
    val authorityCertificatePath =
        System.getenv("REGISTRY_AUTHORITY_CERTIFICATE_PATH")?.takeIf(String::isNotBlank)

    require((authorityIdentityPath == null) == (authorityCertificatePath == null)) {
        "Registry authority identity and certificate must be configured together"
    }

    return if (authorityIdentityPath != null && authorityCertificatePath != null) {
        createCertifiedRuntime(
            authorityIdentityPath = Path.of(authorityIdentityPath),
            authorityCertificatePath = Path.of(authorityCertificatePath)
        )
    } else {
        createLegacyRuntime()
    }
}

private fun createCertifiedRuntime(
    authorityIdentityPath: Path,
    authorityCertificatePath: Path
): RegistrySigningRuntime {
    val authorityIdentity = NodeIdentityStore(authorityIdentityPath).loadExisting()
    val authorityCertificate = RegistryAuthorityCertificateStore(authorityCertificatePath).load()
    val signingDirectory =
        Path.of(
            ServiceEnvironment.string(
                "REGISTRY_SIGNING_IDENTITY_DIRECTORY",
                ".sparrow-server/registry-signing"
            )
        )
    return RegistrySigningRuntime(
        identity = authorityIdentity,
        directorySigner =
            RotatingRegistryDirectorySigner(
                authorityIdentity = authorityIdentity,
                authorityCertificate = authorityCertificate,
                identityDirectory = signingDirectory,
                config = RegistrySigningConfig()
            )
    )
}

private fun createLegacyRuntime(): RegistrySigningRuntime {
    val rootIdentityPath =
        Path.of(
            ServiceEnvironment.string(
                "REGISTRY_IDENTITY_PATH",
                ".sparrow-server/registry.identity"
            )
        )
    val rootIdentity = NodeIdentityStore(rootIdentityPath).loadOrCreate()
    val identityDirectory = rootIdentityPath.parent ?: Path.of(".")
    val authorityIdentityPath = identityDirectory.resolve("registry-authority.identity")
    val authorityCertificatePath = identityDirectory.resolve("registry-authority-certificate.json")
    val authorityIdentity = NodeIdentityStore(authorityIdentityPath).loadOrCreate()
    val authorityCertificate =
        loadOrCreateAuthorityCertificate(
            path = authorityCertificatePath,
            rootIdentity = rootIdentity,
            authorityIdentity = authorityIdentity
        )
    return RegistrySigningRuntime(
        identity = authorityIdentity,
        directorySigner =
            RotatingRegistryDirectorySigner(
                authorityIdentity = authorityIdentity,
                authorityCertificate = authorityCertificate,
                identityDirectory = identityDirectory.resolve("registry-signing"),
                config = RegistrySigningConfig()
            )
    )
}

private fun loadOrCreateAuthorityCertificate(
    path: Path,
    rootIdentity: NodeIdentity,
    authorityIdentity: NodeIdentity
) =
    if (Files.exists(path)) {
        RegistryAuthorityCertificateStore(path).load()
    } else {
        val currentTime = System.currentTimeMillis()
        RegistryCertificateSignatures
            .signAuthorityCertificate(
                rootIdentity = rootIdentity,
                authorityIdentity = authorityIdentity,
                keyVersion = 1L,
                validFromEpochMilliseconds = currentTime - CERTIFICATE_CLOCK_SKEW_MILLISECONDS,
                validUntilEpochMilliseconds = currentTime + LEGACY_AUTHORITY_LIFETIME_MILLISECONDS
            ).also { certificate ->
                RegistryAuthorityCertificateStore(path).save(certificate)
            }
    }

private const val CERTIFICATE_CLOCK_SKEW_MILLISECONDS = 5L * 60L * 1_000L
private const val LEGACY_AUTHORITY_LIFETIME_MILLISECONDS = 10L * 365L * 24L * 60L * 60L * 1_000L
