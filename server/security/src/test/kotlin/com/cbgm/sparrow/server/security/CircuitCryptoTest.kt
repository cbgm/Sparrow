package com.cbgm.sparrow.server.security

import java.security.MessageDigest
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CircuitCryptoTest {
    private val relay = X25519KeyPair.generate()

    @Test
    fun bothSidesDeriveTheSameHopKeys() {
        val ephemeral = CircuitHandshake.initiate()
        val reply = assertNotNull(CircuitHandshake.respond(relay, ephemeral.publicKey))

        val keys = assertNotNull(CircuitHandshake.complete(ephemeral, relay.publicKey, reply.reply))

        assertContentEquals(reply.keys.forward, keys.forward)
        assertContentEquals(reply.keys.backward, keys.backward)
        assertFalse(keys.forward.contentEquals(keys.backward))
    }

    @Test
    fun aRelayWithoutTheChosenRelayKeyIsNotAccepted() {
        val ephemeral = CircuitHandshake.initiate()
        val impostor = assertNotNull(CircuitHandshake.respond(X25519KeyPair.generate(), ephemeral.publicKey))

        assertNull(CircuitHandshake.complete(ephemeral, relay.publicKey, impostor.reply))
    }

    @Test
    fun anInvalidEphemeralKeyGetsNoAnswer() {
        assertNull(CircuitHandshake.respond(relay, ByteArray(31)))
        assertNull(CircuitHandshake.respond(relay, ByteArray(32)))
    }

    @Test
    fun streamLayersKeepTheSizeAndUndoThemselves() {
        val key = ByteArray(32) { 3 }
        val cell = ByteArray(1019) { it.toByte() }

        val layered = CircuitLayer.stream(key, 7, cell)

        assertContentEquals(cell, CircuitLayer.stream(key, 7, layered))
        assertFalse(cell.contentEquals(CircuitLayer.stream(key, 8, layered)))
        assertEquals(cell.size, layered.size)
    }

    @Test
    fun aSealedCellOpensOnlyWithItsCounter() {
        val key = ByteArray(32) { 5 }
        val sealed = CircuitLayer.seal(key, 2, ByteArray(1003) { 1 })

        assertNotNull(CircuitLayer.open(key, 2, sealed))
        assertNull(CircuitLayer.open(key, 3, sealed))
    }

    @Test
    fun matchesAnIndependentImplementation() {
        // Computed with Python's `cryptography` from the description in CircuitHandshake and CircuitLayer.
        val client = X25519KeyPair.fromSeed(ByteArray(32) { (it + 1).toByte() })
        val relayKey = X25519KeyPair.fromSeed(ByteArray(32) { (it + 65).toByte() })
        val reply = decode("WGmv9FBUlzLLqu1eXfmzCm2jHLDldCutWtShp2jxpns=") + decode("sfwbVl7en5UiHrOX38G5utD1B8b6Hpy+9waDWBIW3ls=")

        val keys = assertNotNull(CircuitHandshake.complete(client, relayKey.publicKey, reply))

        assertContentEquals(decode("B6N8vBQgk8i3VdwbEOhstCY3StFqqFPtC9/AsrhtHHw="), client.publicKey)
        assertContentEquals(decode("Tr+9V0PnzHTKPU3TTasAhyOMyzK68S6O/bZCgf9rj80="), keys.forward)
        assertContentEquals(decode("wI5LVQDO7Zfm+n8aJrm/y4SUfChDDhcjINjO1RCwQrs="), keys.backward)
        val data = ByteArray(1019) { it.toByte() }
        assertContentEquals(decode("BdHkrcfhdbOAE5Lp3/dQmZyofKxOmCpHPLlvM33Hxi0="), sha256(CircuitLayer.stream(keys.forward, 5, data)))
        assertContentEquals(
            decode("cPg+FiuW4Sn6iQxW1DvhbwVDe6xWAn7zTXar0MBFzSg="),
            sha256(CircuitLayer.seal(keys.backward, 9, data.copyOf(1003)))
        )
    }

    private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)

    private fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)
}
