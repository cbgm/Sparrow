package com.cbgm.securechat.feature.messaging.application.mailbox

import com.cbgm.securechat.core.protocol.mailbox.MailboxDeliveryRoute

class MailboxRoutePayloadEncoder {
    fun encode(route: MailboxDeliveryRoute): ByteArray =
        listOf(
            route.routeId,
            route.nodeId,
            route.nodeEndpoint,
            route.mailboxId,
            route.sendCapability,
            route.sequence.toString(),
            route.expiresAtEpochMilliseconds.toString()
        ).joinToString(separator = "\u001f").encodeToByteArray()
}
