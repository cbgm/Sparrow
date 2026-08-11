package com.cbgm.securechat.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cbgm.securechat.data.database.entity.ContactRelayIdEntity

@Dao
interface ContactRelayIdDao {
    @Query("SELECT contactId FROM contact_relay_ids WHERE relayId = :relayId LIMIT 1")
    suspend fun findContactIdByRelayId(relayId: String): String?

    @Query("SELECT relayId FROM contact_relay_ids WHERE contactId = :contactId LIMIT 1")
    suspend fun findRelayIdByContactId(contactId: String): String?

    @Query(
        "DELETE FROM contact_relay_ids " +
            "WHERE relayId = :relayId AND contactId != :contactId"
    )
    suspend fun deleteOtherContactMapping(
        relayId: String,
        contactId: String
    )

    @Upsert
    suspend fun upsert(entity: ContactRelayIdEntity)
}
