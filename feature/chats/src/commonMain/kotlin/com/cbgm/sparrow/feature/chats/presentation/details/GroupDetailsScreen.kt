package com.cbgm.sparrow.feature.chats.presentation.details

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.cbgm.sparrow.core.ui.avatar.editor.AvatarEditor
import com.cbgm.sparrow.core.ui.avatar.editor.AvatarEditorStrings
import com.cbgm.sparrow.core.ui.component.Avatar
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.component.SparrowSecondaryButton
import com.cbgm.sparrow.core.ui.component.StatusBadge
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupAvatarUiEvent
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupAvatarUiState
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupDetailsUiEvent
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupDetailsUiState
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupMemberVerificationState
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupMemberVerificationUiState
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupVerificationSummaryUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.base_verify
import com.cbgm.sparrow.resources.base_verify_contact
import com.cbgm.sparrow.resources.feature_chats_group_add_members
import com.cbgm.sparrow.resources.feature_chats_group_admin
import com.cbgm.sparrow.resources.feature_chats_group_avatar
import com.cbgm.sparrow.resources.feature_chats_group_avatar_add
import com.cbgm.sparrow.resources.feature_chats_group_avatar_change
import com.cbgm.sparrow.resources.feature_chats_group_avatar_choose_gallery
import com.cbgm.sparrow.resources.feature_chats_group_avatar_crop
import com.cbgm.sparrow.resources.feature_chats_group_avatar_description
import com.cbgm.sparrow.resources.feature_chats_group_avatar_remove
import com.cbgm.sparrow.resources.feature_chats_group_avatar_take_photo
import com.cbgm.sparrow.resources.feature_chats_group_details_accepted
import com.cbgm.sparrow.resources.feature_chats_group_details_description
import com.cbgm.sparrow.resources.feature_chats_group_details_members
import com.cbgm.sparrow.resources.feature_chats_group_details_title
import com.cbgm.sparrow.resources.feature_chats_group_details_total
import com.cbgm.sparrow.resources.feature_chats_group_details_verified
import com.cbgm.sparrow.resources.feature_chats_group_leave
import com.cbgm.sparrow.resources.feature_chats_group_member_admin_verified_participant
import com.cbgm.sparrow.resources.feature_chats_group_member_invitation_pending
import com.cbgm.sparrow.resources.feature_chats_group_member_mutually_verified
import com.cbgm.sparrow.resources.feature_chats_group_member_participant_verified_admin
import com.cbgm.sparrow.resources.feature_chats_group_member_unavailable
import com.cbgm.sparrow.resources.feature_chats_group_member_unverified
import com.cbgm.sparrow.resources.feature_chats_group_promote_admin
import com.cbgm.sparrow.resources.feature_chats_group_remove_member_name
import com.cbgm.sparrow.resources.feature_chats_group_verification_pending_note
import com.cbgm.sparrow.resources.feature_chats_group_verify_admin_description
import com.cbgm.sparrow.resources.feature_chats_group_verify_admin_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    uiState: GroupDetailsUiState,
    modifier: Modifier = Modifier,
    groupAvatarState: GroupAvatarUiState = GroupAvatarUiState(),
    onUiEvent: (GroupDetailsUiEvent) -> Unit,
    onGroupAvatarEvent: (GroupAvatarUiEvent) -> Unit = {}
) {
    var showAvatarEditor by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        SparrowLazyScaffold(
            modifier = Modifier.fillMaxSize(),
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
                groupAvatarState = groupAvatarState,
                innerPadding = innerPadding,
                listState = listState,
                onUiEvent = onUiEvent,
                onEditGroupAvatar = { showAvatarEditor = true },
                onRemoveGroupAvatar = { onGroupAvatarEvent(GroupAvatarUiEvent.RemoveAvatarClicked) }
            )
        }

        if (showAvatarEditor) {
            AvatarEditor(
                strings =
                    AvatarEditorStrings(
                        sourceTitle = stringResource(Res.string.feature_chats_group_avatar),
                        cropTitle = stringResource(Res.string.feature_chats_group_avatar_crop),
                        takePhoto = stringResource(Res.string.feature_chats_group_avatar_take_photo),
                        chooseFromGallery = stringResource(Res.string.feature_chats_group_avatar_choose_gallery),
                        cancel = stringResource(Res.string.base_cancel)
                    ),
                onAvatarSelected = { bytes ->
                    showAvatarEditor = false
                    onGroupAvatarEvent(GroupAvatarUiEvent.AvatarSelected(bytes))
                },
                onDismiss = { showAvatarEditor = false }
            )
        }
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
    groupAvatarState: GroupAvatarUiState,
    innerPadding: PaddingValues,
    listState: LazyListState,
    onUiEvent: (GroupDetailsUiEvent) -> Unit,
    onEditGroupAvatar: () -> Unit,
    onRemoveGroupAvatar: () -> Unit
) {
    when (uiState) {
        GroupDetailsUiState.Loading ->
            LoadingContent(
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )

        is GroupDetailsUiState.Content ->
            MemberList(
                summary = uiState.summary,
                groupAvatarState = groupAvatarState,
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
                onLeaveGroup = { onUiEvent(GroupDetailsUiEvent.LeaveGroupClicked) },
                onEditGroupAvatar = onEditGroupAvatar,
                onRemoveGroupAvatar = onRemoveGroupAvatar
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
    SparrowCardNoAnimation(modifier = modifier) {
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
    SparrowTheme {
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
    SparrowTheme {
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
    SparrowTheme {
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
    SparrowTheme {
        Summary(summary = GroupDetailsPreviewData.summary)
    }
}

@Composable
private fun GroupAvatarSection(
    state: GroupAvatarUiState,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.feature_chats_group_avatar),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Box(
            modifier = Modifier.padding(top = MaterialTheme.spacing.small),
            contentAlignment = Alignment.Center
        ) {
            Avatar(
                name = state.title,
                pictureBytes = state.avatarBytes,
                size = 104.dp
            )
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
            }
        }

        Text(
            text = stringResource(Res.string.feature_chats_group_avatar_description),
            modifier = Modifier.padding(top = MaterialTheme.spacing.small),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (state.canEdit) {
            SparrowSecondaryButton(
                onClick = onEdit,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.spacing.medium),
                text =
                    stringResource(
                        if (state.avatarBytes != null) {
                            Res.string.feature_chats_group_avatar_change
                        } else {
                            Res.string.feature_chats_group_avatar_add
                        }
                    )
            )

            if (state.avatarBytes != null) {
                SparrowOutlinedButton(
                    onClick = onRemove,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.spacing.small),
                    text = stringResource(Res.string.feature_chats_group_avatar_remove)
                )
            }
        }

        state.errorMessage?.let { error ->
            Text(
                text = error,
                modifier = Modifier.padding(top = MaterialTheme.spacing.small),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview
@Composable
private fun GroupAvatarSectionPreview() {
    SparrowTheme {
        GroupAvatarSection(
            state = GroupAvatarUiState(title = "Sparrow Team", canEdit = true),
            onEdit = {},
            onRemove = {}
        )
    }
}

@Composable
private fun MemberList(
    summary: GroupVerificationSummaryUiState,
    groupAvatarState: GroupAvatarUiState,
    onVerifyMember: (String) -> Unit,
    onAddMembers: () -> Unit,
    onRemoveMember: (String) -> Unit,
    onPromoteMember: (String) -> Unit,
    onLeaveGroup: () -> Unit,
    onEditGroupAvatar: () -> Unit,
    onRemoveGroupAvatar: () -> Unit,
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
        item(key = "group-avatar") {
            GroupAvatarSection(
                state = groupAvatarState,
                onEdit = onEditGroupAvatar,
                onRemove = onRemoveGroupAvatar
            )
        }

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

        if (summary.isLocalAdmin) {
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
    SparrowTheme {
        MemberList(
            summary = GroupDetailsPreviewData.summary,
            groupAvatarState = GroupAvatarUiState(title = "Sparrow Team", canEdit = true),
            onVerifyMember = {},
            onAddMembers = {},
            onRemoveMember = {},
            onPromoteMember = {},
            onLeaveGroup = {},
            onEditGroupAvatar = {},
            onRemoveGroupAvatar = {},
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
    SparrowOutlinedButton(
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
    SparrowTheme {
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
    SparrowTheme {
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
    SparrowTheme {
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
    SparrowOutlinedButton(
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
    SparrowTheme {
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

    SparrowCardNoAnimation(modifier = Modifier.fillMaxWidth()) {
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
                SparrowApprovalButton(
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
    SparrowTheme {
        AdminVerificationCard(
            admin = GroupDetailsPreviewData.admin.copy(canVerify = true),
            onVerify = {}
        )
    }
}

@Preview
@Composable
private fun TopBarPreview() {
    SparrowTheme {
        TopBar(
            containerColor = MaterialTheme.colorScheme.background,
            onBack = {}
        )
    }
}

@Preview
@Composable
private fun GroupDetailsScreenPreview() {
    SparrowTheme {
        GroupDetailsScreen(
            uiState = GroupDetailsUiState.Content(GroupDetailsPreviewData.summary),
            onUiEvent = {}
        )
    }
}
