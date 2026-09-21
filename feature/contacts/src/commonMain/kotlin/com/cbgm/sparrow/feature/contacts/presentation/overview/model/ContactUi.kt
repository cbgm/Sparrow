package com.cbgm.sparrow.feature.contacts.presentation.overview.model

import androidx.compose.runtime.Immutable

/** Immutable values intended for contact list presentation; no domain objects reach composables. */
@Immutable
data class ContactUi(
    val id: String,
    val displayName: String?,
    val preferredPhoneNumber: String?,
    val phoneNumbers: List<String>,
    val hasSparrowIdentity: Boolean,
    val deviceContactMissing: Boolean
)
