package com.cbgm.sparrow.core.crypto.transport

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class DefaultTransportPayloadCodecTest {
    private val codec =
        DefaultTransportPayloadCodec()

    @Test
    fun encodeThenDecodeRestoresPayload() {
        val original =
            EncryptedTransportPayload(
                version = 1,
                mode =
                    TransportEncryptionMode
                        .SEALED_BOX,
                payload =
                    byteArrayOf(
                        1,
                        2,
                        3,
                        4
                    )
            )

        val encoded =
            codec.encode(
                payload = original
            )

        val decoded =
            codec
                .decode(
                    encoded = encoded
                ).getOrThrow()

        assertEquals(
            expected = original.version,
            actual = decoded.version
        )

        assertEquals(
            expected = original.mode,
            actual = decoded.mode
        )

        assertContentEquals(
            expected = original.payload,
            actual = decoded.payload
        )
    }
}
