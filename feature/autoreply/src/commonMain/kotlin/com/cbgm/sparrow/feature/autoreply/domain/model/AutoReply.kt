package com.cbgm.sparrow.feature.autoreply.domain.model

data class AutoReply(
    val id: String,
    val name: String,
    val text: String,
    val isActive: Boolean,
    val activationSessionId: String?,
    val createdAtEpochMilliseconds: Long,
    val updatedAtEpochMilliseconds: Long
)
