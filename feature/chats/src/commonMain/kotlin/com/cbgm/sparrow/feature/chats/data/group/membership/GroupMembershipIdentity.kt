package com.cbgm.sparrow.feature.chats.data.group.membership

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactUseCase

internal class GroupMembershipIdentity(
    private val getContact: GetContactUseCase
) {
    suspend fun requireContact(contactId: String): Contact =
        getContact(contactId).getOrThrow()
            ?: error("Contact was not found: $contactId")

    suspend fun ensureSigningIdentityMatches(
        contactId: String,
        signingPublicKey: ByteArray
    ) {
        val existing = requireContact(contactId).sparrowIdentity ?: return
        check(existing.signingPublicKey.contentEquals(signingPublicKey)) {
            "Contact signing identity conflicts with the invitation response"
        }
    }
}
