package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.LocalMailboxCredentialEntity
import com.cbgm.sparrow.data.database.entity.RemoteMailboxRouteEntity
import com.cbgm.sparrow.data.database.entity.RetiringMailboxCredentialEntity

@Dao
interface MailboxRouteDao {
    @Query("SELECT * FROM local_mailbox_credentials WHERE contactId = :contactId LIMIT 1")
    suspend fun findLocal(contactId: String): LocalMailboxCredentialEntity?

    @Query("SELECT * FROM local_mailbox_credentials")
    suspend fun allLocal(): List<LocalMailboxCredentialEntity>

    @Query(
        """
        SELECT remote_mailbox_routes.*
        FROM remote_mailbox_routes
        INNER JOIN contact_routing_ids
            ON contact_routing_ids.contactId = remote_mailbox_routes.contactId
        WHERE contact_routing_ids.routingId = :routingId
        LIMIT 1
        """
    )
    suspend fun findRemoteByRoutingId(routingId: String): RemoteMailboxRouteEntity?

    @Query("SELECT * FROM remote_mailbox_routes WHERE contactId = :contactId LIMIT 1")
    suspend fun findRemote(contactId: String): RemoteMailboxRouteEntity?

    @Upsert
    suspend fun upsertLocal(entity: LocalMailboxCredentialEntity)

    @Upsert
    suspend fun upsertRemote(entity: RemoteMailboxRouteEntity)

    @Query("UPDATE local_mailbox_credentials SET revocationPending = 1 WHERE contactId = :contactId")
    suspend fun markLocalRevocationPending(contactId: String)

    @Query("DELETE FROM local_mailbox_credentials WHERE contactId = :contactId")
    suspend fun deleteLocal(contactId: String)

    @Query("DELETE FROM remote_mailbox_routes WHERE contactId = :contactId")
    suspend fun deleteRemote(contactId: String)

    @Query("DELETE FROM remote_mailbox_routes")
    suspend fun deleteAllRemote()

    @Query("SELECT * FROM retiring_mailbox_credentials")
    suspend fun allRetiring(): List<RetiringMailboxCredentialEntity>

    @Upsert
    suspend fun upsertRetiring(entity: RetiringMailboxCredentialEntity)

    /** The renewed credential replaces the current one only together with keeping the old one as retiring. */
    @Transaction
    suspend fun retireLocal(retiring: RetiringMailboxCredentialEntity, replacement: LocalMailboxCredentialEntity) {
        upsertRetiring(retiring)
        upsertLocal(replacement)
    }

    @Query("UPDATE retiring_mailbox_credentials SET revocationPending = 1 WHERE contactId = :contactId")
    suspend fun markRetiringRevocationPending(contactId: String)

    @Query("DELETE FROM retiring_mailbox_credentials WHERE routeId = :routeId")
    suspend fun deleteRetiring(routeId: String)
}
