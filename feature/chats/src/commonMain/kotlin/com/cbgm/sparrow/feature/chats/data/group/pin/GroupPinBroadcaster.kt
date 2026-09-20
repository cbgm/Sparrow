package com.cbgm.sparrow.feature.chats.data.group.pin

import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.message.GroupMessageContent
import com.cbgm.sparrow.core.protocol.message.GroupMessageContentCodec
import com.cbgm.sparrow.data.database.entity.GroupPinEntity
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupPinDataSource
import com.cbgm.sparrow.feature.chats.data.group.outgoing.GroupPacketBroadcaster
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeGroupMetadataUseCase

internal class GroupPinBroadcaster(
    private val authorizeGroupMetadata: AuthorizeGroupMetadataUseCase,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val packetProtocol: GroupPinPacketProtocol,
    private val packetBroadcaster: GroupPacketBroadcaster,
    private val dataSource: GroupPinDataSource,
    private val groupMessageContentCodec: GroupMessageContentCodec
) {
    suspend fun requireLocalAdmin(groupId: String): Result<Unit> =
        runCatching { requireAdminContext(groupId).let { } }

    suspend fun broadcast(groupId: String): Result<Unit> =
        runCatching {
            val context = requireAdminContext(groupId)
            val state = dataSource.get(groupId) ?: return@runCatching
            val packets =
                context.recipientContactIds.associateWith {
                    state.toPacket(context).getOrThrow()
                }
            packetBroadcaster.enqueueAll(packets).getOrThrow()
        }

    suspend fun sendCurrentTo(
        groupId: String,
        contactId: String
    ): Result<Unit> =
        runCatching {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            val state = dataSource.get(groupId) ?: return@runCatching
            val context = requireAdminContext(groupId)
            check(contactId in context.recipientContactIds) { "Contact is not an active group member" }

            packetBroadcaster
                .enqueueAll(
                    mapOf(
                        contactId to state.toPacket(context).getOrThrow()
                    )
                ).getOrThrow()
        }

    private suspend fun GroupPinEntity.toPacket(context: AdminContextDto) =
        toPacket(
            context = context,
            messageContent = messageContent?.let(groupMessageContentCodec::decode)
        )

    private suspend fun GroupPinEntity.toPacket(
        context: AdminContextDto,
        messageContent: GroupMessageContent?
    ) =
        packetProtocol.create(
            groupId = groupId,
            epoch = context.epoch,
            messageId = messageId,
            messageSentAtEpochMilliseconds = messageSentAtEpochMilliseconds ?: 0L,
            messageSenderSigningPublicKey = messageSenderSigningPublicKey?.copyOf() ?: byteArrayOf(),
            messageContent = messageContent,
            changedAtEpochMilliseconds = changedAtEpochMilliseconds,
            adminSigningKeyPair = context.signingKeyPair
        )

    private suspend fun requireAdminContext(groupId: String): AdminContextDto {
        val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val authorized = authorizeGroupMetadata.send(
            groupId = groupId,
            localSigningPublicKey = signingKeyPair.publicKey,
            action = "pinned message"
        ).getOrThrow()
        return AdminContextDto(
            epoch = authorized.epoch,
            signingKeyPair = signingKeyPair,
            recipientContactIds = authorized.recipientContactIds
        )
    }

    private data class AdminContextDto(
        val epoch: Int,
        val signingKeyPair: LocalSigningKeyPair,
        val recipientContactIds: Set<String>
    )
}
