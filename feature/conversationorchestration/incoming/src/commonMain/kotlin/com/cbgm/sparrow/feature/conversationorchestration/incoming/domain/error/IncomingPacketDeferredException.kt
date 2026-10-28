package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.error

import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.DeferredPacketWait

/**
 * The packet arrived before what it needs, for example a group message before the group's welcome. Unlike a
 * rejection it is valid: its envelope is parked and processed again once [wait] is met. Parking instead of
 * failing matters because a live envelope that is not acknowledged is lost and a mailbox envelope would be
 * redelivered until it expires.
 */
class IncomingPacketDeferredException(
    val wait: DeferredPacketWait
) : Exception("Incoming packet waits for ${wait.key}")
