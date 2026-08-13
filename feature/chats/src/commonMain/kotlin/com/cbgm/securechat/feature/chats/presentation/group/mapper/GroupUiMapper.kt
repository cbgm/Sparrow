package com.cbgm.securechat.feature.chats.presentation.group.mapper

import com.cbgm.securechat.feature.chats.domain.model.GroupVerificationMembershipStatus
import com.cbgm.securechat.feature.chats.domain.model.GroupVerificationState
import com.cbgm.securechat.feature.contacts.domain.model.Contact

internal data class GroupMemberCounts(
    val total: Int,
    val active: Int
)

internal fun GroupVerificationState?.toMemberCounts(
    currentMemberContactIds: Set<String>
): GroupMemberCounts? {
    val state = this ?: return null
    val participantRows = state.pairs.distinctBy { pair -> pair.invitationId }
    val hasAuthoritativeState = state.context.isLocalAdmin || participantRows.isNotEmpty()
    if (!hasAuthoritativeState) return null

    val activeCount =
        participantRows.count { pair ->
            pair.membershipStatus == GroupVerificationMembershipStatus.ACTIVE &&
                (pair.contactId == null || pair.contactId in currentMemberContactIds)
        }

    return GroupMemberCounts(
        total = participantRows.size + 1,
        active = activeCount
    )
}

internal fun Contact?.displayNameForChat(isInContacts: Boolean): String {
    if (this == null) return "Unknown contact"

    return if (isInContacts) {
        displayName?.takeIf(String::isNotBlank)
            ?: preferredPhoneNumber?.value
            ?: "Unknown contact"
    } else {
        preferredPhoneNumber?.value
            ?: displayName?.takeIf(String::isNotBlank)
            ?: "Unknown contact"
    }
}
