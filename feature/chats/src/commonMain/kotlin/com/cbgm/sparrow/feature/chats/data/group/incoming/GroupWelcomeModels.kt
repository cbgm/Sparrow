package com.cbgm.sparrow.feature.chats.data.group.incoming

import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentity
import com.cbgm.sparrow.data.database.entity.ConversationParticipantEntity
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.feature.chats.data.group.security.OpenedGroupWelcomeDto

internal data class GroupWelcomeContextDto(
    val authorityIdentity: GroupWelcomeAuthorityIdentityDto,
    val localIdentity: LocalPublicIdentity,
    val openedWelcome: OpenedGroupWelcomeDto
)

internal data class GroupWelcomeAuthorityIdentityDto(
    val encryptionPublicKey: ByteArray,
    val signingPublicKey: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GroupWelcomeAuthorityIdentityDto

        if (!encryptionPublicKey.contentEquals(other.encryptionPublicKey)) return false
        if (!signingPublicKey.contentEquals(other.signingPublicKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encryptionPublicKey.contentHashCode()
        result = 31 * result + signingPublicKey.contentHashCode()
        return result
    }
}

internal data class PreviousGroupMembershipDto(
    val participants: List<ConversationParticipantEntity>,
    val signingKeysByContactId: Map<String, ByteArray?>
)

internal data class ResolvedGroupMembershipDto(
    val participants: List<ConversationParticipantEntity>,
    val memberKeys: List<GroupMemberKeyEntity>
)

internal data class GroupWelcomeReferenceAdminDto(
    val contactId: String,
    val signingPublicKey: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GroupWelcomeReferenceAdminDto

        if (contactId != other.contactId) return false
        if (!signingPublicKey.contentEquals(other.signingPublicKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contactId.hashCode()
        result = 31 * result + signingPublicKey.contentHashCode()
        return result
    }
}
