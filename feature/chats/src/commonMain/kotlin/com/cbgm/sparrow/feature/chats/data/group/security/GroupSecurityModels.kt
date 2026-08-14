package com.cbgm.sparrow.feature.chats.data.group.security

import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket

data class GroupWelcomeRecipient(
    val contactId: String,
    val invitationId: String,
    val encryptionPublicKey: ByteArray
)

data class CreatedGroupSecurity(
    val welcomePacketsByContactId: Map<String, GroupCreatedPacket>
)

data class OpenedGroupWelcome(
    val packet: GroupCreatedPacket,
    val groupKey: ByteArray
)

data class SecuredGroupMessage(
    val epoch: Int,
    val nonce: ByteArray,
    val ciphertext: ByteArray,
    val senderSignature: ByteArray
)
