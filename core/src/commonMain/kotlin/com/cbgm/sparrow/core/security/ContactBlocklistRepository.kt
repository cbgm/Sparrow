package com.cbgm.sparrow.core.security

import kotlinx.coroutines.flow.Flow

interface ContactBlocklistRepository {
    fun observeBlockUnknownContactInvites(): Flow<Boolean>

    suspend fun getBlockUnknownContactInvites(): Boolean

    suspend fun setBlockUnknownContactInvites(enabled: Boolean)

    fun observeBlockedContactIds(): Flow<Set<String>>

    suspend fun getBlockedContactIds(): Set<String>

    suspend fun isBlocked(contactId: String): Boolean

    suspend fun block(contactId: String)

    suspend fun unblock(contactId: String)
}
