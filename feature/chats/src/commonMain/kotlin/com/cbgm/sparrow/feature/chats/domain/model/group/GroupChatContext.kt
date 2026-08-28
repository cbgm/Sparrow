package com.cbgm.sparrow.feature.chats.domain.model.group

import com.cbgm.sparrow.feature.contacts.domain.model.Contact

data class GroupChatContext(
    val conversation: GroupConversation?,
    val conversationError: Throwable?,
    val administration: GroupAdministrationState,
    val contacts: List<Contact>,
    val profilePictures: Map<String, ByteArray?>,
    val avatarBytes: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupChatContext) return false

        return conversation == other.conversation &&
            conversationError?.message == other.conversationError?.message &&
            administration == other.administration &&
            contacts == other.contacts &&
            profilePictures.contentEquals(other.profilePictures) &&
            avatarBytes.contentEquals(other.avatarBytes)
    }

    override fun hashCode(): Int {
        var result = conversation?.hashCode() ?: 0
        result = 31 * result + (conversationError?.message?.hashCode() ?: 0)
        result = 31 * result + administration.hashCode()
        result = 31 * result + contacts.hashCode()
        result = 31 * result + profilePictures.contentHashCode()
        result = 31 * result + (avatarBytes?.contentHashCode() ?: 0)
        return result
    }
}

private fun Map<String, ByteArray?>.contentEquals(other: Map<String, ByteArray?>): Boolean {
    if (size != other.size || keys != other.keys) return false
    return all { (key, value) -> value.contentEquals(other[key]) }
}

private fun Map<String, ByteArray?>.contentHashCode(): Int =
    entries.fold(0) { result, (key, value) ->
        result + (key.hashCode() xor (value?.contentHashCode() ?: 0))
    }
