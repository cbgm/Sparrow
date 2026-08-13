package com.cbgm.securechat.feature.contacts.presentation.overview.model

sealed interface ContactsScreenMode {
    val searchQuery: String

    data class Overview(
        override val searchQuery: String
    ) : ContactsScreenMode

    data class GroupSelection(
        val title: String,
        val selectedContactIds: Set<String>,
        val confirmEnabled: Boolean,
        val confirming: Boolean,
        override val searchQuery: String
    ) : ContactsScreenMode

    data class MemberSelection(
        val title: String,
        val selectedContactIds: Set<String>,
        val confirmEnabled: Boolean,
        val confirming: Boolean,
        override val searchQuery: String
    ) : ContactsScreenMode
}
