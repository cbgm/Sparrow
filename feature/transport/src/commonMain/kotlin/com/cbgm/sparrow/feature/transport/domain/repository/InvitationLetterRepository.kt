package com.cbgm.sparrow.feature.transport.domain.repository

import com.cbgm.sparrow.feature.transport.domain.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.domain.model.InvalidInvitationLetterException
import com.cbgm.sparrow.feature.transport.domain.model.InvitationLetterContent

/**
 * Packets for a device whose mailbox route the sender does not have yet: a first invitation to a phone alias, or the
 * answers to a contact before its route arrived. The sender looks up the device's invitation mailbox at its Control
 * Planes (the invitation routes) and puts a letter there through its circuit: the transport payload with the
 * sender's routing ID, signed with the sender's device key and sealed for the recipient's key, so the mailbox's node
 * reads neither the packet nor who sent it.
 *
 * The recipient trusts the sender's routing ID only when the signing key proves it: a device routing ID derives from
 * the key, and a phone alias must belong to that device at the recipient's Control Planes. Nobody can claim another
 * person's phone number this way.
 */
interface InvitationLetterRepository {
    /** Puts [payload] into the invitation mailbox of [recipientRoutingId]; returns when the envelope expires there. */
    suspend fun send(
        senderRoutingId: String,
        recipientRoutingId: String,
        payload: String,
        envelopeId: String,
        createdAtEpochMilliseconds: Long
    ): Result<Long>

    /**
     * The sender and transport payload of an envelope from this device's invitation mailbox. Fails with
     * [InvalidInvitationLetterException] when the letter can never be accepted (not for this device, a bad signature,
     * a routing ID the key does not prove), and with other failures when it may succeed later.
     */
    suspend fun open(envelope: FederatedEnvelope): Result<InvitationLetterContent>
}
