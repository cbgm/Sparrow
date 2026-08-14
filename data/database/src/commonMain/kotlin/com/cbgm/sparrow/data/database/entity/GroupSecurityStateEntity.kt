package com.cbgm.sparrow.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_security_states",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GroupSecurityStateEntity(
    @PrimaryKey
    val groupId: String,
    val currentEpoch: Int,
    val welcomePacketId: String?,
    val ownerContactId: String?,
    val ownerSigningPublicKey: ByteArray,
    val localSigningPublicKey: ByteArray,
    @ColumnInfo(defaultValue = "'MEMBER'")
    val localRole: String,
    val updatedAtEpochMilliseconds: Long
) {
    init {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(currentEpoch > 0) { "Group epoch must be positive" }
        require(ownerSigningPublicKey.isNotEmpty()) { "Owner signing public key must not be empty" }
        require(localSigningPublicKey.isNotEmpty()) { "Local signing public key must not be empty" }
        require(localRole.isNotBlank()) { "Local group role must not be blank" }
        require(updatedAtEpochMilliseconds >= 0L) { "Security-state timestamp must not be negative" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupSecurityStateEntity) return false

        return groupId == other.groupId &&
            currentEpoch == other.currentEpoch &&
            welcomePacketId == other.welcomePacketId &&
            ownerContactId == other.ownerContactId &&
            ownerSigningPublicKey.contentEquals(other.ownerSigningPublicKey) &&
            localSigningPublicKey.contentEquals(other.localSigningPublicKey) &&
            localRole == other.localRole &&
            updatedAtEpochMilliseconds == other.updatedAtEpochMilliseconds
    }

    override fun hashCode(): Int {
        var result = groupId.hashCode()
        result = 31 * result + currentEpoch
        result = 31 * result + (welcomePacketId?.hashCode() ?: 0)
        result = 31 * result + (ownerContactId?.hashCode() ?: 0)
        result = 31 * result + ownerSigningPublicKey.contentHashCode()
        result = 31 * result + localSigningPublicKey.contentHashCode()
        result = 31 * result + localRole.hashCode()
        result = 31 * result + updatedAtEpochMilliseconds.hashCode()
        return result
    }
}
