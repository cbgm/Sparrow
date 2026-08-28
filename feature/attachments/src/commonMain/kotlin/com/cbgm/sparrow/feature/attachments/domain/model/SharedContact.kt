package com.cbgm.sparrow.feature.attachments.domain.model

data class SharedContact(
    val displayName: String?,
    val phoneNumber: String
) {
    init {
        require(phoneNumber.isNotBlank()) { "Shared contact phone number must not be blank" }
    }
}
