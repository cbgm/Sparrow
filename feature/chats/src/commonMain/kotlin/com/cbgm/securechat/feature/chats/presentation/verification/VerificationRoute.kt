package com.cbgm.securechat.feature.chats.presentation.verification

import androidx.compose.runtime.Composable
import com.cbgm.securechat.feature.contactimport.presentation.verify.ContactQrVerificationFlow

@Composable
fun VerificationRoute(
    contactId: String,
    groupId: String?
) {
    if (groupId == null) {
        ContactQrVerificationFlow(contactId = contactId)
    } else {
        GroupMemberVerificationFlow(
            groupId = groupId,
            contactId = contactId
        )
    }
}
