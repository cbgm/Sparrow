package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.model.MailboxContactFacts
import com.cbgm.sparrow.feature.contacts.domain.model.MailboxContactState
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactTransportRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityPeerStateUseCase

/**
 * Decides which contacts get a mailbox route. Besides a mutual identity and a routing ID, the contact must still be
 * someone we talk to: a direct chat with an established exchange, or an active group we share (group packets also
 * arrive through the mailbox). A contact whose chat was deleted is therefore never sent a fresh route.
 */
class GetMailboxContactStatesUseCase(
    private val repository: ContactTransportRepository,
    private val getIdentityPeerState: GetIdentityPeerStateUseCase
) {
    suspend operator fun invoke(): Result<List<MailboxContactState>> =
        safeSuspendCall(logFailure = false) {
            repository.getMailboxContacts().getOrThrow().map { contact ->
                MailboxContactState(contact.contactId, isProvisioningEligible(contact))
            }
        }

    private suspend fun isProvisioningEligible(contact: MailboxContactFacts): Boolean {
        if (!contact.hasMutualIdentity || !contact.hasRoutingId) return false
        if (contact.sharesActiveGroup) return true
        return contact.hasDirectConversation &&
            getIdentityPeerState(contact.contactId).getOrThrow().hasEstablishedExchange
    }
}
