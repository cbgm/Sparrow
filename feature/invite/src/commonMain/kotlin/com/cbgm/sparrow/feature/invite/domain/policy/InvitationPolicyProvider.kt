package com.cbgm.sparrow.feature.invite.domain.policy

import kotlinx.coroutines.flow.Flow

interface InvitationPolicyProvider {
    fun observeEnabled(): Flow<Boolean>

    fun observeBlockedPeerIds(): Flow<Set<String>>

    suspend fun isEnabled(): Boolean

    suspend fun getBlockedPeerIds(): Set<String>

    suspend fun blockUnknownPeers(): Boolean

    suspend fun isPeerBlocked(peerId: String): Boolean
}
