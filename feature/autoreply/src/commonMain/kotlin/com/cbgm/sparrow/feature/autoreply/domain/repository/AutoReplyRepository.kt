package com.cbgm.sparrow.feature.autoreply.domain.repository

import com.cbgm.sparrow.feature.autoreply.domain.model.AutoReply
import kotlinx.coroutines.flow.Flow

interface AutoReplyRepository {
    fun observeAll(): Flow<List<AutoReply>>

    fun observeActive(): Flow<AutoReply?>

    suspend fun create(autoReply: AutoReply): Result<Unit>

    suspend fun update(
        id: String,
        name: String,
        text: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Unit>

    suspend fun delete(id: String): Result<Unit>

    suspend fun activate(
        id: String,
        activationSessionId: String
    ): Result<Unit>

    suspend fun deactivate(): Result<Unit>

    suspend fun claimForContact(
        contactId: String,
        sentAtEpochMilliseconds: Long
    ): Result<AutoReply?>

    suspend fun releaseContactClaim(
        contactId: String,
        expectedActivationSessionId: String
    ): Result<Unit>
}
