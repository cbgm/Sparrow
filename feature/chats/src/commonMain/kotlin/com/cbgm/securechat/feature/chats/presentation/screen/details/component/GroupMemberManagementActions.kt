package com.cbgm.securechat.feature.chats.presentation.component.groupdetails

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.ui.component.SecureChatOutlinedButton
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_chats_group_add_members
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GroupMemberManagementActions(
    onAddMembers: () -> Unit,
    modifier: Modifier = Modifier
) {
    SecureChatOutlinedButton(
        onClick = onAddMembers,
        modifier = modifier.fillMaxWidth(),
        content = {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null
            )
            Text(text = stringResource(Res.string.feature_chats_group_add_members))
        }
    )
}

@Preview
@Composable
private fun GroupMemberManagementActionsPreview() {
    SecureChatTheme {
        GroupMemberManagementActions(onAddMembers = {})
    }
}
