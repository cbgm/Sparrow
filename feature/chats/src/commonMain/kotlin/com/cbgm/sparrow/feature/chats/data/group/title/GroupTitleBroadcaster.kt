package com.cbgm.sparrow.feature.chats.data.group.title

import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupTitleDataSource
import com.cbgm.sparrow.feature.chats.data.group.outgoing.GroupPacketBroadcaster
import com.cbgm.sparrow.feature.membership.data.model.isGroupAdminRole

internal class GroupTitleBroadcaster(
    private val groupSecurityDao: GroupSecurityDao,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val packetProtocol: GroupTitlePacketProtocol,
    private val packetBroadcaster: GroupPacketBroadcaster,
    private val dataSource: GroupTitleDataSource
) {
    suspend fun requireLocalAdmin(groupId: String): Result<Unit> =
        runCatching { requireAdminContext(groupId) }
            .map { Unit }

    suspend fun broadcast(groupId: String): Result<Unit> =
        runCatching {
            val context = requireAdminContext(groupId)
            val title = dataSource.get(groupId)
            if (title.changedAtEpochMilliseconds == 0L) return@runCatching
            val packets =
                context.recipientContactIds.associateWith {
                    packetProtocol
                        .create(
                            groupId = groupId,
                            epoch = context.epoch,
                            title = title.title,
                            changedAtEpochMilliseconds = title.changedAtEpochMilliseconds,
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
            val title = dataSource.get(groupId)
            if (title.changedAtEpochMilliseconds == 0L) return@runCatching
            val context = requireAdminContext(groupId)
            check(contactId in context.recipientContactIds) { "Contact is not an active group member" }
            val packet =
                packetProtocol
                    .create(
                        groupId = groupId,
                        epoch = context.epoch,
                        title = title.title,
                        changedAtEpochMilliseconds = title.changedAtEpochMilliseconds,
                        adminSigningKeyPair = context.signingKeyPair
                    ).getOrThrow()
            packetBroadcaster.enqueueAll(mapOf(contactId to packet)).getOrThrow()
        }

    private suspend fun requireAdminContext(groupId: String): AdminContextDto {
        val state = groupSecurityDao.findState(groupId) ?: error("Group security state was not found")
        check(state.localRole.isGroupAdminRole()) { "Only a group admin may change the group title" }
        val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        check(state.localSigningPublicKey.contentEquals(signingKeyPair.publicKey)) {
            "Local admin signing key does not match the group security state"
        }
        val recipients =
            groupSecurityDao
                .findMemberKeys(groupId, state.currentEpoch)
                .asSequence()
                .filterNot { member -> member.signingPublicKey.contentEquals(signingKeyPair.publicKey) }
                .map { member -> member.contactId }
                .filter(String::isNotBlank)
                .toSet()
        return AdminContextDto(
            epoch = state.currentEpoch,
            signingKeyPair = signingKeyPair,
            recipientContactIds = recipients
        )
    }

    private data class AdminContextDto(
        val epoch: Int,
        val signingKeyPair: LocalSigningKeyPair,
        val recipientContactIds: Set<String>
    )
}
