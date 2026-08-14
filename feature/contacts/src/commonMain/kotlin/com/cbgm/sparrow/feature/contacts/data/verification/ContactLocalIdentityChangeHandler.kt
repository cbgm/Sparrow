package com.cbgm.sparrow.feature.contacts.data.verification

import com.cbgm.sparrow.core.protocol.identity.LocalIdentityChangeHandler
import com.cbgm.sparrow.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.core.protocol.mailbox.NoOpMailboxCapabilityLifecycle
import com.cbgm.sparrow.data.database.identity.LocalIdentityDataResetter

class ContactLocalIdentityChangeHandler(
    private val localIdentityDataResetter: LocalIdentityDataResetter,
    private val mailboxCapabilityLifecycle: MailboxCapabilityLifecycle =
        NoOpMailboxCapabilityLifecycle
) : LocalIdentityChangeHandler {
    override suspend fun onLocalIdentityChanged(): Result<Unit> =
        runCatching {
            mailboxCapabilityLifecycle.revokeAll()
            localIdentityDataResetter.reset()
        }
}
