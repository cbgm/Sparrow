package com.cbgm.securechat.feature.messaging.application.outbox

import com.cbgm.securechat.core.crypto.transport.TransportPayloadCodec
import com.cbgm.securechat.core.protocol.codec.PacketCodec
import com.cbgm.securechat.core.protocol.outbox.OutboxDeliveryStateListener
import com.cbgm.securechat.core.protocol.outbox.OutboxProcessingResult
import com.cbgm.securechat.core.protocol.outbox.OutboxProcessor
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutboxItem
import com.cbgm.securechat.core.protocol.packet.ContactInviteAcceptedPacket
import com.cbgm.securechat.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.securechat.core.protocol.packet.ContactInvitePacket
import com.cbgm.securechat.core.protocol.packet.DeliveryReceiptPacket
import com.cbgm.securechat.core.protocol.packet.GroupChatMessagePacket
import com.cbgm.securechat.core.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.securechat.core.protocol.packet.GroupCreatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.securechat.core.protocol.packet.GroupInvitePacket
import com.cbgm.securechat.core.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.securechat.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.securechat.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.securechat.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.securechat.core.protocol.packet.GroupVerificationReceiptPacket
import com.cbgm.securechat.core.protocol.packet.GroupVerificationSnapshotPacket
import com.cbgm.securechat.core.protocol.packet.GroupVerificationSnapshotRequestPacket
import com.cbgm.securechat.core.protocol.packet.ReadReceiptPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.core.protocol.transport.OutgoingWireSender
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import com.cbgm.securechat.feature.messaging.domain.relay.ContactRelayIdResolver
import com.cbgm.securechat.feature.messaging.domain.relay.GroupRelayIdResolver
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class DefaultOutboxProcessor(
    private val protocolOutbox: ProtocolOutbox,
    private val getContact: GetContact,
    private val transportPayloadFactory: OutgoingTransportPayloadFactory,
    private val transportPayloadCodec: TransportPayloadCodec,
    private val packetCodec: PacketCodec,
    private val contactRelayIdResolver: ContactRelayIdResolver,
    private val groupRelayIdResolver: GroupRelayIdResolver,
    private val outgoingWireSender: OutgoingWireSender,
    private val deliveryStateListener: OutboxDeliveryStateListener
) : OutboxProcessor {
    override suspend fun processPending(limit: Int): Result<OutboxProcessingResult> =
        runCatching {
            require(limit > 0) {
                "Outbox processing limit must be positive"
            }

            val pendingItems = protocolOutbox.getPending(limit = limit).getOrThrow()
            val results = processByRecipient(pendingItems)

            OutboxProcessingResult(
                processedCount = results.size,
                sentCount = results.count { result -> result.isSuccess },
                failedCount = results.count { result -> result.isFailure }
            )
        }

    private suspend fun processByRecipient(
        pendingItems: List<ProtocolOutboxItem>
    ): List<Result<Unit>> =
        coroutineScope {
            val slots = Semaphore(MAX_CONCURRENT_RECIPIENTS)

            pendingItems
                .groupBy(ProtocolOutboxItem::contactId)
                .values
                .map { recipientItems ->
                    async {
                        slots.withPermit {
                            recipientItems.map { item ->
                                processItem(item = item)
                            }
                        }
                    }
                }.awaitAll()
                .flatten()
        }

    private suspend fun processItem(item: ProtocolOutboxItem): Result<Unit> {
        val processingResult = protocolOutbox.markProcessing(itemId = item.id)

        if (processingResult.isFailure) {
            return processingResult
        }

        val sendResult =
            runCatching {
                deliveryStateListener.onProcessing(packetId = item.packetId).getOrThrow()
                prepareAndSend(item)
            }

        if (sendResult.isFailure) {
            return markFailed(
                item = item,
                error = sendResult.exceptionOrNull()
            )
        }

        return deliveryStateListener.onSent(packetId = item.packetId)
    }

    private suspend fun markFailed(
        item: ProtocolOutboxItem,
        error: Throwable?
    ): Result<Unit> {
        val errorMessage = error?.message ?: "Outgoing packet could not be sent"

        protocolOutbox
            .markFailed(
                itemId = item.id,
                errorMessage = errorMessage
            ).getOrElse { markFailedError ->
                return Result.failure(markFailedError)
            }

        deliveryStateListener
            .onFailed(
                packetId = item.packetId,
                errorMessage = errorMessage
            ).getOrElse { listenerError ->
                return Result.failure(listenerError)
            }

        return Result.failure(error ?: IllegalStateException(errorMessage))
    }

    private suspend fun prepareAndSend(item: ProtocolOutboxItem) {
        val contact =
            getContact(contactId = item.contactId).getOrThrow()
                ?: error("Outbox contact was not found")

        val packet = packetCodec.decode(item.encodedPacket).getOrThrow()
        val transportPayload =
            transportPayloadFactory
                .create(
                    encodedPacket = item.encodedPacket,
                    contact = contact,
                    packet = packet
                ).getOrThrow()

        val encodedTransportPayload = transportPayloadCodec.encode(payload = transportPayload)

        deliveryStateListener
            .onPrepared(
                packetId = item.packetId,
                encodedTransportPayload = encodedTransportPayload,
                transportMode = transportPayload.mode.name
            ).getOrThrow()

        val recipientRelayId =
            resolveRecipientRelayId(
                contactId = item.contactId,
                packet = packet
            )

        outgoingWireSender
            .send(
                recipientAddress = recipientRelayId,
                encodedTransportPayload = encodedTransportPayload
            ).getOrThrow()

        protocolOutbox.markSent(itemId = item.id).getOrThrow()
    }

    private suspend fun resolveRecipientRelayId(
        contactId: String,
        packet: SecureChatPacket
    ): String =
        when (packet) {
            is ContactInvitePacket,
            is ContactInviteAcceptedPacket,
            is ContactInviteDeclinedPacket,
            is GroupInvitePacket,
            is GroupInviteReceivedPacket,
            is GroupJoinRequestPacket,
            is GroupInviteDeclinedPacket ->
                contactRelayIdResolver.resolveBootstrap(contactId).getOrThrow()

            is GroupConversationDeletedPacket ->
                resolveGroupOrBootstrap(
                    groupId = packet.groupId,
                    contactId = contactId,
                    useBootstrap = packet.epoch == GroupConversationDeletedPacket.PENDING_GROUP_EPOCH
                )

            is GroupMemberRemovedPacket ->
                if (packet.epoch == GroupMemberRemovedPacket.PENDING_INVITATION_EPOCH) {
                    contactRelayIdResolver.resolveBootstrap(contactId).getOrThrow()
                } else {
                    groupRelayIdResolver
                        .resolveRemovedMember(packet.removedMemberSigningPublicKey)
                        .getOrThrow()
                }

            is DeliveryReceiptPacket ->
                resolveReceiptRecipient(
                    messageId = packet.messageId,
                    contactId = contactId
                )

            is ReadReceiptPacket ->
                resolveReceiptRecipient(
                    messageId = packet.messageId,
                    contactId = contactId
                )

            else ->
                packet.groupIdForRouting()
                    ?.let { groupId -> groupRelayIdResolver.resolve(groupId, contactId).getOrThrow() }
                    ?: contactRelayIdResolver.resolve(contactId).getOrThrow()
        }

    private suspend fun resolveGroupOrBootstrap(
        groupId: String,
        contactId: String,
        useBootstrap: Boolean
    ): String =
        if (useBootstrap) {
            contactRelayIdResolver.resolveBootstrap(contactId).getOrThrow()
        } else {
            groupRelayIdResolver.resolve(groupId, contactId).getOrThrow()
        }

    private suspend fun resolveReceiptRecipient(
        messageId: String,
        contactId: String
    ): String =
        groupRelayIdResolver
            .resolveForMessage(messageId, contactId)
            .getOrThrow()
            ?: contactRelayIdResolver.resolve(contactId).getOrThrow()

    private fun SecureChatPacket.groupIdForRouting(): String? =
        when (this) {
            is GroupChatMessagePacket -> groupId
            is GroupCreatedPacket -> groupId
            is GroupLeaveRequestPacket -> groupId
            is GroupMemberActivatedPacket -> groupId
            is GroupMemberActivationAcknowledgementPacket -> groupId
            is GroupReadyAcknowledgementPacket -> groupId
            is GroupVerificationReceiptPacket -> groupId
            is GroupVerificationSnapshotRequestPacket -> groupId
            is GroupVerificationSnapshotPacket -> groupId
            else -> null
        }

    private companion object {
        const val MAX_CONCURRENT_RECIPIENTS = 8
    }
}
