package com.cbgm.sparrow.server.security

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class X25519SealingTest {
    @Test
    fun opensAMessageSealedByAnotherImplementation() {
        // Sealed with Python's `cryptography`; the app's libsodium test seals the same bytes.
        val keys = X25519KeyPair.fromSeed(decode(VECTOR_PRIVATE_KEY))

        assertContentEquals(decode(VECTOR_PUBLIC_KEY), keys.publicKey)
        assertContentEquals(VECTOR_MESSAGE.encodeToByteArray(), keys.open(decode(VECTOR_SEALED), VECTOR_CONTEXT))
    }

    @Test
    fun aMessageForAnotherKeyOrContextOrChangedOnTheWayDoesNotOpen() {
        val keys = X25519KeyPair.generate()
        val sealed = X25519Sealing.seal("message".encodeToByteArray(), keys.publicKey, "context-a")

        assertContentEquals("message".encodeToByteArray(), keys.open(sealed, "context-a"))
        assertNull(keys.open(sealed, "context-b"))
        assertNull(X25519KeyPair.generate().open(sealed, "context-a"))
        assertNull(keys.open(sealed.copyOf().also { bytes -> bytes[bytes.lastIndex] = (bytes.last() + 1).toByte() }, "context-a"))
        assertNull(keys.open(ByteArray(8), "context-a"))
    }

    @Test
    fun repliesOpenOnlyWithTheirKey() {
        val key = ByteArray(ReplySealing.KEY_BYTES) { 3 }
        val sealed = ReplySealing.seal("reply".encodeToByteArray(), key)

        assertContentEquals("reply".encodeToByteArray(), ReplySealing.open(sealed, key))
        assertNull(ReplySealing.open(sealed, ByteArray(ReplySealing.KEY_BYTES) { 4 }))
    }

    @Test
    fun theRelayKeyFollowsTheNodeIdentity() {
        val identity = NodeIdentity.generate()

        assertContentEquals(identity.relayKeyPair().publicKey, identity.relayKeyPair().publicKey)
        assertFalse(identity.relayKeyPair().publicKey.contentEquals(NodeIdentity.generate().relayKeyPair().publicKey))
    }

    private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)

    private companion object {
        const val VECTOR_CONTEXT = "sparrow-alias-claim-v1"
        const val VECTOR_PRIVATE_KEY = "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA="
        const val VECTOR_PUBLIC_KEY = "B6N8vBQgk8i3VdwbEOhstCY3StFqqFPtC9/AsrhtHHw="
        const val VECTOR_SEALED =
            "WGmv9FBUlzLLqu1eXfmzCm2jHLDldCutWtShp2jxpnslaVDMK2vUQXzNkghOjg78sKmi1DWz6SgCbYEcrdjoQPJ4MazdSL6bWPRGlz0="
        const val VECTOR_MESSAGE = """{"aliases":["scphone1_test"]}"""
    }
}
