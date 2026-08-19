package com.cbgm.sparrow.feature.search.data.embedding

import kotlin.test.Test
import kotlin.test.assertContentEquals

class EmbeddingCodecTest {
    @Test
    fun encodeDecode_roundTripsFloatArray() {
        val source = floatArrayOf(-1.5f, 0f, 0.25f, 42.125f)

        val decoded = EmbeddingCodec.decode(EmbeddingCodec.encode(source))

        assertContentEquals(source, decoded)
    }
}
