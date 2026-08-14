package com.cbgm.sparrow.core.crypto.transport

interface IncomingTransportMessageDecoder {
    suspend fun decode(
        encodedPayload: String,
        localPublicKey: ByteArray,
        localPrivateKey: ByteArray
    ): DecodedTransportMessage
}
