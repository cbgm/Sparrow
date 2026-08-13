package com.cbgm.securechat.feature.contacts.data.verification

import com.cbgm.securechat.core.protocol.identity.LocalIdentityChangeHandler
import com.cbgm.securechat.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.securechat.core.protocol.mailbox.NoOpMailboxCapabilityLifecycle
import com.cbgm.securechat.data.database.identity.LocalIdentityDataResetter

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
