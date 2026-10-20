package com.cbgm.sparrow.feature.contacts.domain.repository

import kotlinx.coroutines.flow.Flow

interface ContactBlocklistRepository {
    fun observeBlockUnknownContactInvites(): Flow<Boolean>

    suspend fun getBlockUnknownContactInvites(): Result<Boolean>

    suspend fun setBlockUnknownContactInvites(enabled: Boolean): Result<Unit>

    fun observeBlockedContactIds(): Flow<Set<String>>

    suspend fun getBlockedContactIds(): Result<Set<String>>

    suspend fun isBlocked(contactId: String): Result<Boolean>

    suspend fun block(contactId: String): Result<Unit>

    suspend fun unblock(contactId: String): Result<Unit>
}
