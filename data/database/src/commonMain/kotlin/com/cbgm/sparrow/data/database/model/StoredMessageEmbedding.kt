package com.cbgm.sparrow.data.database.model

data class StoredMessageEmbedding(
    val messageId: String,
    val conversationId: String,
    val text: String,
    val createdAtEpochMilliseconds: Long,
    val embedding: ByteArray
)
