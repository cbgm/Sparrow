package com.cbgm.sparrow.feature.settings.domain.model

data class DeveloperError(
    val id: String,
    val timestampEpochMilliseconds: Long,
    val tag: String,
    val message: String,
    val exceptionType: String? = null,
    val stackTrace: String? = null
)
