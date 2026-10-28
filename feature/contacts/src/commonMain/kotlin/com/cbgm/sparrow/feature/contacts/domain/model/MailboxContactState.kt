package com.cbgm.sparrow.feature.contacts.domain.model

/** Contacts-owned projection used when provisioning transport mailboxes. */
data class MailboxContactState(
    val contactId: String,
    val isProvisioningEligible: Boolean
)

/** What Contacts stores about a contact that decides whether it gets a mailbox route. */
data class MailboxContactFacts(
    val contactId: String,
    val hasMutualIdentity: Boolean,
    val hasRoutingId: Boolean,
    val hasDirectConversation: Boolean,
    val sharesActiveGroup: Boolean
)
