package com.cbgm.securechat.feature.chats.presentation.screen.details.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.component.SecureChatAlertDialog
import com.cbgm.securechat.core.ui.component.SecureChatOutlinedButton
import com.cbgm.securechat.feature.chats.presentation.model.GroupMemberVerificationUiState
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_cancel
import com.cbgm.securechat.resources.feature_chats_group_promote_before_leave
import com.cbgm.securechat.resources.feature_chats_group_promote_before_leave_description
import org.jetbrains.compose.resources.stringResource

@Composable
fun PromoteAdminBeforeLeaveDialog(
    members: List<GroupMemberVerificationUiState>,
    isUpdating: Boolean,
    errorMessage: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    SecureChatAlertDialog(
        onDismissRequest = {},
        title = stringResource(Res.string.feature_chats_group_promote_before_leave),
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.feature_chats_group_promote_before_leave_description))
                members.forEachIndexed { index, member ->
                    val contactId = member.contactId ?: return@forEachIndexed
                    ListItem(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isUpdating) { onSelect(contactId) },
                        headlineContent = { Text(member.displayName) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    if (index < members.lastIndex) {
                        HorizontalDivider()
                    }
                }
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            SecureChatOutlinedButton(
                onClick = onDismiss,
                text = stringResource(Res.string.base_cancel),
                fillMaxWidth = false
            )
        }
    )
}
