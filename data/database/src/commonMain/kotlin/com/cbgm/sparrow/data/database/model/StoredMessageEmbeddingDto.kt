package com.cbgm.sparrow.data.database.model

data class StoredMessageEmbeddingDto(
    val messageId: String,
    val conversationId: String,
    val conversationType: String,
    val contactId: String?,
    val senderName: String?,
    val conversationTitle: String?,
    val contactName: String?,
    val text: String,
    val createdAtEpochMilliseconds: Long,
    val embedding: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StoredMessageEmbeddingDto

        if (createdAtEpochMilliseconds != other.createdAtEpochMilliseconds) return false
        if (messageId != other.messageId) return false
        if (conversationId != other.conversationId) return false
        if (conversationType != other.conversationType) return false
        if (contactId != other.contactId) return false
        if (senderName != other.senderName) return false
        if (conversationTitle != other.conversationTitle) return false
        if (contactName != other.contactName) return false
        if (text != other.text) return false
        if (!embedding.contentEquals(other.embedding)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = createdAtEpochMilliseconds.hashCode()
        result = 31 * result + messageId.hashCode()
        result = 31 * result + conversationId.hashCode()
        result = 31 * result + conversationType.hashCode()
        result = 31 * result + (contactId?.hashCode() ?: 0)
        result = 31 * result + (senderName?.hashCode() ?: 0)
        result = 31 * result + (conversationTitle?.hashCode() ?: 0)
        result = 31 * result + (contactName?.hashCode() ?: 0)
        result = 31 * result + text.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}
