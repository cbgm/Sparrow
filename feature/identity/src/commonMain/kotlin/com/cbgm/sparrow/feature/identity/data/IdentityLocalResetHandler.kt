package com.cbgm.sparrow.feature.identity.data

import com.cbgm.sparrow.core.protocol.identity.LocalIdentityChangeHandler
import com.cbgm.sparrow.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.data.database.identity.LocalIdentityDataResetter

/** The identity owner coordinates capability revocation before resetting persisted state. */
internal class IdentityLocalResetHandler(
    private val mailboxCapabilityLifecycle: MailboxCapabilityLifecycle,
    private val localIdentityDataResetter: LocalIdentityDataResetter
) : LocalIdentityChangeHandler {
    override suspend fun onLocalIdentityChanged(): Result<Unit> = runCatching {
        mailboxCapabilityLifecycle.revokeAll().getOrThrow()
        localIdentityDataResetter.reset()
    }
}
