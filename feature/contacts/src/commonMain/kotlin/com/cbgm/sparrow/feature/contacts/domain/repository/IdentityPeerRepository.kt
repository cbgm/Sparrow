package com.cbgm.sparrow.feature.contacts.domain.repository

import com.cbgm.sparrow.feature.contacts.domain.model.identity.IdentityPeerMerge

/** Contacts-owned peer metadata; no keys or trust state are read by this repository. */
interface IdentityPeerRepository {
    suspend fun getDisplayName(peerId: String): String?

    suspend fun containsPeer(peerId: String): Boolean

    suspend fun isKnownContact(peerId: String): Boolean

    suspend fun findEquivalentPhonePeerId(phoneNumber: String): String?

    suspend fun canMergeRoutingDuplicate(peerId: String, remotePhoneNumber: String?): Boolean

    suspend fun applyMerge(merge: IdentityPeerMerge)

    suspend fun updateIncomingMetadata(peerId: String, phoneNumber: String, updatedAtEpochMilliseconds: Long)
}
