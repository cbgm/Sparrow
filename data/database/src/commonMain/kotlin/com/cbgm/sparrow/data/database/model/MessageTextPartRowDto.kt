package com.cbgm.sparrow.data.database.model

data class MessageTextPartRowDto(
    val messageId: String,
    val partId: String,
    val position: Int,
    val text: String
)
