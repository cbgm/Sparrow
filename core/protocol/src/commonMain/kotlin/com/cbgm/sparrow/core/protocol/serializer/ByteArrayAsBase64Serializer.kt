package com.cbgm.sparrow.core.protocol.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object ByteArrayAsBase64Serializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(
            serialName = "SparrowBase64ByteArray",
            kind = PrimitiveKind.STRING
        )

    @OptIn(ExperimentalEncodingApi::class)
    override fun serialize(
        encoder: Encoder,
        value: ByteArray
    ) {
        encoder.encodeString(Base64.encode(value))
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun deserialize(decoder: Decoder): ByteArray {
        val encoded = decoder.decodeString()

        if (encoded.isEmpty()) {
            return byteArrayOf()
        }

        return Base64.decode(encoded)
    }
}
