package com.cbgm.sparrow.feature.settings.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DeveloperErrorDto(
    val id: String,
    val timestampEpochMilliseconds: Long,
    val tag: String,
    val message: String,
    val exceptionType: String? = null,
    val stackTrace: String? = null
)
