package com.cbgm.sparrow.feature.chats.data.group.incoming

import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentity
import com.cbgm.sparrow.data.database.entity.ConversationParticipantEntity
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.feature.chats.data.group.security.OpenedGroupWelcome

internal data class GroupWelcomeContext(
    val authorityIdentity: GroupWelcomeAuthorityIdentity,
    val localIdentity: LocalPublicIdentity,
    val openedWelcome: OpenedGroupWelcome
)

internal data class GroupWelcomeAuthorityIdentity(
    val encryptionPublicKey: ByteArray,
    val signingPublicKey: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GroupWelcomeAuthorityIdentity

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

internal data class PreviousGroupMembership(
    val participants: List<ConversationParticipantEntity>,
    val signingKeysByContactId: Map<String, ByteArray?>
)

internal data class ResolvedGroupMembership(
    val participants: List<ConversationParticipantEntity>,
    val memberKeys: List<GroupMemberKeyEntity>
)

internal data class GroupWelcomeReferenceAdmin(
    val contactId: String,
    val signingPublicKey: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GroupWelcomeReferenceAdmin

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
