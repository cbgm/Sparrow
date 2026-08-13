package com.cbgm.securechat.feature.contacts.presentation.overview.model

import androidx.compose.runtime.Immutable
import com.cbgm.securechat.feature.contacts.domain.model.Contact

@Immutable
data class ContactGroupEntity(
    val title: String,
    val contacts: List<Contact>
)
