package com.cbgm.sparrow.feature.identity.data.direct.datasource

import com.cbgm.sparrow.data.database.dao.ContactRoutingIdDao
import com.cbgm.sparrow.data.database.entity.ContactRoutingIdEntity

internal class DirectContactRoutingDataSource(
    private val contactRoutingIdDao: ContactRoutingIdDao
) {
    suspend fun findRoutingIdByContactId(contactId: String): String? =
        contactRoutingIdDao.findRoutingIdByContactId(contactId)

    suspend fun deleteOtherContactMapping(routingId: String, contactId: String) =
        contactRoutingIdDao.deleteOtherContactMapping(routingId = routingId, contactId = contactId)

    suspend fun upsert(entity: ContactRoutingIdEntity) = contactRoutingIdDao.upsert(entity)
}
