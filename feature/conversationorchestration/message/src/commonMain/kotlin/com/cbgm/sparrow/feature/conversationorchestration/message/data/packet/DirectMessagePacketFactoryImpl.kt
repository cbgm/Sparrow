package com.cbgm.sparrow.feature.conversationorchestration.message.data.packet

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.messagepart.data.mapper.toDto
import com.cbgm.sparrow.core.messagepart.data.model.TextDto
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.packet.DirectMessagePacketFactory
import com.cbgm.sparrow.protocol.packet.ChatMessagePacket
import com.cbgm.sparrow.protocol.profile.LocalProfilePictureMetadataProvider
import com.cbgm.sparrow.protocol.profile.ProfilePictureMetadata

internal class DirectMessagePacketFactoryImpl(
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val localProfilePictureMetadataProvider: LocalProfilePictureMetadataProvider
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
                senderPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow(),
                profilePicture = localProfilePictureMetadataProvider.forMessage().getOrElse { ProfilePictureMetadata() }
            )
        }
}
