package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.IdentityInvitationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IdentityInvitationDao {
    @Upsert
    suspend fun upsert(invitation: IdentityInvitationEntity)

    @Query(
        """
        SELECT *
        FROM identity_invitations
        WHERE invitationId = :invitationId
        LIMIT 1
        """
    )
    suspend fun findById(invitationId: String): IdentityInvitationEntity?

    @Query(
        """
        UPDATE identity_invitations
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
        FROM identity_invitations
        WHERE contactId = :contactId
          AND state NOT IN (:terminalStates)
        ORDER BY createdAtEpochMilliseconds DESC, updatedAtEpochMilliseconds DESC
        LIMIT 1
        """
    )
    suspend fun findActiveForContact(
        contactId: String,
        terminalStates: List<String>
    ): IdentityInvitationEntity?

    @Query(
        """
        SELECT *
        FROM identity_invitations
        WHERE contactId = :contactId
        ORDER BY createdAtEpochMilliseconds DESC, updatedAtEpochMilliseconds DESC
        LIMIT 1
        """
    )
    suspend fun findLatestForContact(contactId: String): IdentityInvitationEntity?

    @Query(
        """
        SELECT *
        FROM identity_invitations
        WHERE contactId = :contactId
        ORDER BY createdAtEpochMilliseconds DESC, updatedAtEpochMilliseconds DESC
        LIMIT 1
        """
    )
    fun observeLatestForContact(contactId: String): Flow<IdentityInvitationEntity?>

    @Query(
        """
        SELECT invitation.*
        FROM identity_invitations AS invitation
        WHERE invitation.invitationId = (
            SELECT latest.invitationId
            FROM identity_invitations AS latest
            WHERE latest.contactId = invitation.contactId
            ORDER BY
                latest.createdAtEpochMilliseconds DESC,
                latest.updatedAtEpochMilliseconds DESC,
                latest.invitationId DESC
            LIMIT 1
        )
        """
    )
    fun observeLatestInvitations(): Flow<List<IdentityInvitationEntity>>

    @Query(
        """
        SELECT *
        FROM identity_invitations
        WHERE contactId = :contactId
          AND state IN (:states)
        ORDER BY updatedAtEpochMilliseconds DESC, createdAtEpochMilliseconds DESC
        LIMIT 1
        """
    )
    suspend fun findLatestForContactByStates(
        contactId: String,
        states: List<String>
    ): IdentityInvitationEntity?

    @Query(
        """
        SELECT *
        FROM identity_invitations
        WHERE contactId = :contactId
          AND state IN (:states)
        ORDER BY updatedAtEpochMilliseconds DESC, createdAtEpochMilliseconds DESC
        LIMIT 1
        """
    )
    fun observeLatestForContactByStates(
        contactId: String,
        states: List<String>
    ): Flow<IdentityInvitationEntity?>

    @Query(
        """
        SELECT *
        FROM identity_invitations
        WHERE direction = :direction
          AND state IN (:states)
        ORDER BY createdAtEpochMilliseconds
        """
    )
    fun observeByDirectionAndStates(
        direction: String,
        states: List<String>
    ): Flow<List<IdentityInvitationEntity>>

    @Query(
        """
        SELECT *
        FROM identity_invitations
        WHERE state IN (:states)
        ORDER BY updatedAtEpochMilliseconds DESC, createdAtEpochMilliseconds DESC
        """
    )
    fun observeByStates(states: List<String>): Flow<List<IdentityInvitationEntity>>

    @Query(
        """
        UPDATE identity_invitations
        SET viewedAtEpochMilliseconds = :viewedAtEpochMilliseconds
        WHERE direction = :direction
          AND hiddenAtEpochMilliseconds IS NULL
        """
    )
    suspend fun markDirectionViewed(
        direction: String,
        viewedAtEpochMilliseconds: Long
    )

    @Query(
        """
        UPDATE identity_invitations
        SET hiddenAtEpochMilliseconds = :hiddenAtEpochMilliseconds
        WHERE invitationId = :invitationId
          AND direction = :direction
          AND state = :state
        """
    )
    suspend fun hideByIdAndState(
        invitationId: String,
        direction: String,
        state: String,
        hiddenAtEpochMilliseconds: Long
    ): Int
}
