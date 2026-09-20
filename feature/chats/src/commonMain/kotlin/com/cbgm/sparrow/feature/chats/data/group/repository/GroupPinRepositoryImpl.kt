package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.core.protocol.attachment.GroupPinnedAttachmentProvider
import com.cbgm.sparrow.core.protocol.message.GroupMessageContent
import com.cbgm.sparrow.core.protocol.message.GroupMessageContentCodec
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupPinDataSource
import com.cbgm.sparrow.feature.chats.data.group.mapper.createGroupPinEntity
import com.cbgm.sparrow.feature.chats.data.group.mapper.toDomain
import com.cbgm.sparrow.feature.chats.data.group.mapper.unpinnedGroupPinEntity
import com.cbgm.sparrow.feature.chats.data.group.pin.GroupPinBroadcaster
import com.cbgm.sparrow.feature.chats.data.group.security.GROUP_END_TO_END_ENCRYPTED_MODE
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupPin
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupPinTarget
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupPinRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachment as ProtocolMessageAttachment

internal class GroupPinRepositoryImpl(
    private val attachmentDataSource: MessageAttachmentOperationsRepository,
    private val dataSource: GroupPinDataSource,
    private val broadcaster: GroupPinBroadcaster,
    private val groupMessageContentCodec: GroupMessageContentCodec
) : GroupPinRepository,
    GroupPinnedAttachmentProvider {
    private val updateMutex = Mutex()

    override fun observe(groupId: String): Flow<GroupPin?> =
        dataSource
            .observe(groupId)
            .map { entity -> entity?.toDomain(groupMessageContentCodec) }

    override suspend fun getPinTarget(groupId: String, messageId: String): Result<GroupPinTarget> =
        safeSuspendCall { requirePinTarget(groupId, messageId) }

    private suspend fun requirePinTarget(groupId: String, messageId: String): GroupPinTarget {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(messageId.isNotBlank()) { "Message ID must not be blank" }
        val message = dataSource.findMessage(messageId) ?: error("Message was not found")
        check(message.conversationId == groupId) { "Message does not belong to this group" }
        check(message.transportMode == GROUP_END_TO_END_ENCRYPTED_MODE) {
            "Only group user messages can be pinned"
        }
        check(message.contentStatus == MessageContentStatus.READABLE.name) {
            "Only readable messages can be pinned"
        }
        return GroupPinTarget(
            isMine = message.isMine,
            senderContactId = message.senderContactId,
            isAlreadyPinned = dataSource.get(groupId)?.messageId == messageId
        )
    }

    override suspend fun pin(
        groupId: String,
        messageId: String,
        target: GroupPinTarget,
        senderSigningPublicKey: ByteArray
    ): Result<Unit> =
        safeSuspendCall {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            require(messageId.isNotBlank()) { "Message ID must not be blank" }

            updateMutex.withLock {
                broadcaster.requireLocalAdmin(groupId).getOrThrow()
                check(requirePinTarget(groupId, messageId) == target) {
                    "Pinned message sender changed while resolving the signing identity"
                }
                val message = dataSource.findMessage(messageId) ?: error("Message was not found")

                val currentState = dataSource.get(groupId)
                if (currentState?.messageId == messageId) return@withLock

                require(senderSigningPublicKey.isNotEmpty()) { "Pinned message sender identity was not found" }
                val senderKey = senderSigningPublicKey.copyOf()

                val attachments = attachmentDataSource.protocolAttachments(messageId)
                val content =
                    GroupMessageContent(
                        text = message.text,
                        attachments = attachments,
                        replyToMessageId = message.replyToMessageId
                    )
                val changedAt =
                    maxOf(
                        SystemClock.nowEpochMilliseconds(),
                        (currentState?.changedAtEpochMilliseconds ?: 0L) + 1L
                    )
                dataSource.save(
                    createGroupPinEntity(
                        groupId = groupId,
                        messageId = message.id,
                        messageSentAtEpochMilliseconds = message.createdAtEpochMilliseconds,
                        messageSenderSigningPublicKey = senderKey,
                        senderContactId = message.senderContactId,
                        isMine = message.isMine,
                        content = content,
                        pinnedAtEpochMilliseconds = changedAt,
                        changedAtEpochMilliseconds = changedAt,
                        groupMessageContentCodec = groupMessageContentCodec
                    )
                )
                broadcaster.broadcast(groupId).getOrThrow()
            }
        }

    override suspend fun load(groupId: String, attachmentId: String): ByteArray =
        loadAttachment(groupId, attachmentId).getOrThrow()

    override suspend fun loadAttachment(
        groupId: String,
        attachmentId: String
    ): Result<ByteArray> =
        safeSuspendCall {
            attachmentDataSource.loadDetachedBytes(
                findPinnedAttachment(groupId, attachmentId)
            )
        }

    private suspend fun findPinnedAttachment(
        groupId: String,
        attachmentId: String
    ): ProtocolMessageAttachment {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(attachmentId.isNotBlank()) { "Attachment ID must not be blank" }

        val state = dataSource.get(groupId) ?: error("Group pin was not found")
        val encodedContent = state.messageContent ?: error("Group pin has no message content")
        val content = groupMessageContentCodec.decode(encodedContent)
        return content.attachments.firstOrNull { item -> item.attachmentId == attachmentId }
            ?: error("Pinned message attachment was not found")
    }

    override suspend fun unpin(groupId: String): Result<Unit> =
        safeSuspendCall {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }

            updateMutex.withLock {
                broadcaster.requireLocalAdmin(groupId).getOrThrow()
                val currentState = dataSource.get(groupId)
                if (currentState?.messageId == null) return@withLock

                val changedAt =
                    maxOf(
                        SystemClock.nowEpochMilliseconds(),
                        currentState.changedAtEpochMilliseconds + 1L
                    )
                dataSource.save(
                    unpinnedGroupPinEntity(
                        groupId = groupId,
                        changedAtEpochMilliseconds = changedAt
                    )
                )
                broadcaster.broadcast(groupId).getOrThrow()
            }
        }
}
