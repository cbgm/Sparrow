package com.cbgm.securechat.feature.chats.presentation.component.groupdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.chats.presentation.model.GroupMemberVerificationUiState
import com.cbgm.securechat.feature.chats.presentation.model.GroupVerificationSummaryUiState

@Composable
internal fun GroupMemberList(
    summary: GroupVerificationSummaryUiState,
    onVerifyMember: (String) -> Unit,
    onAddMembers: () -> Unit,
    onRemoveMember: (String) -> Unit,
    onPromoteMember: (String) -> Unit,
    onLeaveGroup: () -> Unit,
    innerPadding: PaddingValues,
    listState: LazyListState
) {
    val admin = summary.members.firstOrNull(GroupMemberVerificationUiState::isGroupAdmin)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding =
            PaddingValues(
                start = MaterialTheme.spacing.medium,
                top = innerPadding.calculateTopPadding(),
                end = MaterialTheme.spacing.medium,
                bottom = innerPadding.calculateBottomPadding()
            ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        if (!summary.isLocalAdmin && admin != null && admin.canVerify) {
            item(key = "verify-group-admin") {
                ParticipantAdminVerificationCard(
                    admin = admin,
                    onVerify = {
                        admin.contactId?.let(onVerifyMember)
                    }
                )
            }
        }

        item(key = "summary") {
            GroupDetailsSummary(summary = summary)
        }

        if (summary.isLocalAdmin && !summary.isOrphaned) {
            item(key = "member-management") {
                GroupMemberManagementActions(onAddMembers = onAddMembers)
            }
        }
        if (summary.canLeaveGroup) {
            item(key = "leave-group") {
                LeaveGroupAction(onLeaveGroup = onLeaveGroup)
            }
        }

        item(key = "members") {
            GroupMembersCard(
                summary = summary,
                onVerifyMember = onVerifyMember,
                onRemoveMember = onRemoveMember,
                onPromoteMember = onPromoteMember
            )
        }
    }
}

@Preview
@Composable
private fun GroupMemberListPreview() {
    SecureChatTheme {
        GroupMemberList(
            summary = GroupDetailsPreviewData.summary,
            onVerifyMember = {},
            onAddMembers = {},
            onRemoveMember = {},
            onPromoteMember = {},
            onLeaveGroup = {},
            innerPadding = PaddingValues(),
            listState = rememberLazyListState()
        )
    }
}
