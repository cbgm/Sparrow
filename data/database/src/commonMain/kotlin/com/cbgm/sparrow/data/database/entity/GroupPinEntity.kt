package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_pins",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GroupPinEntity(
    @PrimaryKey
    val groupId: String,
    val messageId: String?,
    val messageSentAtEpochMilliseconds: Long?,
    val messageSenderSigningPublicKey: ByteArray?,
    val senderContactId: String?,
    val isMine: Boolean?,
    val messageContent: String?,
    val pinnedAtEpochMilliseconds: Long?,
    val changedAtEpochMilliseconds: Long
) {
    init {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(changedAtEpochMilliseconds > 0L) { "Group-pin timestamp must be positive" }
        if (messageId == null) {
            require(messageSentAtEpochMilliseconds == null)
            require(messageSenderSigningPublicKey == null)
            require(senderContactId == null)
            require(isMine == null)
            require(messageContent == null)
            require(pinnedAtEpochMilliseconds == null)
        } else {
            require(messageId.isNotBlank()) { "Pinned message ID must not be blank" }
            require(requireNotNull(messageSentAtEpochMilliseconds) >= 0L)
            require(!requireNotNull(messageSenderSigningPublicKey).isEmpty())
            requireNotNull(isMine)
            require(!requireNotNull(messageContent).isBlank())
            require(requireNotNull(pinnedAtEpochMilliseconds) > 0L)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupPinEntity) return false
        return groupId == other.groupId &&
            messageId == other.messageId &&
            messageSentAtEpochMilliseconds == other.messageSentAtEpochMilliseconds &&
            messageSenderSigningPublicKey.contentEquals(other.messageSenderSigningPublicKey) &&
            senderContactId == other.senderContactId &&
            isMine == other.isMine &&
            messageContent == other.messageContent &&
            pinnedAtEpochMilliseconds == other.pinnedAtEpochMilliseconds &&
            changedAtEpochMilliseconds == other.changedAtEpochMilliseconds
    }

    override fun hashCode(): Int {
        var result = groupId.hashCode()
        result = 31 * result + (messageId?.hashCode() ?: 0)
        result = 31 * result + (messageSentAtEpochMilliseconds?.hashCode() ?: 0)
        result = 31 * result + (messageSenderSigningPublicKey?.contentHashCode() ?: 0)
        result = 31 * result + (senderContactId?.hashCode() ?: 0)
        result = 31 * result + (isMine?.hashCode() ?: 0)
        result = 31 * result + (messageContent?.hashCode() ?: 0)
        result = 31 * result + (pinnedAtEpochMilliseconds?.hashCode() ?: 0)
        result = 31 * result + changedAtEpochMilliseconds.hashCode()
        return result
    }
}
