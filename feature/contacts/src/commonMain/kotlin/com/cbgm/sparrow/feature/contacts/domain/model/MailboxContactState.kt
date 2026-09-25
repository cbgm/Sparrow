package com.cbgm.sparrow.feature.contacts.domain.model

/** Contacts-owned projection used when provisioning transport mailboxes. */
data class MailboxContactState(
    val contactId: String,
    val isProvisioningEligible: Boolean
)
