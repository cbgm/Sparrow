package com.cbgm.sparrow.server.registry

import com.cbgm.sparrow.server.security.NodeIdentity
import com.cbgm.sparrow.server.security.NodeIdentityStore
import com.cbgm.sparrow.server.security.RegistryCertificateSignatures
import java.nio.file.Files
import java.nio.file.Path

object RegistryAuthorityProvisioningCli {
    @JvmStatic
    fun main(arguments: Array<String>) {
        require(arguments.size == ARGUMENT_COUNT) {
            "Expected root identity path, authority identity path and authority certificate path"
        }

        val rootIdentity =
            NodeIdentityStore(
                Path.of(arguments[ROOT_IDENTITY_PATH_INDEX])
            ).loadExisting()
        val authorityIdentityPath = Path.of(arguments[AUTHORITY_IDENTITY_PATH_INDEX])
        val authorityCertificatePath = Path.of(arguments[AUTHORITY_CERTIFICATE_PATH_INDEX])
        val authorityIdentity = NodeIdentityStore(authorityIdentityPath).loadOrCreate()
        val certificate =
            if (Files.exists(authorityCertificatePath)) {
                RegistryAuthorityCertificateStore(authorityCertificatePath).load()
            } else {
                createCertificate(
                    rootIdentity = rootIdentity,
                    authorityIdentity = authorityIdentity,
                    authorityCertificatePath = authorityCertificatePath
                )
            }

        require(RegistryCertificateSignatures.verifyAuthorityCertificate(certificate)) {
            "Registry authority certificate signature is invalid"
        }
        require(certificate.rootNodeId == rootIdentity.nodeId) {
            "Registry authority certificate does not match the supplied root identity"
        }
        require(certificate.authorityNodeId == authorityIdentity.nodeId) {
            "Registry authority certificate does not match the authority identity"
        }

        println("rootNodeId=${certificate.rootNodeId}")
        println("authorityNodeId=${certificate.authorityNodeId}")
    }

    private fun createCertificate(
        rootIdentity: NodeIdentity,
        authorityIdentity: NodeIdentity,
        authorityCertificatePath: Path
    ) =
        RegistryCertificateSignatures
            .signAuthorityCertificate(
                rootIdentity = rootIdentity,
                authorityIdentity = authorityIdentity,
                keyVersion = AUTHORITY_KEY_VERSION,
                validFromEpochMilliseconds =
                    System.currentTimeMillis() - CERTIFICATE_CLOCK_SKEW_MILLISECONDS,
                validUntilEpochMilliseconds =
                    System.currentTimeMillis() + AUTHORITY_LIFETIME_MILLISECONDS
            ).also { certificate ->
                RegistryAuthorityCertificateStore(authorityCertificatePath).save(certificate)
            }

    private const val ARGUMENT_COUNT = 3
    private const val ROOT_IDENTITY_PATH_INDEX = 0
    private const val AUTHORITY_IDENTITY_PATH_INDEX = 1
    private const val AUTHORITY_CERTIFICATE_PATH_INDEX = 2
    private const val AUTHORITY_KEY_VERSION = 1L
    private const val CERTIFICATE_CLOCK_SKEW_MILLISECONDS = 5L * 60L * 1_000L
    private const val AUTHORITY_LIFETIME_MILLISECONDS = 10L * 365L * 24L * 60L * 60L * 1_000L
}
