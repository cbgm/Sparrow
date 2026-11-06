package com.cbgm.sparrow.core.crypto.circuit

import com.cbgm.sparrow.core.crypto.SodiumRuntime
import kotlinx.coroutines.test.runTest
import org.kotlincrypto.hash.sha2.SHA256
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SodiumCircuitCipherTest {
    private val cipher = SodiumCircuitCipher()

    // Same vector as the server's CircuitCryptoTest (computed with Python's `cryptography`).
    private val ephemeral =
        CircuitEphemeralKey(
            secretKey = ByteArray(32) { (it + 1).toByte() },
            publicKey = Base64.decode("B6N8vBQgk8i3VdwbEOhstCY3StFqqFPtC9/AsrhtHHw=")
        )
    private val relayKey = Base64.decode("ZLEBsdC+WocEvQePmJUAH8A+jp+VIvGI3RKNmEbUhGY=")
    private val reply =
        Base64.decode("WGmv9FBUlzLLqu1eXfmzCm2jHLDldCutWtShp2jxpns=") +
            Base64.decode("sfwbVl7en5UiHrOX38G5utD1B8b6Hpy+9waDWBIW3ls=")

    @Test
    fun derivesTheSameHopKeysAndLayersAsTheServer() =
        runTest {
            SodiumRuntime.initialize().getOrThrow()

            val keys = assertNotNull(cipher.completeHandshake(ephemeral, relayKey, reply))

            assertContentEquals(Base64.decode("Tr+9V0PnzHTKPU3TTasAhyOMyzK68S6O/bZCgf9rj80="), keys.forward)
            assertContentEquals(Base64.decode("wI5LVQDO7Zfm+n8aJrm/y4SUfChDDhcjINjO1RCwQrs="), keys.backward)
            val data = ByteArray(1019) { it.toByte() }
            assertContentEquals(
                Base64.decode("BdHkrcfhdbOAE5Lp3/dQmZyofKxOmCpHPLlvM33Hxi0="),
                SHA256().digest(cipher.stream(keys.forward, 5, data))
            )
            assertContentEquals(
                Base64.decode("cPg+FiuW4Sn6iQxW1DvhbwVDe6xWAn7zTXar0MBFzSg="),
                SHA256().digest(cipher.seal(keys.backward, 9, data.copyOf(1003)))
            )
        }

    @Test
    fun aReplyWithoutTheRelaysProofIsRefused() =
        runTest {
            SodiumRuntime.initialize().getOrThrow()
            val forged = reply.copyOf().also { bytes -> bytes[40] = (bytes[40] + 1).toByte() }

            assertNull(cipher.completeHandshake(ephemeral, relayKey, forged))
        }
}
