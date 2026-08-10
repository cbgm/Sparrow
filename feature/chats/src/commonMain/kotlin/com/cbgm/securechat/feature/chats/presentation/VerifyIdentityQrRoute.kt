package com.cbgm.securechat.feature.chats.presentation

import androidx.compose.runtime.Composable
import com.cbgm.securechat.feature.contactimport.presentation.ContactQrVerificationFlow

@Composable
fun VerifyIdentityQrRoute(
    contactId: String,
    groupId: String?
) {
    if (groupId == null) {
        ContactQrVerificationFlow(contactId = contactId)
    } else {
        GroupMemberQrVerificationFlow(
            groupId = groupId,
            contactId = contactId
        )
    }
}
