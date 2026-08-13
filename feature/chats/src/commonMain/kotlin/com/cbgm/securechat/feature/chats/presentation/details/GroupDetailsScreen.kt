package com.cbgm.securechat.feature.chats.presentation.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.component.SecureChatCardNoAnimation
import com.cbgm.securechat.core.ui.component.SecureChatLazyScaffold
import com.cbgm.securechat.core.ui.component.SecureChatOutlinedButton
import com.cbgm.securechat.core.ui.component.StatusBadge
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.chats.presentation.details.model.GroupDetailsUiEvent
import com.cbgm.securechat.feature.chats.presentation.details.model.GroupDetailsUiState
import com.cbgm.securechat.feature.chats.presentation.details.model.GroupMemberVerificationState
import com.cbgm.securechat.feature.chats.presentation.details.model.GroupMemberVerificationUiState
import com.cbgm.securechat.feature.chats.presentation.details.model.GroupVerificationSummaryUiState
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_verify
import com.cbgm.securechat.resources.base_verify_contact
import com.cbgm.securechat.resources.feature_chats_group_add_members
import com.cbgm.securechat.resources.feature_chats_group_admin
import com.cbgm.securechat.resources.feature_chats_group_details_accepted
import com.cbgm.securechat.resources.feature_chats_group_details_description
import com.cbgm.securechat.resources.feature_chats_group_details_members
import com.cbgm.securechat.resources.feature_chats_group_details_title
import com.cbgm.securechat.resources.feature_chats_group_details_total
import com.cbgm.securechat.resources.feature_chats_group_details_verified
import com.cbgm.securechat.resources.feature_chats_group_leave
import com.cbgm.securechat.resources.feature_chats_group_member_admin_verified_participant
import com.cbgm.securechat.resources.feature_chats_group_member_invitation_pending
import com.cbgm.securechat.resources.feature_chats_group_member_mutually_verified
import com.cbgm.securechat.resources.feature_chats_group_member_participant_verified_admin
import com.cbgm.securechat.resources.feature_chats_group_member_unavailable
import com.cbgm.securechat.resources.feature_chats_group_member_unverified
import com.cbgm.securechat.resources.feature_chats_group_orphaned_description
import com.cbgm.securechat.resources.feature_chats_group_orphaned_title
import com.cbgm.securechat.resources.feature_chats_group_promote_admin
import com.cbgm.securechat.resources.feature_chats_group_remove_member_name
import com.cbgm.securechat.resources.feature_chats_group_verification_pending_note
import com.cbgm.securechat.resources.feature_chats_group_verify_admin_description
import com.cbgm.securechat.resources.feature_chats_group_verify_admin_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    uiState: GroupDetailsUiState,
    onUiEvent: (GroupDetailsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SecureChatLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            TopBar(
                containerColor = containerColor,
                onBack = { onUiEvent(GroupDetailsUiEvent.BackClicked) }
            )
        }
    ) { innerPadding, listState ->
        Content(
            uiState = uiState,
            innerPadding = innerPadding,
            listState = listState,
            onUiEvent = onUiEvent
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    containerColor: Color,
    onBack: () -> Unit
) {
    Column {
        CenterAlignedTopAppBar(
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    scrolledContainerColor = containerColor,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
            title = {
                Text(
                    text = stringResource(Res.string.feature_chats_group_details_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null
                    )
                }
            }
        )
    }
}

@Composable
private fun Content(
    uiState: GroupDetailsUiState,
    innerPadding: PaddingValues,
    listState: LazyListState,
    onUiEvent: (GroupDetailsUiEvent) -> Unit
) {
    when (uiState) {
        GroupDetailsUiState.Loading ->
            LoadingContent(
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )

        is GroupDetailsUiState.Content ->
            MemberList(
                summary = uiState.summary,
                innerPadding = innerPadding,
                listState = listState,
                onVerifyMember = { contactId ->
                    onUiEvent(GroupDetailsUiEvent.VerifyMemberClicked(contactId))
                },
                onAddMembers = { onUiEvent(GroupDetailsUiEvent.AddMembersClicked) },
                onRemoveMember = { contactId ->
                    onUiEvent(GroupDetailsUiEvent.RemoveMemberClicked(contactId))
                },
                onPromoteMember = { contactId ->
                    onUiEvent(GroupDetailsUiEvent.PromoteMemberClicked(contactId))
                },
                onLeaveGroup = { onUiEvent(GroupDetailsUiEvent.LeaveGroupClicked) }
            )

        is GroupDetailsUiState.Error ->
            ErrorContent(
                message = uiState.message,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(MaterialTheme.spacing.medium)
            )
    }
}

@Composable
private fun Metric(
    value: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    SecureChatCardNoAnimation(modifier = modifier) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.small),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
private fun MetricPreview() {
    SecureChatTheme {
        Metric(
            value = 3,
            label = "Verified"
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Preview
@Composable
private fun ErrorContentPreview() {
    SecureChatTheme {
        ErrorContent(
            message = "Group details could not be loaded",
            modifier = Modifier.padding(24.dp)
        )
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
    }
}

@Preview
@Composable
private fun LoadingContentPreview() {
    SecureChatTheme {
        LoadingContent(modifier = Modifier.size(160.dp))
    }
}

@Composable
private fun Summary(summary: GroupVerificationSummaryUiState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.feature_chats_group_details_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (summary.isOrphaned) {
            Text(
                text = stringResource(Res.string.feature_chats_group_orphaned_title),
                modifier = Modifier.padding(top = MaterialTheme.spacing.small),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(Res.string.feature_chats_group_orphaned_description),
                modifier = Modifier.padding(top = MaterialTheme.spacing.base.div(2)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Metric(
                value = summary.mutuallyVerifiedParticipantCount,
                label = stringResource(Res.string.feature_chats_group_details_verified),
                modifier = Modifier.weight(1f)
            )
            Metric(
                value = summary.activeParticipantCount,
                label = stringResource(Res.string.feature_chats_group_details_accepted),
                modifier = Modifier.weight(1f)
            )
            Metric(
                value = summary.totalMemberCount,
                label = stringResource(Res.string.feature_chats_group_details_total),
                modifier = Modifier.weight(1f)
            )
        }

        if (summary.members.any { member -> !member.isActive }) {
            Text(
                text = stringResource(Res.string.feature_chats_group_verification_pending_note),
                modifier = Modifier.padding(top = MaterialTheme.spacing.small),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
private fun SummaryPreview() {
    SecureChatTheme {
        Summary(summary = GroupDetailsPreviewData.summary)
    }
}

@Composable
private fun MemberList(
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
                AdminVerificationCard(
                    admin = admin,
                    onVerify = {
                        admin.contactId?.let(onVerifyMember)
                    }
                )
            }
        }

        item(key = "summary") {
            Summary(summary = summary)
        }

        if (summary.isLocalAdmin && !summary.isOrphaned) {
            item(key = "member-management") {
                MemberManagementActions(onAddMembers = onAddMembers)
            }
        }
        if (summary.canLeaveGroup) {
            item(key = "leave-group") {
                LeaveAction(onLeaveGroup = onLeaveGroup)
            }
        }

        item(key = "members") {
            MembersCard(
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
private fun MemberListPreview() {
    SecureChatTheme {
        MemberList(
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

@Composable
private fun MemberManagementActions(
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
private fun MemberManagementActionsPreview() {
    SecureChatTheme {
        MemberManagementActions(onAddMembers = {})
    }
}

@Composable
private fun MemberRow(
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
private fun GroupMemberVerificationUiState.verificationStatusText(): String =
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
private fun MemberRowPreview() {
    SecureChatTheme {
        MemberRow(
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

@Composable
private fun MembersCard(
    summary: GroupVerificationSummaryUiState,
    onVerifyMember: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onPromoteMember: (String) -> Unit
) {
    Column {
        Text(
            text = stringResource(Res.string.feature_chats_group_details_members),
            modifier =
                Modifier.padding(
                    start = MaterialTheme.spacing.small,
                    bottom = MaterialTheme.spacing.small
                ),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.74f)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp,
            shadowElevation = 1.dp
        ) {
            Column {
                summary.members.forEachIndexed { index, member ->
                    MemberRow(
                        member = member,
                        showVerifyAction = summary.isLocalAdmin && member.canVerify,
                        showRemoveAction =
                            summary.isLocalAdmin &&
                                !member.isGroupAdmin &&
                                member.contactId != null,
                        showPromoteAction =
                            summary.isLocalAdmin &&
                                member.contactId in summary.promotableContactIds,
                        showDivider = index < summary.members.lastIndex,
                        onVerify = {
                            member.contactId?.let(onVerifyMember)
                        },
                        onRemove = {
                            member.contactId?.let(onRemoveMember)
                        },
                        onPromote = {
                            member.contactId?.let(onPromoteMember)
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun MembersCardPreview() {
    SecureChatTheme {
        MembersCard(
            summary = GroupDetailsPreviewData.summary,
            onVerifyMember = {},
            onRemoveMember = {},
            onPromoteMember = {}
        )
    }
}

@Composable
private fun LeaveAction(
    onLeaveGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    SecureChatOutlinedButton(
        onClick = onLeaveGroup,
        modifier = modifier.fillMaxWidth(),
        content = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null
            )
            Text(text = stringResource(Res.string.feature_chats_group_leave))
        }
    )
}

@Preview
@Composable
private fun LeaveActionPreview() {
    SecureChatTheme {
        LeaveAction(onLeaveGroup = {})
    }
}

@Composable
private fun AdminVerificationCard(
    admin: GroupMemberVerificationUiState,
    onVerify: () -> Unit
) {
    val adminName =
        admin.displayName.takeIf(String::isNotBlank)
            ?: stringResource(Res.string.feature_chats_group_admin)

    SecureChatCardNoAnimation(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Text(
                text = stringResource(Res.string.feature_chats_group_verify_admin_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(Res.string.feature_chats_group_verify_admin_description),
                modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = admin.verificationStatusText(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.74f),
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.small)
            )

            if (admin.canVerify && admin.contactId != null) {
                SecureChatApprovalButton(
                    onClick = onVerify,
                    text = stringResource(Res.string.base_verify_contact, adminName)
                )
            }
        }
    }
}

@Preview
@Composable
private fun AdminVerificationCardPreview() {
    SecureChatTheme {
        AdminVerificationCard(
            admin = GroupDetailsPreviewData.admin.copy(canVerify = true),
            onVerify = {}
        )
    }
}

@Preview
@Composable
private fun TopBarPreview() {
    SecureChatTheme {
        TopBar(
            containerColor = MaterialTheme.colorScheme.background,
            onBack = {}
        )
    }
}

@Preview
@Composable
private fun GroupDetailsScreenPreview() {
    SecureChatTheme {
        GroupDetailsScreen(
            uiState = GroupDetailsUiState.Content(GroupDetailsPreviewData.summary),
            onUiEvent = {}
        )
    }
}
