package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.LinkPreviewEntity

@Dao
interface LinkPreviewDao {
    @Query("SELECT * FROM link_previews WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): LinkPreviewEntity?

    @Upsert
    suspend fun upsert(linkPreview: LinkPreviewEntity)
}
