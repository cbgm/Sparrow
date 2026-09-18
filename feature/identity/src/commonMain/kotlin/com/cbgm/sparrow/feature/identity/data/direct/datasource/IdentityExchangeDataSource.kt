package com.cbgm.sparrow.feature.identity.data.direct.datasource

import com.cbgm.sparrow.data.database.dao.IdentityExchangeDao
import com.cbgm.sparrow.data.database.entity.IdentityExchangeEntity
import kotlinx.coroutines.flow.Flow

class IdentityExchangeDataSource(
    private val identityExchangeDao: IdentityExchangeDao
) {
    suspend fun upsert(exchange: IdentityExchangeEntity) =
        identityExchangeDao.upsert(exchange)

    suspend fun findById(exchangeId: String): IdentityExchangeEntity? =
        identityExchangeDao.findById(exchangeId)

    suspend fun reassignContact(fromContactId: String, toContactId: String) =
        identityExchangeDao.reassignContact(fromContactId = fromContactId, toContactId = toContactId)

    suspend fun findActiveForContact(
        contactId: String,
        terminalStages: List<String>
    ): IdentityExchangeEntity? =
        identityExchangeDao.findActiveForContact(contactId = contactId, terminalStages = terminalStages)

    suspend fun findLatestForContact(contactId: String): IdentityExchangeEntity? =
        identityExchangeDao.findLatestForContact(contactId = contactId)

    fun observeLatestForContact(contactId: String): Flow<IdentityExchangeEntity?> =
        identityExchangeDao.observeLatestForContact(contactId = contactId)

    fun observeAll(): Flow<List<IdentityExchangeEntity>> =
        identityExchangeDao.observeAll()

    suspend fun findLatestForContactByStages(
        contactId: String,
        stages: List<String>
    ): IdentityExchangeEntity? =
        identityExchangeDao.findLatestForContactByStages(contactId = contactId, stages = stages)

    fun observeLatestForContactByStages(
        contactId: String,
        stages: List<String>
    ): Flow<IdentityExchangeEntity?> =
        identityExchangeDao.observeLatestForContactByStages(contactId = contactId, stages = stages)
}
