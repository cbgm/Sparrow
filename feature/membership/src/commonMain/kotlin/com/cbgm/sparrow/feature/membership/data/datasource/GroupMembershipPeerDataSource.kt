package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPeerDto

interface GroupMembershipPeerDataSource {
    suspend fun findPeer(contactId: String): GroupMembershipPeerDto?

    suspend fun requirePeer(contactId: String): GroupMembershipPeerDto =
        findPeer(contactId) ?: error("Membership peer was not found: $contactId")

    suspend fun ensureSigningIdentityMatches(
        contactId: String,
        signingPublicKey: ByteArray
    ) {
        val existingSigningKey = requirePeer(contactId).signingPublicKey ?: return
        check(existingSigningKey.contentEquals(signingPublicKey)) {
            "Contact signing identity conflicts with the membership handshake"
        }
    }
}
