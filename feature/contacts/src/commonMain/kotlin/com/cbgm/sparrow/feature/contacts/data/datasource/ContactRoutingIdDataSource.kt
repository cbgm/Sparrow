package com.cbgm.sparrow.feature.contacts.data.datasource

import com.cbgm.sparrow.data.database.dao.ContactRoutingIdDao
import com.cbgm.sparrow.data.database.entity.ContactRoutingIdEntity

class ContactRoutingIdDataSource(
    private val contactRoutingIdDao: ContactRoutingIdDao
) {
    suspend fun findRoutingIdByContactId(contactId: String): String? =
        contactRoutingIdDao.findRoutingIdByContactId(contactId)

    suspend fun findContactIdByRoutingId(routingId: String): String? =
        contactRoutingIdDao.findContactIdByRoutingId(routingId)

    suspend fun deleteOtherContactMapping(routingId: String, contactId: String) =
        contactRoutingIdDao.deleteOtherContactMapping(routingId = routingId, contactId = contactId)

    suspend fun upsert(entity: ContactRoutingIdEntity) =
        contactRoutingIdDao.upsert(entity)
}
