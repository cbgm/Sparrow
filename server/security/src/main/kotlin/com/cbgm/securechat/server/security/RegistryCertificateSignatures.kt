package com.cbgm.securechat.server.security

import com.cbgm.securechat.server.protocol.RegistryAuthorityCertificate
import com.cbgm.securechat.server.protocol.RegistrySigningCertificate
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.protocol.unsigned

object RegistryCertificateSignatures {
    fun signAuthorityCertificate(
        rootIdentity: NodeIdentity,
        authorityIdentity: NodeIdentity,
        keyVersion: Long,
        validFromEpochMilliseconds: Long,
        validUntilEpochMilliseconds: Long
    ): RegistryAuthorityCertificate {
        val unsigned =
            RegistryAuthorityCertificate(
                rootNodeId = rootIdentity.nodeId,
                rootPublicKey = rootIdentity.encodedPublicKey,
                authorityNodeId = authorityIdentity.nodeId,
                authorityPublicKey = authorityIdentity.encodedPublicKey,
                keyVersion = keyVersion,
                validFromEpochMilliseconds = validFromEpochMilliseconds,
                validUntilEpochMilliseconds = validUntilEpochMilliseconds,
                signature = byteArrayOf()
            )
        return unsigned.copy(
            signature =
                Signatures.sign(
                    serverJson.encodeToString(unsigned.unsigned()).encodeToByteArray(),
                    rootIdentity.privateKey
                )
        )
    }

    fun verifyAuthorityCertificate(certificate: RegistryAuthorityCertificate): Boolean {
        if (NodeIds.fromPublicKey(certificate.rootPublicKey) != certificate.rootNodeId) {
            return false
        }
        if (NodeIds.fromPublicKey(certificate.authorityPublicKey) != certificate.authorityNodeId) {
            return false
        }
        return runCatching {
            Signatures.verify(
                content = serverJson.encodeToString(certificate.unsigned()).encodeToByteArray(),
                signature = certificate.signature,
                publicKey = Signatures.decodePublicKey(certificate.rootPublicKey)
            )
        }.getOrDefault(false)
    }

    fun signSigningCertificate(
        authorityIdentity: NodeIdentity,
        signingIdentity: NodeIdentity,
        keyVersion: Long,
        validFromEpochMilliseconds: Long,
        validUntilEpochMilliseconds: Long
    ): RegistrySigningCertificate {
        val unsigned =
            RegistrySigningCertificate(
                authorityNodeId = authorityIdentity.nodeId,
                authorityPublicKey = authorityIdentity.encodedPublicKey,
                signingNodeId = signingIdentity.nodeId,
                signingPublicKey = signingIdentity.encodedPublicKey,
                keyVersion = keyVersion,
                validFromEpochMilliseconds = validFromEpochMilliseconds,
                validUntilEpochMilliseconds = validUntilEpochMilliseconds,
                signature = byteArrayOf()
            )
        return unsigned.copy(
            signature =
                Signatures.sign(
                    serverJson.encodeToString(unsigned.unsigned()).encodeToByteArray(),
                    authorityIdentity.privateKey
                )
        )
    }

    fun verifySigningCertificate(certificate: RegistrySigningCertificate): Boolean {
        if (NodeIds.fromPublicKey(certificate.authorityPublicKey) != certificate.authorityNodeId) {
            return false
        }
        if (NodeIds.fromPublicKey(certificate.signingPublicKey) != certificate.signingNodeId) {
            return false
        }
        return runCatching {
            Signatures.verify(
                content = serverJson.encodeToString(certificate.unsigned()).encodeToByteArray(),
                signature = certificate.signature,
                publicKey = Signatures.decodePublicKey(certificate.authorityPublicKey)
            )
        }.getOrDefault(false)
    }
}
