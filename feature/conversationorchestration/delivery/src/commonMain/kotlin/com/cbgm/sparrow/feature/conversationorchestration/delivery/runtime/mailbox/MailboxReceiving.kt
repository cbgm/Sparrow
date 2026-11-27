package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.protocol.mailbox.MailboxRouteRepository

/** Every mailbox that still receives: the current ones and retiring ones a renewal replaced, unless being revoked. */
internal suspend fun MailboxRouteRepository.receiving(): Result<List<LocalMailboxCredential>> =
    safeSuspendCall(logFailure = false) {
        (allLocal().getOrThrow() + allRetiring().getOrThrow()).filterNot(LocalMailboxCredential::revocationPending)
    }
