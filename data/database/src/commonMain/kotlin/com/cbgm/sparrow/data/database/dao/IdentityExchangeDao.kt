package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.IdentityExchangeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IdentityExchangeDao {
    @Upsert
    suspend fun upsert(exchange: IdentityExchangeEntity)

    @Query(
        """
        SELECT *
        FROM identity_exchanges
        WHERE exchangeId = :exchangeId
        LIMIT 1
        """
    )
    suspend fun findById(exchangeId: String): IdentityExchangeEntity?

    @Query(
        """
        UPDATE identity_exchanges
        SET contactId = :toContactId
        WHERE contactId = :fromContactId
        """
    )
    suspend fun reassignContact(
        fromContactId: String,
        toContactId: String
    )

    @Query(
        """
        SELECT *
        FROM identity_exchanges
        WHERE contactId = :contactId
          AND stage NOT IN (:terminalStages)
        ORDER BY createdAtEpochMilliseconds DESC, updatedAtEpochMilliseconds DESC
        LIMIT 1
        """
    )
    suspend fun findActiveForContact(
        contactId: String,
        terminalStages: List<String>
    ): IdentityExchangeEntity?

    @Query(
        """
        SELECT *
        FROM identity_exchanges
        WHERE contactId = :contactId
        ORDER BY createdAtEpochMilliseconds DESC, updatedAtEpochMilliseconds DESC
        LIMIT 1
        """
    )
    suspend fun findLatestForContact(contactId: String): IdentityExchangeEntity?

    @Query(
        """
        SELECT *
        FROM identity_exchanges
        WHERE contactId = :contactId
        ORDER BY createdAtEpochMilliseconds DESC, updatedAtEpochMilliseconds DESC
        LIMIT 1
        """
    )
    fun observeLatestForContact(contactId: String): Flow<IdentityExchangeEntity?>

    @Query(
        """
        SELECT *
        FROM identity_exchanges
        WHERE contactId = :contactId
          AND stage IN (:stages)
        ORDER BY updatedAtEpochMilliseconds DESC, createdAtEpochMilliseconds DESC
        LIMIT 1
        """
    )
    suspend fun findLatestForContactByStages(
        contactId: String,
        stages: List<String>
    ): IdentityExchangeEntity?

    @Query(
        """
        SELECT *
        FROM identity_exchanges
        WHERE contactId = :contactId
          AND stage IN (:stages)
        ORDER BY updatedAtEpochMilliseconds DESC, createdAtEpochMilliseconds DESC
        LIMIT 1
        """
    )
    fun observeLatestForContactByStages(
        contactId: String,
        stages: List<String>
    ): Flow<IdentityExchangeEntity?>
}
