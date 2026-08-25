package com.cbgm.sparrow.feature.chats.data.incoming

import com.cbgm.sparrow.core.crypto.error.SignatureVerificationException
import com.cbgm.sparrow.core.crypto.transport.DecodedTransportMessage
import com.cbgm.sparrow.core.crypto.transport.IncomingTransportMessageDecoder
import com.cbgm.sparrow.core.crypto.transport.TransportEncryptionMode
import com.cbgm.sparrow.core.protocol.codec.PacketCodec
import com.cbgm.sparrow.core.protocol.handler.IncomingMessageHandler
import com.cbgm.sparrow.core.protocol.handler.IncomingMessageRejectedException
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.chats.data.datasource.UnreadableTransportMessageDataSource
import com.cbgm.sparrow.feature.chats.data.model.DecodedIncomingPacket
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus

/**
 * Transport boundary for incoming messages.
 *
 * It only decodes transport/protocol data. Decoded packets are handed to
 * [IncomingPacketRouter], which keeps Direct and Group routing explicit.
 */
class IncomingPacketProcessor(
    private val transportMessageDecoder: IncomingTransportMessageDecoder,
    private val packetCodec: PacketCodec,
    private val packetRouter: IncomingPacketRouter,
    private val unreadableMessageDataSource: UnreadableTransportMessageDataSource
) : IncomingMessageHandler {
    override suspend fun handle(
        contactId: String,
        encodedTransportPayload: String,
        localEncryptionPublicKey: ByteArray,
        localEncryptionPrivateKey: ByteArray
    ) {
        require(encodedTransportPayload.isNotBlank()) {
            "Incoming transport payload must not be blank"
        }

        val receivedAt = SystemClock.nowEpochMilliseconds()
        when (
            val decoded =
                transportMessageDecoder.decode(
                    encodedPayload = encodedTransportPayload,
                    localPublicKey = localEncryptionPublicKey,
                    localPrivateKey = localEncryptionPrivateKey
                )
        ) {
            is DecodedTransportMessage.Readable ->
                processReadable(
                    contactId = contactId,
                    encodedTransportPayload = encodedTransportPayload,
                    decoded = decoded,
                    receivedAt = receivedAt
                )

            is DecodedTransportMessage.InvalidPacket ->
                storeUnreadable(
                    contactId = contactId,
                    payload = encodedTransportPayload,
                    text = "Invalid transport packet",
                    transportMode = UNKNOWN_TRANSPORT_MODE,
                    status = MessageContentStatus.INVALID_PACKET,
                    receivedAt = receivedAt
                )

            is DecodedTransportMessage.InvalidPlaintext ->
                storeUnreadable(
                    contactId = contactId,
                    payload = encodedTransportPayload,
                    text = "Unable to read plaintext message",
                    transportMode = TransportEncryptionMode.PLAINTEXT.name,
                    status = MessageContentStatus.INVALID_PLAINTEXT_PACKET,
                    receivedAt = receivedAt
                )

            is DecodedTransportMessage.DecryptionFailed ->
                storeUnreadable(
                    contactId = contactId,
                    payload = encodedTransportPayload,
                    text = "Unable to decrypt secure message",
                    transportMode = TransportEncryptionMode.SEALED_BOX.name,
                    status = MessageContentStatus.TRANSPORT_DECRYPTION_FAILED,
                    receivedAt = receivedAt
                )
        }
    }

    private suspend fun processReadable(
        contactId: String,
        encodedTransportPayload: String,
        decoded: DecodedTransportMessage.Readable,
        receivedAt: Long
    ) {
        val packet =
            packetCodec.decode(decoded.plaintext).getOrElse { error ->
                throw IncomingMessageRejectedException(
                    message = "Invalid protocol packet",
                    cause = error
                )
            }

        packetRouter
            .route(
                DecodedIncomingPacket(
                    contactId = contactId,
                    packet = packet,
                    encodedTransportPayload = encodedTransportPayload,
                    transportMode = decoded.mode.name,
                    receivedAtEpochMilliseconds = receivedAt
                )
            ).getOrElse { error ->
                if (error is SignatureVerificationException) {
                    throw IncomingMessageRejectedException(
                        message = "Incoming packet signature is invalid",
                        cause = error
                    )
                }
                throw error
            }
    }

    private suspend fun storeUnreadable(
        contactId: String,
        payload: String,
        text: String,
        transportMode: String,
        status: MessageContentStatus,
        receivedAt: Long
    ) {
        unreadableMessageDataSource.store(
            contactId = contactId,
            encodedTransportPayload = payload,
            text = text,
            transportMode = transportMode,
            contentStatus = status,
            receivedAtEpochMilliseconds = receivedAt
        )
    }

    private companion object {
        const val UNKNOWN_TRANSPORT_MODE = "UNKNOWN"
    }
}
