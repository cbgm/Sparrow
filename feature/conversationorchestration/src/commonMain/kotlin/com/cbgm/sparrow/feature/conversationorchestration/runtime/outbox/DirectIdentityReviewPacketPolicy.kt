package com.cbgm.sparrow.feature.conversationorchestration.runtime.outbox

import com.cbgm.sparrow.core.protocol.packet.ChatMessagePacket
import com.cbgm.sparrow.core.protocol.packet.MessageDeletionPacket
import com.cbgm.sparrow.core.protocol.packet.MessageEditPacket
import com.cbgm.sparrow.core.protocol.packet.ReadReceiptPacket
import kotlin.reflect.KClass

/** Restrict only ordinary direct-chat traffic; signed recovery invitations must still pass. */
internal fun blocksDirectPacketDuringIdentityReview(type: KClass<*>): Boolean =
    type == ChatMessagePacket::class || type == ReadReceiptPacket::class ||
        type == MessageEditPacket::class || type == MessageDeletionPacket::class
