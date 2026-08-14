package com.cbgm.sparrow.core.protocol.handler

/**
 * Application boundary for a transport message that is ready to be decoded.
 *
 * Transport runners resolve the sender and local key pair, then delegate here.
 * Feature repositories remain concerned with conversations instead of wire data.
 */
interface IncomingMessageHandler {
    suspend fun handle(
        contactId: String,
        encodedTransportPayload: String,
        localEncryptionPublicKey: ByteArray,
        localEncryptionPrivateKey: ByteArray
    )
}
