package com.cbgm.sparrow.feature.transport.domain.repository

import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential

/** This device's invitation mailbox: where anyone without one of its mailbox routes puts packets. */
interface InvitationMailboxRepository {
    /** The mailbox the alias claims name, opened or renewed first when needed. */
    suspend fun current(): Result<LocalMailboxCredential>

    /** Every invitation mailbox that may still hold envelopes. */
    suspend fun receiving(): Result<List<LocalMailboxCredential>>

    companion object {
        /** Names the invitation mailboxes where a mailbox credential names its contact. */
        const val CONTACT_ID = "invitation-mailbox"
    }
}
