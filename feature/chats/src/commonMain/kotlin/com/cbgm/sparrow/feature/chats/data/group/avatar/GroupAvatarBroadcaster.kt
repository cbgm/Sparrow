package com.cbgm.sparrow.feature.chats.data.group.avatar

import com.cbgm.sparrow.core.protocol.avatar.GroupAvatarMetadata
import com.cbgm.sparrow.core.protocol.avatar.GroupAvatarPayload
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupAvatarDataSource
import com.cbgm.sparrow.feature.chats.data.group.outgoing.GroupPacketBroadcaster
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAvatar
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeGroupMetadataUseCase

internal class GroupAvatarBroadcaster(
    private val authorizeGroupMetadata: AuthorizeGroupMetadataUseCase,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val packetProtocol: GroupAvatarPacketProtocol,
    private val packetBroadcaster: GroupPacketBroadcaster,
    private val dataSource: GroupAvatarDataSource
) {
    suspend fun requireLocalAdmin(groupId: String): Result<Unit> =
        runCatching { requireAdminContext(groupId).let { } }

    suspend fun broadcast(groupId: String): Result<Unit> =
        runCatching {
            val context = requireAdminContext(groupId)
            val avatar = dataSource.get(groupId)
            if (avatar.changedAtEpochMilliseconds == 0L) return@runCatching
            val metadata = avatar.toGroupAvatarMetadata()
            val packets =
                context.recipientContactIds.associateWith {
                    packetProtocol
                        .create(groupId, context.epoch, metadata, context.signingKeyPair)
                        .getOrThrow()
                }
            packetBroadcaster.enqueueAll(packets).getOrThrow()
        }

    suspend fun sendCurrentTo(
        groupId: String,
        contactId: String
    ): Result<Unit> =
        runCatching {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            val avatar = dataSource.get(groupId)
            if (avatar.changedAtEpochMilliseconds == 0L) return@runCatching
            val context = requireAdminContext(groupId)
            check(contactId in context.recipientContactIds) { "Contact is not an active group member" }
            val packet = packetProtocol.create(groupId, context.epoch, avatar.toGroupAvatarMetadata(), context.signingKeyPair).getOrThrow()
            packetBroadcaster.enqueueAll(mapOf(contactId to packet)).getOrThrow()
        }

    private suspend fun requireAdminContext(groupId: String): AdminContextDto {
        val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val authorized = authorizeGroupMetadata.send(
            groupId = groupId,
            localSigningPublicKey = signingKeyPair.publicKey,
            action = "group avatar"
        ).getOrThrow()
        return AdminContextDto(
            epoch = authorized.epoch,
            signingKeyPair = signingKeyPair,
            recipientContactIds = authorized.recipientContactIds
        )
    }

    private fun GroupAvatar.toGroupAvatarMetadata(): GroupAvatarMetadata =
        GroupAvatarMetadata(
            changedAtEpochMilliseconds = changedAtEpochMilliseconds,
            hasAvatar = hasAvatar,
            payload = bytes?.let { GroupAvatarPayload(it.copyOf()) }
        )

    private data class AdminContextDto(
        val epoch: Int,
        val signingKeyPair: com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair,
        val recipientContactIds: Set<String>
    )
}
