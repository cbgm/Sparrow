package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.ContactProfilePictureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactProfilePictureDao {
    @Query("SELECT * FROM contact_profile_pictures WHERE contactId = :contactId LIMIT 1")
    fun observe(contactId: String): Flow<ContactProfilePictureEntity?>

    @Query("SELECT * FROM contact_profile_pictures WHERE contactId = :contactId LIMIT 1")
    suspend fun get(contactId: String): ContactProfilePictureEntity?

    @Upsert
    suspend fun upsert(entity: ContactProfilePictureEntity)
}
