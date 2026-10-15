package com.cbgm.sparrow.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_metadata",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GroupMetadataEntity(
    @PrimaryKey
    val groupId: String,
    @ColumnInfo(defaultValue = "")
    val title: String = "",
    @ColumnInfo(defaultValue = "0")
    val titleChangedAtEpochMilliseconds: Long = 0L,
    @ColumnInfo(defaultValue = "NULL")
    val description: String? = null,
    @ColumnInfo(defaultValue = "0")
    val descriptionChangedAtEpochMilliseconds: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    val hasAvatar: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val avatarChangedAtEpochMilliseconds: Long = 0L
)
