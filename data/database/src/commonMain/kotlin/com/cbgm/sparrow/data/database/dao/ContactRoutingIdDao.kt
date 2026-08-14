package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.ContactRoutingIdEntity

@Dao
interface ContactRoutingIdDao {
    @Query("SELECT contactId FROM contact_routing_ids WHERE routingId = :routingId LIMIT 1")
    suspend fun findContactIdByRoutingId(routingId: String): String?

    @Query("SELECT routingId FROM contact_routing_ids WHERE contactId = :contactId LIMIT 1")
    suspend fun findRoutingIdByContactId(contactId: String): String?

    @Query(
        "DELETE FROM contact_routing_ids " +
            "WHERE routingId = :routingId AND contactId != :contactId"
    )
    suspend fun deleteOtherContactMapping(
        routingId: String,
        contactId: String
    )

    @Upsert
    suspend fun upsert(entity: ContactRoutingIdEntity)
}
