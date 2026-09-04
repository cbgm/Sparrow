package com.cbgm.sparrow.feature.chats.presentation.group.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMemberInvitationStatus
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupConversationUiEvent
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupConversationUiState
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupMembershipUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_group_deleted_status
import com.cbgm.sparrow.resources.feature_chats_group_member_accepted
import com.cbgm.sparrow.resources.feature_chats_group_member_active
import com.cbgm.sparrow.resources.feature_chats_group_member_count
import com.cbgm.sparrow.resources.feature_chats_group_member_declined
import com.cbgm.sparrow.resources.feature_chats_group_member_expired
import com.cbgm.sparrow.resources.feature_chats_group_member_failed
import com.cbgm.sparrow.resources.feature_chats_group_member_invited
import com.cbgm.sparrow.resources.feature_chats_group_member_key_sent
import com.cbgm.sparrow.resources.feature_chats_group_message_queued
import com.cbgm.sparrow.resources.feature_chats_group_status_declined
import com.cbgm.sparrow.resources.feature_chats_group_status_distributing
import com.cbgm.sparrow.resources.feature_chats_group_status_expired
import com.cbgm.sparrow.resources.feature_chats_group_status_failed
import com.cbgm.sparrow.resources.feature_chats_group_status_invited
import com.cbgm.sparrow.resources.feature_chats_group_status_joining
import com.cbgm.sparrow.resources.feature_chats_group_status_leaving
import com.cbgm.sparrow.resources.feature_chats_group_status_partial
import com.cbgm.sparrow.resources.feature_chats_group_status_removed
import com.cbgm.sparrow.resources.feature_chats_group_status_waiting
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun StatusHint(
    uiState: GroupConversationUiState,
    membershipState: GroupMembershipUiState,
    onUiEvent: (GroupConversationUiEvent) -> Unit
) {
    when {
        uiState.state == GroupConversationState.INVITED ->
            InvitationHint(
                onAccept = { onUiEvent(GroupConversationUiEvent.AcceptInvitation) },
                onDecline = { onUiEvent(GroupConversationUiEvent.DeclineInvitation) }
            )

        uiState.state == GroupConversationState.DELETED -> ConversationDeletedHint()
        uiState.state == GroupConversationState.REMOVED ||
            (uiState.state == GroupConversationState.DECLINED && uiState.messages.isNotEmpty()) ->
            MembershipRemovedHint()

        uiState.state == GroupConversationState.LEAVING -> MembershipLeavingHint()
        uiState.state != GroupConversationState.READY && uiState.composerState.isInputEnabled ->
            PendingMessageHint(membershipState = membershipState)
    }
}

@Composable
private fun PendingMessageHint(membershipState: GroupMembershipUiState) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.spacing.medium,
                        vertical = MaterialTheme.spacing.small
                    )
        ) {
            Text(
                text = stringResource(Res.string.feature_chats_group_message_queued),
                style = MaterialTheme.typography.bodySmall
            )
            membershipState.memberProgress.forEach { member ->
                Text(
                    text = "${member.displayName} · ${memberStatus(member.status)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
internal fun subtitle(
    uiState: GroupConversationUiState,
    membershipState: GroupMembershipUiState
): String =
    when (uiState.state) {
        GroupConversationState.READY ->
            stringResource(Res.string.feature_chats_group_member_count, membershipState.memberCount)

        GroupConversationState.INVITED -> stringResource(Res.string.feature_chats_group_status_invited)
        GroupConversationState.JOINING -> stringResource(Res.string.feature_chats_group_status_joining)
        GroupConversationState.WAITING_FOR_MEMBERS ->
            pendingSubtitle(
                readyCount = membershipState.readyMemberCount,
                pendingCount = membershipState.pendingMemberCount,
                waitingResource = Res.string.feature_chats_group_status_waiting
            )

        GroupConversationState.DISTRIBUTING_KEYS ->
            pendingSubtitle(
                readyCount = membershipState.readyMemberCount,
                pendingCount = membershipState.pendingMemberCount,
                waitingResource = Res.string.feature_chats_group_status_distributing
            )

        GroupConversationState.LEAVING -> stringResource(Res.string.feature_chats_group_status_leaving)
        GroupConversationState.REMOVED -> stringResource(Res.string.feature_chats_group_status_removed)
        GroupConversationState.DELETED -> stringResource(Res.string.feature_chats_group_deleted_status)
        GroupConversationState.DECLINED -> stringResource(Res.string.feature_chats_group_status_declined)
        GroupConversationState.EXPIRED -> stringResource(Res.string.feature_chats_group_status_expired)
        GroupConversationState.FAILED -> stringResource(Res.string.feature_chats_group_status_failed)
    }

@Composable
private fun pendingSubtitle(
    readyCount: Int,
    pendingCount: Int,
    waitingResource: StringResource
): String =
    if (readyCount > 0) {
        stringResource(
            Res.string.feature_chats_group_status_partial,
            readyCount,
            pendingCount
        )
    } else {
        stringResource(waitingResource, pendingCount)
    }

@Composable
private fun memberStatus(status: GroupMemberInvitationStatus): String =
    when (status) {
        GroupMemberInvitationStatus.INVITED -> stringResource(Res.string.feature_chats_group_member_invited)
        GroupMemberInvitationStatus.ACCEPTED -> stringResource(Res.string.feature_chats_group_member_accepted)
        GroupMemberInvitationStatus.KEY_SENT -> stringResource(Res.string.feature_chats_group_member_key_sent)
        GroupMemberInvitationStatus.ACTIVE -> stringResource(Res.string.feature_chats_group_member_active)
        GroupMemberInvitationStatus.DECLINED -> stringResource(Res.string.feature_chats_group_member_declined)
        GroupMemberInvitationStatus.EXPIRED -> stringResource(Res.string.feature_chats_group_member_expired)
        GroupMemberInvitationStatus.FAILED -> stringResource(Res.string.feature_chats_group_member_failed)
    }
