package com.cbgm.securechat.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cbgm.securechat.data.database.entity.ContactRoutingIdEntity

@Dao
interface ContactRoutingIdDao {
    @Query("SELECT contactId FROM contact_relay_ids WHERE relayId = :routingId LIMIT 1")
    suspend fun findContactIdByRoutingId(routingId: String): String?

    @Query("SELECT relayId FROM contact_relay_ids WHERE contactId = :contactId LIMIT 1")
    suspend fun findRoutingIdByContactId(contactId: String): String?

    @Query(
        "DELETE FROM contact_relay_ids " +
            "WHERE relayId = :routingId AND contactId != :contactId"
    )
    suspend fun deleteOtherContactMapping(
        routingId: String,
        contactId: String
    )

    @Upsert
    suspend fun upsert(entity: ContactRoutingIdEntity)
}
