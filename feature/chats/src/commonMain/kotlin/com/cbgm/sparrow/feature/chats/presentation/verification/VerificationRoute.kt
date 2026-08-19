package com.cbgm.sparrow.feature.chats.presentation.verification

import androidx.compose.runtime.Composable
import com.cbgm.sparrow.feature.contactimport.presentation.verify.ContactQrVerificationFlow

@Composable
fun VerificationRoute(
    contactId: String,
    groupId: String?
) {
    if (groupId == null) {
        ContactQrVerificationFlow(contactId = contactId)
    } else {
        GroupMemberVerificationFlow()
    }
}
