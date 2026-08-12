package com.cbgm.securechat.feature.chats.presentation.component.groupdetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.component.StatusBadge
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.feature.chats.presentation.model.GroupMemberVerificationState
import com.cbgm.securechat.feature.chats.presentation.model.GroupMemberVerificationUiState
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_verify
import com.cbgm.securechat.resources.base_verify_contact
import com.cbgm.securechat.resources.feature_chats_group_admin
import com.cbgm.securechat.resources.feature_chats_group_member_admin_verified_participant
import com.cbgm.securechat.resources.feature_chats_group_member_invitation_pending
import com.cbgm.securechat.resources.feature_chats_group_member_mutually_verified
import com.cbgm.securechat.resources.feature_chats_group_member_participant_verified_admin
import com.cbgm.securechat.resources.feature_chats_group_member_unavailable
import com.cbgm.securechat.resources.feature_chats_group_member_unverified
import com.cbgm.securechat.resources.feature_chats_group_promote_admin
import com.cbgm.securechat.resources.feature_chats_group_remove_member_name
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GroupMemberRow(
    member: GroupMemberVerificationUiState,
    showVerifyAction: Boolean,
    showRemoveAction: Boolean,
    showPromoteAction: Boolean,
    showDivider: Boolean,
    onVerify: () -> Unit,
    onRemove: () -> Unit,
    onPromote: () -> Unit
) {
    val statusColor = member.verificationStatusColor()
    val displayName =
        member.displayName.takeIf(String::isNotBlank)
            ?: stringResource(Res.string.feature_chats_group_admin)
    val verifyDescription = stringResource(Res.string.base_verify_contact, displayName)

    Column(modifier = Modifier.fillMaxWidth().padding(0.dp)) {
        ListItem(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (showVerifyAction) {
                            Modifier.clickable(
                                onClickLabel = verifyDescription,
                                role = Role.Button,
                                onClick = onVerify
                            )
                        } else {
                            Modifier
                        }
                    ),
            leadingContent = {
                Icon(
                    imageVector = member.verificationStatusIcon(),
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(22.dp)
                )
            },
            headlineContent = {
                Text(
                    text = displayName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            supportingContent = {
                Text(
                    text = member.verificationStatusText(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f)
                )
            },
            trailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showVerifyAction) {
                        StatusBadge(
                            text = stringResource(Res.string.base_verify),
                            icon = Icons.Default.Verified,
                            color = statusColor
                        )
                    }
                    if (showPromoteAction) {
                        IconButton(onClick = onPromote) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription =
                                    stringResource(Res.string.feature_chats_group_promote_admin),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    if (showRemoveAction) {
                        IconButton(onClick = onRemove) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription =
                                    stringResource(
                                        Res.string.feature_chats_group_remove_member_name,
                                        displayName
                                    ),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        if (showDivider) {
            HorizontalDivider(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 80.dp),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.05f)
            )
        }
    }
}

@Composable
internal fun GroupMemberVerificationUiState.verificationStatusText(): String =
    if (isGroupAdmin) {
        stringResource(Res.string.feature_chats_group_admin)
    } else {
        when (state) {
            GroupMemberVerificationState.GROUP_ADMIN ->
                stringResource(Res.string.feature_chats_group_admin)

            GroupMemberVerificationState.MUTUALLY_VERIFIED ->
                stringResource(Res.string.feature_chats_group_member_mutually_verified)

            GroupMemberVerificationState.ADMIN_VERIFIED_PARTICIPANT ->
                stringResource(Res.string.feature_chats_group_member_admin_verified_participant)

            GroupMemberVerificationState.PARTICIPANT_VERIFIED_ADMIN ->
                stringResource(Res.string.feature_chats_group_member_participant_verified_admin)

            GroupMemberVerificationState.UNVERIFIED ->
                stringResource(Res.string.feature_chats_group_member_unverified)

            GroupMemberVerificationState.UNAVAILABLE ->
                stringResource(Res.string.feature_chats_group_member_unavailable)

            GroupMemberVerificationState.INVITATION_PENDING ->
                stringResource(Res.string.feature_chats_group_member_invitation_pending)
        }
    }

@Composable
private fun GroupMemberVerificationUiState.verificationStatusColor(): Color =
    if (isGroupAdmin) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        when (state) {
            GroupMemberVerificationState.GROUP_ADMIN ->
                MaterialTheme.colorScheme.onSurfaceVariant

            GroupMemberVerificationState.MUTUALLY_VERIFIED ->
                MaterialTheme.colorScheme.secondary

            GroupMemberVerificationState.ADMIN_VERIFIED_PARTICIPANT,
            GroupMemberVerificationState.PARTICIPANT_VERIFIED_ADMIN ->
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.73f)

            GroupMemberVerificationState.UNVERIFIED,
            GroupMemberVerificationState.UNAVAILABLE ->
                MaterialTheme.colorScheme.error

            GroupMemberVerificationState.INVITATION_PENDING ->
                MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

private fun GroupMemberVerificationUiState.verificationStatusIcon(): ImageVector =
    if (isGroupAdmin) {
        Icons.Default.Group
    } else {
        when (state) {
            GroupMemberVerificationState.MUTUALLY_VERIFIED -> Icons.Default.CheckCircle
            GroupMemberVerificationState.ADMIN_VERIFIED_PARTICIPANT,
            GroupMemberVerificationState.PARTICIPANT_VERIFIED_ADMIN -> Icons.Default.Lock

            GroupMemberVerificationState.UNVERIFIED,
            GroupMemberVerificationState.UNAVAILABLE -> Icons.Default.Warning
            GroupMemberVerificationState.INVITATION_PENDING -> Icons.Default.Schedule
            GroupMemberVerificationState.GROUP_ADMIN -> Icons.Default.Group
        }
    }

@Preview
@Composable
private fun GroupMemberRowPreview() {
    SecureChatTheme {
        GroupMemberRow(
            member = GroupDetailsPreviewData.participant,
            showVerifyAction = true,
            showRemoveAction = true,
            showPromoteAction = true,
            showDivider = false,
            onVerify = {},
            onRemove = {},
            onPromote = {}
        )
    }
}
