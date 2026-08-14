package com.cbgm.securechat.core.protocol.packet

import com.cbgm.securechat.core.protocol.mailbox.MailboxDeliveryRoute
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("mailbox_route")
data class MailboxRoutePacket(
    override val packetId: String,
    override val version: Int = 1,
    val deliveryRoute: MailboxDeliveryRoute
) : SecureChatPacket
