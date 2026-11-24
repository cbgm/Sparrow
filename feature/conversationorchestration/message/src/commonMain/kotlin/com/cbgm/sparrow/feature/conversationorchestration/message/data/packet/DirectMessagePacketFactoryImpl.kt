package com.cbgm.sparrow.feature.conversationorchestration.message.data.packet

import com.cbgm.sparrow.core.base.messagepart.data.mapper.toDto
import com.cbgm.sparrow.core.base.messagepart.data.model.TextDto
import com.cbgm.sparrow.core.base.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.core.util.phone.LocalPhoneNumberSource
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.packet.DirectMessagePacketFactory
import com.cbgm.sparrow.protocol.packet.ChatMessagePacket
import com.cbgm.sparrow.protocol.profile.LocalProfilePictureMetadataSource
import com.cbgm.sparrow.protocol.profile.ProfilePictureMetadata

internal class DirectMessagePacketFactoryImpl(
    private val localPhoneNumberSource: LocalPhoneNumberSource,
    private val localProfilePictureMetadataSource: LocalProfilePictureMetadataSource
) : DirectMessagePacketFactory {
    override suspend fun create(
        messageId: String,
        text: String,
        parts: List<MessagePart>,
        replyToMessageId: String?
    ): Result<ChatMessagePacket> =
        safeSuspendCall {
            ChatMessagePacket(
                packetId = IdGenerator.generate(prefix = "packet"),
                messageId = messageId,
                sentAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                parts =
                    buildList {
                        text.takeIf(String::isNotBlank)
                            ?.let { value -> add(TextDto(id = messageId, text = value)) }
                        addAll(parts.map { it.toDto() })
                    },
                replyToMessageId = replyToMessageId,
                senderPhoneNumber = localPhoneNumberSource.getLocalPhoneNumber().getOrThrow(),
                profilePicture = localProfilePictureMetadataSource.forMessage().getOrElse { ProfilePictureMetadata() }
            )
        }
}
