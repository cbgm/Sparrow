package com.cbgm.securechat.server.security

import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

data class NodeIdentity(
    val nodeId: String,
    val publicKey: PublicKey,
    val privateKey: PrivateKey
) {
    val encodedPublicKey: ByteArray
        get() = publicKey.encoded

    companion object {
        fun generate(): NodeIdentity {
            val keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()

            return NodeIdentity(
                nodeId = NodeIds.fromPublicKey(keyPair.public.encoded),
                publicKey = keyPair.public,
                privateKey = keyPair.private
            )
        }

        fun decode(
            publicKey: String,
            privateKey: String
        ): NodeIdentity {
            val keyFactory = KeyFactory.getInstance("Ed25519")
            val decoder = Base64.getDecoder()
            val decodedPublicKey =
                keyFactory.generatePublic(
                    X509EncodedKeySpec(decoder.decode(publicKey))
                )
            val decodedPrivateKey =
                keyFactory.generatePrivate(
                    PKCS8EncodedKeySpec(decoder.decode(privateKey))
                )

            return NodeIdentity(
                nodeId = NodeIds.fromPublicKey(decodedPublicKey.encoded),
                publicKey = decodedPublicKey,
                privateKey = decodedPrivateKey
            )
        }
    }
}
