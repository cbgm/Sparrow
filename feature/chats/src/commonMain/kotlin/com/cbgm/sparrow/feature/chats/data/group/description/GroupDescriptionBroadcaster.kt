package com.cbgm.sparrow.feature.chats.data.group.description

import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupDescriptionDataSource
import com.cbgm.sparrow.feature.chats.data.group.outgoing.GroupPacketBroadcaster
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeGroupMetadataUseCase

internal class GroupDescriptionBroadcaster(
    private val authorizeGroupMetadata: AuthorizeGroupMetadataUseCase,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val packetProtocol: GroupDescriptionPacketProtocol,
    private val packetBroadcaster: GroupPacketBroadcaster,
    private val dataSource: GroupDescriptionDataSource
) {
    suspend fun requireLocalAdmin(groupId: String): Result<Unit> =
        runCatching { requireAdminContext(groupId).let { } }

    suspend fun broadcast(groupId: String): Result<Unit> =
        runCatching {
            val context = requireAdminContext(groupId)
            val description = dataSource.get(groupId)
            if (description.changedAtEpochMilliseconds == 0L) return@runCatching
            val packets =
                context.recipientContactIds.associateWith {
                    packetProtocol
                        .create(
                            groupId = groupId,
                            epoch = context.epoch,
                            description = description.description,
                            changedAtEpochMilliseconds = description.changedAtEpochMilliseconds,
                            adminSigningKeyPair = context.signingKeyPair
                        ).getOrThrow()
                }
            packetBroadcaster.enqueueAll(packets).getOrThrow()
        }

    suspend fun sendCurrentTo(
        groupId: String,
        contactId: String
    ): Result<Unit> =
        runCatching {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            val description = dataSource.get(groupId)
            if (description.changedAtEpochMilliseconds == 0L) return@runCatching
            val context = requireAdminContext(groupId)
            check(contactId in context.recipientContactIds) { "Contact is not an active group member" }
            val packet =
                packetProtocol
                    .create(
                        groupId = groupId,
                        epoch = context.epoch,
                        description = description.description,
                        changedAtEpochMilliseconds = description.changedAtEpochMilliseconds,
                        adminSigningKeyPair = context.signingKeyPair
                    ).getOrThrow()
            packetBroadcaster.enqueueAll(mapOf(contactId to packet)).getOrThrow()
        }

    private suspend fun requireAdminContext(groupId: String): AdminContextDto {
        val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val authorized = authorizeGroupMetadata.send(
            groupId = groupId,
            localSigningPublicKey = signingKeyPair.publicKey,
            action = "group description"
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
