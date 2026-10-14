package com.cbgm.sparrow.feature.contacts.domain.repository

import com.cbgm.sparrow.feature.contacts.domain.model.identity.IdentityPeerMerge

/** Contacts-owned peer metadata; no keys or trust state are read by this repository. */
interface IdentityPeerRepository {
    suspend fun getDisplayName(peerId: String): Result<String?>

    suspend fun containsPeer(peerId: String): Result<Boolean>

    suspend fun isKnownContact(peerId: String): Result<Boolean>

    suspend fun findEquivalentPhonePeerId(phoneNumber: String): Result<String?>

    suspend fun canMergeRoutingDuplicate(
        peerId: String,
        remotePhoneNumber: String?
    ): Result<Boolean>

    suspend fun applyMerge(merge: IdentityPeerMerge): Result<Unit>

    suspend fun updateIncomingMetadata(
        peerId: String,
        phoneNumber: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Unit>
}
