package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.AliasClaim
import com.cbgm.sparrow.server.protocol.ClientRoute
import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.protocol.UnsignedAliasClaim
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.security.ClientRoutingIds
import com.cbgm.sparrow.server.security.NodeIdentity
import com.cbgm.sparrow.server.security.Signatures
import java.security.KeyPairGenerator
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AliasClaimKeysTest {
    @Test
    fun opensAClaimSealedByAnotherImplementation() {
        // Sealed with Python's `cryptography` (X25519, SHA-256, ChaCha20-Poly1305); the app's libsodium test uses it too.
        val keys = AliasClaimKeys.fromRaw(decode(VECTOR_PRIVATE_KEY), decode(VECTOR_PUBLIC_KEY))

        assertContentEquals(VECTOR_MESSAGE.encodeToByteArray(), keys.open(decode(VECTOR_SEALED)))
    }

    @Test
    fun aClaimSealedForAnotherKeyOrChangedOnTheWayDoesNotOpen() {
        val keys = AliasClaimKeys.generate()
        val sealed = seal("claim".encodeToByteArray(), keys.publicKey)

        assertNull(AliasClaimKeys.generate().open(sealed))
        assertNull(keys.open(sealed.copyOf().also { bytes -> bytes[bytes.lastIndex] = (bytes.last() + 1).toByte() }))
        assertNull(keys.open(ByteArray(8)))
    }

    @Test
    fun theClaimMustBeSignedByTheDeviceForThisRoute() {
        val keys = AliasClaimKeys.generate()
        val device = NodeIdentity.generate()
        val registration = registration(device)

        assertEquals(listOf(ALIAS), keys.claimFor(registration.withClaim(keys, device))?.aliases)
        assertNull(keys.claimFor(registration.withClaim(keys, NodeIdentity.generate())))
        assertNull(keys.claimFor(registration.withClaim(keys, device, generation = 2L)))
        assertNull(keys.claimFor(registration))
    }

    private fun ClientRouteRegistration.withClaim(
        keys: AliasClaimKeys,
        signer: NodeIdentity,
        generation: Long = route.generation
    ): ClientRouteRegistration {
        val unsigned =
            UnsignedAliasClaim(
                routingId = route.routingId,
                connectionId = route.connectionId,
                generation = generation,
                aliases = listOf(ALIAS),
                phoneVerificationToken = "token"
            )
        val signature = Signatures.sign(serverJson.encodeToString(unsigned).encodeToByteArray(), signer.privateKey)
        val claim =
            AliasClaim(
                routingId = unsigned.routingId,
                connectionId = unsigned.connectionId,
                generation = unsigned.generation,
                aliases = unsigned.aliases,
                phoneVerificationToken = unsigned.phoneVerificationToken,
                signature = signature
            )
        return copy(sealedAliasClaim = seal(serverJson.encodeToString(claim).encodeToByteArray(), keys.publicKey))
    }

    private fun registration(device: NodeIdentity) =
        ClientRouteRegistration(
            route =
                ClientRoute(
                    routingId = ClientRoutingIds.fromSigningPublicKey(device.encodedPublicKey),
                    nodeId = "node-a",
                    connectionId = "connection-1",
                    generation = 1L,
                    expiresAtEpochMilliseconds = 2_000L,
                    clientSignature = byteArrayOf(1)
                ),
            clientSigningPublicKey = device.encodedPublicKey
        )

    private fun seal(
        message: ByteArray,
        recipientPublicKey: ByteArray
    ): ByteArray {
        val ephemeral = KeyPairGenerator.getInstance("X25519").generateKeyPair()
        val ephemeralPublicKey = ephemeral.public.encoded.takeLast(32).toByteArray()
        val shared =
            KeyAgreement.getInstance("X25519").run {
                init(ephemeral.private)
                doPhase(AliasClaimKeys.decodePublicKey(recipientPublicKey), true)
                generateSecret()
            }
        val key = AliasClaimKeys.sealingKey(shared, ephemeralPublicKey, recipientPublicKey)
        val ciphertext =
            Cipher.getInstance("ChaCha20-Poly1305").run {
                init(
                    Cipher.ENCRYPT_MODE,
                    SecretKeySpec(key, "ChaCha20"),
                    IvParameterSpec(ByteArray(AliasClaimKeys.NONCE_BYTES))
                )
                doFinal(message)
            }
        return ephemeralPublicKey + ciphertext
    }

    private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)

    private companion object {
        val ALIAS = ClientRoutingIds.fromPhoneNumber("+491701234567")
        const val VECTOR_PRIVATE_KEY = "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA="
        const val VECTOR_PUBLIC_KEY = "B6N8vBQgk8i3VdwbEOhstCY3StFqqFPtC9/AsrhtHHw="
        const val VECTOR_SEALED =
            "WGmv9FBUlzLLqu1eXfmzCm2jHLDldCutWtShp2jxpnslaVDMK2vUQXzNkghOjg78sKmi1DWz6SgCbYEcrdjoQPJ4MazdSL6bWPRGlz0="
        const val VECTOR_MESSAGE = """{"aliases":["scphone1_test"]}"""
    }
}
