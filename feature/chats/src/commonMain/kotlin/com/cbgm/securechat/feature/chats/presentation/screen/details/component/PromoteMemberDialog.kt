package com.cbgm.securechat.feature.chats.presentation.screen.details.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.component.SecureChatAlertDialog
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.component.SecureChatOutlinedButton
import com.cbgm.securechat.feature.chats.presentation.model.GroupMemberVerificationUiState
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_cancel
import com.cbgm.securechat.resources.feature_chats_group_promote_admin
import com.cbgm.securechat.resources.feature_chats_group_promote_admin_description
import org.jetbrains.compose.resources.stringResource

@Composable
fun PromoteMemberDialog(
    member: GroupMemberVerificationUiState,
    isUpdating: Boolean,
    errorMessage: String?,
    onApprove: () -> Unit,
    onDismiss: () -> Unit
) {
    SecureChatAlertDialog(
        onDismissRequest = {},
        title = stringResource(Res.string.feature_chats_group_promote_admin),
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text =
                        stringResource(
                            Res.string.feature_chats_group_promote_admin_description,
                            member.displayName
                        )
                )
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            SecureChatApprovalButton(
                onClick = onApprove,
                fillMaxWidth = false,
                content = {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(Res.string.feature_chats_group_promote_admin))
                    }
                }
            )
        },
        dismissButton = {
            SecureChatOutlinedButton(
                onClick = onDismiss,
                text = stringResource(Res.string.base_cancel),
                fillMaxWidth = false
            )
        }
    )
}
