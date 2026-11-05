package com.cbgm.sparrow.core.crypto.claim

import com.cbgm.sparrow.core.crypto.SodiumRuntime
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse

class SodiumAliasClaimSealerTest {
    private val sealer = SodiumAliasClaimSealer()

    @Test
    fun sealsExactlyLikeTheServerExpects() =
        runTest {
            SodiumRuntime.initialize().getOrThrow()

            // Same vector as the server's AliasClaimKeysTest (sealed with Python's `cryptography`).
            val sealed =
                sealer.sealWith(
                    claim = """{"aliases":["scphone1_test"]}""".encodeToByteArray(),
                    presencePublicKey = Base64.decode("B6N8vBQgk8i3VdwbEOhstCY3StFqqFPtC9/AsrhtHHw="),
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
    fun everyClaimUsesItsOwnEphemeralKey() =
        runTest {
            val key = Base64.decode("B6N8vBQgk8i3VdwbEOhstCY3StFqqFPtC9/AsrhtHHw=")
            val first = sealer.seal("claim".encodeToByteArray(), key).getOrThrow()
            val second = sealer.seal("claim".encodeToByteArray(), key).getOrThrow()

            assertFalse(first.contentEquals(second))
        }
}
