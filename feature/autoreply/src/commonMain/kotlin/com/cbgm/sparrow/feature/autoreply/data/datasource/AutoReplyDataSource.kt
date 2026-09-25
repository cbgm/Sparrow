package com.cbgm.sparrow.feature.autoreply.data.datasource

import com.cbgm.sparrow.data.database.dao.AutoReplyDao
import com.cbgm.sparrow.data.database.entity.AutoReplyEntity
import kotlinx.coroutines.flow.Flow

class AutoReplyDataSource(
    private val dao: AutoReplyDao
) {
    fun observeAll(): Flow<List<AutoReplyEntity>> = dao.observeAll()

    fun observeActive(): Flow<AutoReplyEntity?> = dao.observeActive()

    suspend fun upsert(entity: AutoReplyEntity) {
        dao.upsert(entity)
    }

    suspend fun update(
        id: String,
        name: String,
        text: String,
        updatedAtEpochMilliseconds: Long
    ) {
        check(
            dao.updateContent(
                id = id,
                name = name,
                text = text,
                updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
            ) == 1
        ) { "Auto reply not found: $id" }
    }

    suspend fun delete(id: String) {
        dao.delete(id)
    }

    suspend fun activate(
        id: String,
        activationSessionId: String
    ) {
        dao.activate(id, activationSessionId)
    }

    suspend fun deactivate() {
        dao.deactivate()
    }

    suspend fun claimForContact(
        contactId: String,
        sentAtEpochMilliseconds: Long
    ): AutoReplyEntity? =
        dao.claimForContact(contactId, sentAtEpochMilliseconds)

    suspend fun releaseContactClaim(
        contactId: String,
        expectedActivationSessionId: String
    ) {
        dao.releaseContactClaim(contactId, expectedActivationSessionId)
    }
}
