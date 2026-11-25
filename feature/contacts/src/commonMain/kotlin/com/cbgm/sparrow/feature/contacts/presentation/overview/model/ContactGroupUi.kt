package com.cbgm.sparrow.feature.contacts.presentation.overview.model

import androidx.compose.runtime.Immutable

@Immutable
data class ContactGroupUi(
    val title: String,
    val contacts: List<ContactUi>
)
