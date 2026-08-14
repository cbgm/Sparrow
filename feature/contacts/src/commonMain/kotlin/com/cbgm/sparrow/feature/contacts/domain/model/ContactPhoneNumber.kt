package com.cbgm.sparrow.feature.contacts.domain.model

data class ContactPhoneNumber(
    val id: String,
    val value: String,
    val type: ContactPhoneNumberType,
    val label: String?
)
