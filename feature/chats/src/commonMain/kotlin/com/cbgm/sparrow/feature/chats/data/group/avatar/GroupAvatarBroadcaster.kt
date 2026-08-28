package com.cbgm.sparrow.feature.chats.data.group.avatar

import com.cbgm.sparrow.core.protocol.avatar.GroupAvatarMetadata
import com.cbgm.sparrow.core.protocol.avatar.GroupAvatarPayload
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupAvatarDataSource
import com.cbgm.sparrow.feature.chats.data.group.outgoing.GroupPacketBroadcaster
import com.cbgm.sparrow.feature.chats.data.group.security.isGroupAdminRole
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAvatar

internal class GroupAvatarBroadcaster(
    private val groupSecurityDao: GroupSecurityDao,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val packetProtocol: GroupAvatarPacketProtocol,
    private val packetBroadcaster: GroupPacketBroadcaster,
    private val dataSource: GroupAvatarDataSource
) {
    suspend fun requireLocalAdmin(groupId: String): Result<Unit> =
        runCatching { requireAdminContext(groupId) }
            .map { Unit }

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
        val state = groupSecurityDao.findState(groupId) ?: error("Group security state was not found")
        check(state.localRole.isGroupAdminRole()) { "Only a group admin may change the group avatar" }
        val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        check(state.localSigningPublicKey.contentEquals(signingKeyPair.publicKey)) {
            "Local admin signing key does not match the group security state"
        }
        val members = groupSecurityDao.findMemberKeys(groupId, state.currentEpoch)
        val recipients =
            members
                .asSequence()
                .filterNot { member -> member.signingPublicKey.contentEquals(signingKeyPair.publicKey) }
                .map { member -> member.contactId }
                .filter(String::isNotBlank)
                .toSet()
        return AdminContextDto(state.currentEpoch, signingKeyPair, recipients)
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
