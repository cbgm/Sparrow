package com.cbgm.sparrow.feature.conversationorchestration.incoming.runtime

import com.cbgm.sparrow.core.crypto.error.SignatureVerificationException
import com.cbgm.sparrow.core.crypto.transport.DecodedTransportMessage
import com.cbgm.sparrow.core.crypto.transport.IncomingTransportMessageDecoder
import com.cbgm.sparrow.core.crypto.transport.TransportEncryptionMode
import com.cbgm.sparrow.core.message.domain.model.MessageContentStatus
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.StoreUnreadableMessageUseCase
import com.cbgm.sparrow.protocol.codec.PacketCodec
import com.cbgm.sparrow.protocol.handler.IncomingMessageHandler
import com.cbgm.sparrow.protocol.handler.IncomingMessageRejectedException

/**
 * Transport boundary for incoming messages: decrypts and decodes, then hands every readable packet to the
 * routing workflow. Unreadable messages are kept; packets with invalid signatures are rejected permanently.
 */
internal class IncomingTransportMessageHandler(
    private val transportMessageDecoder: IncomingTransportMessageDecoder,
    private val packetCodec: PacketCodec,
    private val engine: WorkflowEngine,
    private val storeUnreadableMessage: StoreUnreadableMessageUseCase
) : IncomingMessageHandler {
    override suspend fun handle(
        contactId: String,
        encodedTransportPayload: String,
        localEncryptionPublicKey: ByteArray,
        localEncryptionPrivateKey: ByteArray
    ) {
        require(encodedTransportPayload.isNotBlank()) { "Incoming transport payload must not be blank" }
        val receivedAt = SystemClock.nowEpochMilliseconds()
        when (
            val decoded =
                transportMessageDecoder.decode(
                    encodedTransportPayload,
                    localEncryptionPublicKey,
                    localEncryptionPrivateKey
                )
        ) {
            is DecodedTransportMessage.Readable -> route(contactId, encodedTransportPayload, decoded, receivedAt)
            is DecodedTransportMessage.InvalidPacket ->
                storeUnreadable(contactId, encodedTransportPayload, Unreadable.INVALID_PACKET, receivedAt)
            is DecodedTransportMessage.InvalidPlaintext ->
                storeUnreadable(contactId, encodedTransportPayload, Unreadable.INVALID_PLAINTEXT, receivedAt)
            is DecodedTransportMessage.DecryptionFailed ->
                storeUnreadable(contactId, encodedTransportPayload, Unreadable.DECRYPTION_FAILED, receivedAt)
        }
    }

    private suspend fun route(
        contactId: String,
        encodedTransportPayload: String,
        decoded: DecodedTransportMessage.Readable,
        receivedAt: Long
    ) {
        val packet =
            packetCodec.decode(decoded.plaintext).getOrElse { error ->
                throw IncomingMessageRejectedException(message = "Invalid protocol packet", cause = error)
            }
        engine
            .run(
                IncomingCommand.RouteIncomingPacket(
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

    private suspend fun storeUnreadable(contactId: String, payload: String, unreadable: Unreadable, receivedAt: Long) {
        storeUnreadableMessage(
            contactId = contactId,
            encodedTransportPayload = payload,
            text = unreadable.text,
            transportMode = unreadable.transportMode,
            contentStatus = unreadable.status,
            receivedAtEpochMilliseconds = receivedAt
        ).getOrThrow()
    }

    private enum class Unreadable(
        val text: String,
        val transportMode: String,
        val status: MessageContentStatus
    ) {
        INVALID_PACKET("Invalid transport packet", "UNKNOWN", MessageContentStatus.INVALID_PACKET),
        INVALID_PLAINTEXT(
            "Unable to read plaintext message",
            TransportEncryptionMode.PLAINTEXT.name,
            MessageContentStatus.INVALID_PLAINTEXT_PACKET
        ),
        DECRYPTION_FAILED(
            "Unable to decrypt secure message",
            TransportEncryptionMode.SEALED_BOX.name,
            MessageContentStatus.TRANSPORT_DECRYPTION_FAILED
        )
    }
}
