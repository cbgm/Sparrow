package com.cbgm.sparrow.feature.settings.presentation.errors.model

data class DeveloperErrorUi(
    val id: String,
    val timestamp: String,
    val tag: String,
    val message: String,
    val exceptionType: String? = null,
    val stackTrace: String? = null
)
