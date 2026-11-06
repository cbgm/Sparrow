package com.cbgm.sparrow.core.crypto.sealing

import com.cbgm.sparrow.core.crypto.SodiumRuntime
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class SodiumSealedMessageCipherTest {
    private val cipher = SodiumSealedMessageCipher()

    @Test
    fun sealsExactlyLikeTheServerExpects() =
        runTest {
            SodiumRuntime.initialize().getOrThrow()

            // Same vector as the server's X25519SealingTest (sealed with Python's `cryptography`).
            val sealed =
                cipher.sealWith(
                    message = """{"aliases":["scphone1_test"]}""".encodeToByteArray(),
                    recipientPublicKey = Base64.decode("B6N8vBQgk8i3VdwbEOhstCY3StFqqFPtC9/AsrhtHHw="),
                    context = "sparrow-alias-claim-v1",
                    ephemeralSecretKey = Base64.decode("ISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0+P0A=")
                )

            assertContentEquals(
                Base64.decode(
                    "WGmv9FBUlzLLqu1eXfmzCm2jHLDldCutWtShp2jxpnslaVDMK2vUQXzNkghOjg78sKmi1DWz6SgCbYEcrdjoQPJ4MazdSL6bWPRGlz0="
                ),
                sealed
            )
        }

    @Test
    fun everyMessageUsesItsOwnEphemeralKey() =
        runTest {
            val key = Base64.decode("B6N8vBQgk8i3VdwbEOhstCY3StFqqFPtC9/AsrhtHHw=")
            val first = cipher.seal("message".encodeToByteArray(), key, "context").getOrThrow()
            val second = cipher.seal("message".encodeToByteArray(), key, "context").getOrThrow()

            assertFalse(first.contentEquals(second))
        }

    @Test
    fun aReplySealedWithAnotherKeyDoesNotOpen() =
        runTest {
            assertNull(cipher.openReply(ByteArray(40), cipher.newReplyKey()))
        }
}
