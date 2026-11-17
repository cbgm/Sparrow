package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.model.MailboxContactFacts
import com.cbgm.sparrow.feature.contacts.domain.model.MailboxContactState
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactTransportRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityPeerStateUseCase

/**
 * Decides which contacts get a mailbox route. Besides a routing ID, the contact must still be someone we talk to: a
 * direct chat with an established exchange (right after the handshake, before the identity is verified, so the
 * messages after the handshake already use mailboxes), or an active group we share with a mutual identity (group
 * packets also arrive through the mailbox). A contact whose chat was deleted is therefore never sent a fresh route.
 */
class GetMailboxContactStatesUseCase(
    private val repository: ContactTransportRepository,
    private val getIdentityPeerState: GetIdentityPeerStateUseCase
) {
    suspend operator fun invoke(): Result<List<MailboxContactState>> =
        safeSuspendCall(logFailure = false) {
            repository.getMailboxContacts().getOrThrow().map { contact ->
                val reason = ineligibleReason(contact)
                MailboxContactState(contact.contactId, isProvisioningEligible = reason == null, ineligibleReason = reason)
            }
        }

    private suspend fun ineligibleReason(contact: MailboxContactFacts): String? {
        if (!contact.hasRoutingId) return "no routing ID"
        if (contact.sharesActiveGroup && contact.hasMutualIdentity) return null
        if (!contact.hasDirectConversation) {
            return if (contact.sharesActiveGroup) "only a shared group, and the identity is not mutual" else "no direct chat and no shared group"
        }
        val peer = getIdentityPeerState(contact.contactId).getOrThrow()
        return if (peer.hasEstablishedExchange) {
            null
        } else {
            "the handshake is not established (pending=${peer.hasPendingExchange}, mutual=${peer.hasMutualIdentity})"
        }
    }
}
