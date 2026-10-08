package com.cbgm.sparrow.feature.identity.data

import com.cbgm.sparrow.data.database.identity.LocalIdentityDataResetter
import com.cbgm.sparrow.protocol.identity.LocalIdentityChangeHandler
import com.cbgm.sparrow.protocol.mailbox.MailboxCapabilityLifecycle

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
