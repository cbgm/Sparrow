package com.cbgm.sparrow.feature.chats.domain.model.direct

import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityHandshakeState

data class DirectChatContext(
    val conversation: DirectConversation?,
    val contact: Contact?,
    val handshake: IdentityHandshakeState?,
    val setupMode: DirectIdentitySetupMode,
    val profilePictureBytes: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DirectChatContext) return false

        return conversation == other.conversation &&
            contact == other.contact &&
            handshake == other.handshake &&
            setupMode == other.setupMode &&
            profilePictureBytes.contentEquals(other.profilePictureBytes)
    }

    override fun hashCode(): Int {
        var result = conversation?.hashCode() ?: 0
        result = 31 * result + (contact?.hashCode() ?: 0)
        result = 31 * result + (handshake?.hashCode() ?: 0)
        result = 31 * result + setupMode.hashCode()
        result = 31 * result + (profilePictureBytes?.contentHashCode() ?: 0)
        return result
    }
}
