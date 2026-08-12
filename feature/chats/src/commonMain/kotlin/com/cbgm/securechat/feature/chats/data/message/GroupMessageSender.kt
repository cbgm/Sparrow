package com.cbgm.securechat.feature.chats.data.message

import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.packet.GroupChatMessagePacket
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.data.database.entity.ConversationParticipantEntity
import com.cbgm.securechat.data.database.entity.GroupInvitationEntity
import com.cbgm.securechat.data.database.entity.MessageEntity
import com.cbgm.securechat.data.database.entity.MessageRecipientStateEntity
import com.cbgm.securechat.feature.chats.data.delivery.MessageDeliveryStateCoordinator
import com.cbgm.securechat.feature.chats.data.invitation.GroupInvitationStatus
import com.cbgm.securechat.feature.chats.data.invitation.canSendToActiveGroupMembers
import com.cbgm.securechat.feature.chats.data.security.GROUP_END_TO_END_ENCRYPTED_MODE
import com.cbgm.securechat.feature.chats.data.security.GroupSecurityManager
import com.cbgm.securechat.feature.chats.domain.model.MessageContentStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryEvent
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GroupMessageSender(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val groupSecurityManager: GroupSecurityManager,
    private val deliveryStateCoordinator: MessageDeliveryStateCoordinator
) {
    private val sendMutex = Mutex()

    suspend fun queueOrSend(
        conversationId: String,
        text: String,
        invitations: List<GroupInvitationEntity>
    ): Result<Unit> =
        runCatching {
            sendMutex.withLock {
                val normalizedText = text.trim()
                require(normalizedText.isNotEmpty()) { "Message text must not be blank" }

                val conversation =
                    chatDao.findConversationById(conversationId)
                        ?: error("Group conversation was not found")
                check(conversation.type == GROUP_CONVERSATION_TYPE) { "Conversation is not a group" }
                check(invitations.none { it.status.isIncomingPendingStatus() }) {
                    "Accept the group invitation before sending messages"
                }
                check(invitations.none { it.status == GroupInvitationStatus.LEAVE_SENT.name }) {
                    "Messages are disabled while the group is being left"
                }
                check(invitations.none { it.status == GroupInvitationStatus.GROUP_DELETED.name }) {
                    "This group conversation was deleted"
                }
                check(
                    invitations.any { invitation ->
                        invitation.status != GroupInvitationStatus.REMOVED.name &&
                            invitation.status != GroupInvitationStatus.GROUP_DELETED.name
                    } ||
                        (
                            !chatDao.hasMessageWithTransportMode(
                                conversationId = conversationId,
                                transportMode = GroupMembershipMessageFactory.LOCAL_MEMBERSHIP_REMOVED_TRANSPORT_MODE
                            ) &&
                                !chatDao.hasMessageWithTransportMode(
                                    conversationId = conversationId,
                                    transportMode = GroupMembershipMessageFactory.LOCAL_MEMBERSHIP_LEFT_TRANSPORT_MODE
                                )
                        )
                ) {
                    "You are no longer a member of this group"
                }

                val message =
                    MessageEntity(
                        id = IdGenerator.generate(prefix = "group-message"),
                        conversationId = conversationId,
                        packetId = null,
                        text = normalizedText,
                        transportPayload = null,
                        transportMode = GROUP_END_TO_END_ENCRYPTED_MODE,
                        contentStatus = MessageContentStatus.READABLE.name,
                        deliveryStatus = MessageDeliveryStatus.QUEUED.name,
                        senderContactId = null,
                        isMine = true,
                        createdAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                    )

                val activeParticipants = findCurrentEpochParticipants(conversationId)
                if (canSendToActiveGroupMembers(activeParticipants.size)) {
                    encryptAndEnqueue(
                        message = message,
                        participants = activeParticipants
                    )
                } else {
                    storeLocalOnly(message)
                }
            }
        }

    private suspend fun findCurrentEpochParticipants(
        conversationId: String
    ): List<ConversationParticipantEntity> =
        chatDao
            .findConversationParticipants(conversationId)
            .filter { participant ->
                val signingPublicKey =
                    contactDao
                        .findPublicIdentityByContactId(participant.contactId)
                        ?.signingPublicKey
                        ?: return@filter false

                groupSecurityManager
                    .isRemoteMemberIdentityCurrent(
                        groupId = conversationId,
                        contactId = participant.contactId,
                        signingPublicKey = signingPublicKey
                    ).getOrDefault(false)
            }

    private suspend fun storeLocalOnly(message: MessageEntity) {
        chatDao.upsertMessage(message.copy(deliveryStatus = MessageDeliveryStatus.FAILED.name))
        chatDao.updateConversationTimestamp(
            conversationId = message.conversationId,
            timestamp = message.createdAtEpochMilliseconds
        )
    }

    private suspend fun encryptAndEnqueue(
        message: MessageEntity,
        participants: List<ConversationParticipantEntity>
    ) {
        check(participants.isNotEmpty()) { "Group has no active participants" }

        val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val securedMessage =
            groupSecurityManager
                .encryptMessage(
                    groupId = message.conversationId,
                    messageId = message.id,
                    sentAtEpochMilliseconds = message.createdAtEpochMilliseconds,
                    plaintext = message.text,
                    localSigningKeyPair = localSigningKeyPair
                ).getOrThrow()
        val packets =
            participants.associateWith { participant ->
                GroupChatMessagePacket(
                    packetId = packetId(message.id, participant.contactId),
                    groupId = message.conversationId,
                    epoch = securedMessage.epoch,
                    messageId = message.id,
                    sentAtEpochMilliseconds = message.createdAtEpochMilliseconds,
                    nonce = securedMessage.nonce.copyOf(),
                    ciphertext = securedMessage.ciphertext.copyOf(),
                    senderSignature = securedMessage.senderSignature.copyOf()
                )
            }
        val recipientStates =
            packets.map { (participant, packet) ->
                MessageRecipientStateEntity(
                    messageId = message.id,
                    contactId = participant.contactId,
                    packetId = packet.packetId,
                    deliveryStatus = MessageDeliveryStatus.QUEUED.name,
                    lastError = null,
                    updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                )
            }

        chatDao.upsertOutgoingGroupMessage(
            message = message.copy(deliveryStatus = MessageDeliveryStatus.QUEUED.name),
            recipientStates = recipientStates,
            timestamp = message.createdAtEpochMilliseconds
        )

        packets.forEach { (participant, packet) ->
            protocolOutbox.enqueue(participant.contactId, packet).getOrElse { error ->
                deliveryStateCoordinator.applyPacketEvent(
                    packetId = packet.packetId,
                    event = MessageDeliveryEvent.SEND_FAILED,
                    errorMessage = error.message
                )
                throw error
            }
        }
    }

    private fun String.isIncomingPendingStatus(): Boolean =
        this == GroupInvitationStatus.AWAITING_ACCEPTANCE.name ||
            this == GroupInvitationStatus.JOIN_SENT.name ||
            this == GroupInvitationStatus.WAITING_FOR_ACTIVATION.name

    private fun packetId(
        messageId: String,
        contactId: String
    ): String = "group-message-$messageId-$contactId"

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
    }
}
