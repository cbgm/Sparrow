package com.cbgm.sparrow.feature.chats.presentation.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.base.group.domain.model.GroupMetadataPolicy
import com.cbgm.sparrow.core.ui.component.SparrowBanner
import com.cbgm.sparrow.core.ui.component.SparrowBannerAction
import com.cbgm.sparrow.core.ui.component.SparrowDetailRow
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.component.SparrowSectionLabel
import com.cbgm.sparrow.core.ui.component.SparrowSwipeRevealItem
import com.cbgm.sparrow.core.ui.component.SparrowTopBar
import com.cbgm.sparrow.core.ui.component.SwipeActionTone
import com.cbgm.sparrow.core.ui.component.SwipeRevealAction
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.divider
import com.cbgm.sparrow.core.ui.theme.errorBright
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.avatar.presentation.component.SparrowAvatar
import com.cbgm.sparrow.feature.avatar.presentation.editor.AvatarEditor
import com.cbgm.sparrow.feature.avatar.presentation.editor.AvatarEditorStrings
import com.cbgm.sparrow.feature.avatar.presentation.model.AvatarRing
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupAvatarUiState
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupDescriptionUiState
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupDetailsUiEvent
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupDetailsUiState
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupMemberVerificationState
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupMemberVerificationUiState
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupVerificationSummaryUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.base_verify_contact
import com.cbgm.sparrow.resources.feature_attachments_media_and_files
import com.cbgm.sparrow.resources.feature_chats_group_add_members
import com.cbgm.sparrow.resources.feature_chats_group_admin
import com.cbgm.sparrow.resources.feature_chats_group_avatar
import com.cbgm.sparrow.resources.feature_chats_group_avatar_choose_gallery
import com.cbgm.sparrow.resources.feature_chats_group_avatar_crop
import com.cbgm.sparrow.resources.feature_chats_group_avatar_remove
import com.cbgm.sparrow.resources.feature_chats_group_avatar_take_photo
import com.cbgm.sparrow.resources.feature_chats_group_description_edit
import com.cbgm.sparrow.resources.feature_chats_group_description_empty
import com.cbgm.sparrow.resources.feature_chats_group_description_placeholder
import com.cbgm.sparrow.resources.feature_chats_group_description_save
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
    onUiEvent: (GroupDetailsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAvatarEditor by remember { mutableStateOf(false) }
    var isEditingTitle by remember { mutableStateOf(false) }
    var titleDraft by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                val containerColor = MaterialTheme.colorScheme.background
                val titleState = (uiState as? GroupDetailsUiState.Content)?.groupTitle
                TopBar(
                    containerColor = containerColor,
                    title = titleState?.title
                        ?: stringResource(Res.string.feature_chats_group_details_title),
                    canEditTitle = titleState?.canEdit == true,
                    isSavingTitle = titleState?.isSaving == true,
                    isEditingTitle = isEditingTitle,
                    titleDraft = titleDraft,
                    onTitleDraftChanged = { titleDraft = it },
                    onEditTitle = {
                        titleDraft = titleState?.title.orEmpty()
                        isEditingTitle = true
                    },
                    onSaveTitle = {
                        if (titleDraft.isNotBlank()) {
                            isEditingTitle = false
                            onUiEvent(GroupDetailsUiEvent.SaveGroupTitleClicked(titleDraft))
                        }
                    },
                    onCancelTitle = {
                        isEditingTitle = false
                        titleDraft = titleState?.title.orEmpty()
                    },
                    onBack = { onUiEvent(GroupDetailsUiEvent.BackClicked) }
                )
            },
            bottomBar = {
                val visibleDetails = uiState as? GroupDetailsUiState.Content
                if (visibleDetails != null) {
                    GroupDetailsBottomActions(
                        canLeaveGroup = visibleDetails.summary.canLeaveGroup,
                        onMediaAndFiles = { onUiEvent(GroupDetailsUiEvent.MediaAndFilesClicked) },
                        onLeaveGroup = { onUiEvent(GroupDetailsUiEvent.LeaveGroupClicked) }
                    )
                }
            }
        ) { innerPadding ->
            Content(
                uiState = uiState,
                innerPadding = innerPadding,
                onUiEvent = onUiEvent,
                onEditGroupAvatar = { showAvatarEditor = true }
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
                        remove =
                            if ((uiState as? GroupDetailsUiState.Content)?.groupAvatar?.hasAvatar == true) {
                                stringResource(Res.string.feature_chats_group_avatar_remove)
                            } else {
                                null
                            },
                        cancel = stringResource(Res.string.base_cancel)
                    ),
                onAvatarSelected = { result ->
                    showAvatarEditor = false
                    onUiEvent(GroupDetailsUiEvent.AvatarSelected(result))
                },
                onRemoveAvatar = {
                    showAvatarEditor = false
                    onUiEvent(GroupDetailsUiEvent.RemoveGroupAvatarClicked)
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
    title: String,
    canEditTitle: Boolean,
    isSavingTitle: Boolean,
    isEditingTitle: Boolean,
    titleDraft: String,
    onTitleDraftChanged: (String) -> Unit,
    onEditTitle: () -> Unit,
    onSaveTitle: () -> Unit,
    onCancelTitle: () -> Unit,
    onBack: () -> Unit
) {
    val titleFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(isEditingTitle) {
        if (isEditingTitle) {
            titleFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    Column {
        SparrowTopBar(
            containerColor = containerColor,
            title = {
                if (isEditingTitle) {
                    SparrowInputField(
                        value = titleDraft,
                        onValueChange = onTitleDraftChanged,
                        label = "",
                        isEnabled = !isSavingTitle,
                        isSingleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onSaveTitle() }),
                        focusRequester = titleFocusRequester
                    )
                } else {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack, enabled = !isSavingTitle) {
                    Icon(
                        imageVector = SparrowIcons.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.TopBar.iconSize)
                    )
                }
            },
            actions = {
                when {
                    isEditingTitle -> {
                        IconButton(
                            onClick = onSaveTitle,
                            enabled = !isSavingTitle && titleDraft.isNotBlank()
                        ) {
                            if (isSavingTitle) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(Dimens.GroupDetailsScreen.verificationProgressSize),
                                    strokeWidth = Dimens.Base.progressIndicatorStrokeWidth
                                )
                            } else {
                                Icon(
                                    imageVector = SparrowIcons.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(Dimens.TopBar.iconSize)
                                )
                            }
                        }
                        IconButton(onClick = onCancelTitle, enabled = !isSavingTitle) {
                            Icon(
                                imageVector = SparrowIcons.Close,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.TopBar.iconSize)
                            )
                        }
                    }

                    canEditTitle -> {
                        IconButton(onClick = onEditTitle, enabled = !isSavingTitle) {
                            Icon(
                                imageVector = SparrowIcons.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.TopBar.iconSize)
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun Content(
    uiState: GroupDetailsUiState,
    innerPadding: PaddingValues,
    onUiEvent: (GroupDetailsUiEvent) -> Unit,
    onEditGroupAvatar: () -> Unit
) {
    when (uiState) {
        GroupDetailsUiState.Loading ->
            Loading(
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )

        is GroupDetailsUiState.Content ->
            Content(
                summary = uiState.summary,
                groupAvatarState = uiState.groupAvatar,
                groupDescriptionState = uiState.groupDescription,
                innerPadding = innerPadding,
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
                onEditGroupAvatar = onEditGroupAvatar,
                onSaveGroupDescription = { description ->
                    onUiEvent(GroupDetailsUiEvent.SaveGroupDescriptionClicked(description))
                }
            )

        is GroupDetailsUiState.Error ->
            Error(
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
    Column(
        modifier = modifier.padding(vertical = MaterialTheme.spacing.base),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
private fun Error(
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
private fun ErrorPreview() {
    SparrowTheme {
        Error(
            message = "Group details could not be loaded",
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Composable
private fun Loading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Preview
@Composable
private fun LoadingPreview() {
    SparrowTheme {
        Loading(modifier = Modifier.size(Dimens.GroupDetailsScreen.loadingSize))
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
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            SparrowAvatar(
                name = state.title,
                target = state.groupId
                    .takeIf(String::isNotBlank)
                    ?.let { AvatarTarget.Group(it) },
                size = Dimens.Avatar.profileSize
            )

            if (state.canEdit) {
                Surface(
                    onClick = onEdit,
                    enabled = !state.isSaving,
                    modifier = Modifier.align(Alignment.BottomEnd),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Box(modifier = Modifier.size(Dimens.GroupDetailsScreen.cameraBadgeSize), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.GroupDetailsScreen.cameraIconSize)
                        )
                    }
                }
            }

            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.GroupDetailsScreen.avatarProgressSize),
                    strokeWidth = Dimens.GroupDetailsScreen.avatarProgressStrokeWidth
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
            onEdit = {}
        )
    }
}

@Composable
private fun GroupDescriptionSection(
    state: GroupDescriptionUiState,
    onSave: (String) -> Unit
) {
    var isEditing by remember(state.description) { mutableStateOf(false) }
    var draft by remember(state.description) { mutableStateOf(state.description) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (isEditing && state.canEdit) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SparrowInputField(
                    value = draft,
                    onValueChange = { if (it.length <= GroupMetadataPolicy.MAX_DESCRIPTION_LENGTH) draft = it },
                    label = "",
                    placeholderText = stringResource(Res.string.feature_chats_group_description_placeholder),
                    modifier = Modifier.weight(1f),
                    isEnabled = !state.isSaving,
                    minLines = 1,
                    maxLines = 4,
                    focusRequester = focusRequester
                )
                IconButton(
                    onClick = {
                        isEditing = false
                        draft = state.description
                    },
                    enabled = !state.isSaving
                ) {
                    Icon(
                        SparrowIcons.Close,
                        contentDescription = stringResource(Res.string.base_cancel)
                    )
                }
                IconButton(
                    onClick = {
                        isEditing = false
                        keyboardController?.hide()
                        onSave(draft)
                    },
                    enabled = !state.isSaving
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            SparrowIcons.Check,
                            contentDescription = stringResource(Res.string.feature_chats_group_description_save),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Text(
                text = "${draft.length}/${GroupMetadataPolicy.MAX_DESCRIPTION_LENGTH}",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End
            )
        } else {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = state.description.takeIf(String::isNotBlank)
                        ?: stringResource(Res.string.feature_chats_group_description_empty),
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = if (state.canEdit) 44.dp else 0.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (state.canEdit) {
                    IconButton(
                        onClick = {
                            draft = state.description
                            isEditing = true
                        },
                        enabled = !state.isSaving,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = SparrowIcons.Edit,
                            contentDescription = stringResource(Res.string.feature_chats_group_description_edit),
                            modifier = Modifier.size(Dimens.GroupDetailsScreen.memberStatusIconSize),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Content(
    summary: GroupVerificationSummaryUiState,
    groupAvatarState: GroupAvatarUiState,
    groupDescriptionState: GroupDescriptionUiState,
    onVerifyMember: (String) -> Unit,
    onAddMembers: () -> Unit,
    onRemoveMember: (String) -> Unit,
    onPromoteMember: (String) -> Unit,
    onEditGroupAvatar: () -> Unit,
    onSaveGroupDescription: (String) -> Unit,
    innerPadding: PaddingValues
) {
    val admin = summary.members.firstOrNull(GroupMemberVerificationUiState::isGroupAdmin)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = MaterialTheme.spacing.screenPadding)
            .padding(top = MaterialTheme.spacing.base, bottom = MaterialTheme.spacing.base),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        GroupAvatarSection(state = groupAvatarState, onEdit = onEditGroupAvatar)
        if (groupDescriptionState.canEdit || groupDescriptionState.description.isNotBlank()) {
            GroupDescriptionSection(state = groupDescriptionState, onSave = onSaveGroupDescription)
        }
        if (!summary.isLocalAdmin && admin != null && admin.canVerify) {
            AdminVerificationCard(admin = admin, onVerify = {
                admin.contactId?.let(onVerifyMember)
            })
        }
        MembersCard(
            summary = summary,
            onAddMembers = if (summary.isLocalAdmin) onAddMembers else null,
            onVerifyMember = onVerifyMember,
            onRemoveMember = onRemoveMember,
            onPromoteMember = onPromoteMember,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Preview
@Composable
private fun ContentPreview() {
    SparrowTheme {
        Content(
            summary = GroupDetailsPreviewData.summary,
            groupAvatarState = GroupAvatarUiState(title = "Sparrow Team", canEdit = true),
            groupDescriptionState = GroupDescriptionUiState(
                description = "A private Sparrow group.",
                canEdit = true
            ),
            onVerifyMember = {},
            onAddMembers = {},
            onRemoveMember = {},
            onPromoteMember = {},
            onEditGroupAvatar = {},
            onSaveGroupDescription = {},
            innerPadding = PaddingValues()
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
    val displayName = member.displayName.takeIf(String::isNotBlank)
        ?: stringResource(Res.string.feature_chats_group_admin)
    val verifyDescription = stringResource(Res.string.base_verify_contact, displayName)
    val removeDescription =
        stringResource(Res.string.feature_chats_group_remove_member_name, displayName)
    val promoteDescription = stringResource(Res.string.feature_chats_group_promote_admin)

    val actions = buildList {
        if (showPromoteAction) {
            add(
                SwipeRevealAction(
                    tone = SwipeActionTone.PRIMARY,
                    onClick = onPromote
                ) {
                    Icon(
                        SparrowIcons.ShieldCheck,
                        contentDescription = promoteDescription
                    )
                }
            )
        }
        if (showRemoveAction) {
            add(
                SwipeRevealAction(
                    tone = SwipeActionTone.DESTRUCTIVE,
                    onClick = onRemove
                ) {
                    Icon(
                        SparrowIcons.Delete,
                        contentDescription = removeDescription
                    )
                }
            )
        }
    }
    SparrowSwipeRevealItem(actions = actions) {
        Row(
            modifier = Modifier
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
                )
                .padding(vertical = MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SparrowAvatar(
                name = displayName,
                target = member.contactId?.takeIf(String::isNotBlank)
                    ?.let { AvatarTarget.User(it) },
                size = Dimens.Avatar.defaultSize,
                ring = member.avatarRing()
            )
            Column(modifier = Modifier.weight(1f).padding(start = MaterialTheme.spacing.small)) {
                Text(
                    text = displayName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = member.verificationStatusText(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth()
                .padding(start = Dimens.Avatar.defaultSize + MaterialTheme.spacing.small),
            thickness = Dimens.Base.dividerThickness,
            color = MaterialTheme.colorScheme.divider
        )
    }
}

private fun GroupMemberVerificationUiState.avatarRing(): AvatarRing =
    when (state) {
        GroupMemberVerificationState.MUTUALLY_VERIFIED -> AvatarRing.VERIFIED
        GroupMemberVerificationState.GROUP_ADMIN,
        GroupMemberVerificationState.ADMIN_VERIFIED_PARTICIPANT,
        GroupMemberVerificationState.PARTICIPANT_VERIFIED_ADMIN,
        GroupMemberVerificationState.UNVERIFIED,
        GroupMemberVerificationState.UNAVAILABLE,
        GroupMemberVerificationState.INVITATION_PENDING -> AvatarRing.PENDING
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
    onPromoteMember: (String) -> Unit,
    modifier: Modifier = Modifier,
    onAddMembers: (() -> Unit)? = null
) {
    // Do not add any padding around the member rows: swipe actions extend to the edges.
    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MaterialTheme.spacing.base),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SparrowSectionLabel(
                    text = stringResource(Res.string.feature_chats_group_details_members),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${summary.totalMemberCount} \u00B7 ${summary.mutuallyVerifiedParticipantCount} " +
                        stringResource(Res.string.feature_chats_group_details_verified).lowercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(thickness = Dimens.Base.dividerThickness, color = MaterialTheme.colorScheme.divider)
            // Only this list scrolls; member count / Add Members remain visible.
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                if (onAddMembers != null) {
                    item(key = "add-members") {
                        AddMembersRow(onClick = onAddMembers)
                    }
                }
                items(
                    count = summary.members.size,
                    key = { index -> summary.members[index].contactId ?: "member-$index" }
                ) { index ->
                    val member = summary.members[index]
                    MemberRow(
                        member = member,
                        showVerifyAction = summary.canVerify(member),
                        showRemoveAction = summary.canRemove(member),
                        showPromoteAction = summary.canPromote(member),
                        showDivider = index < summary.members.lastIndex,
                        onVerify = { member.contactId?.let(onVerifyMember) },
                        onRemove = { member.contactId?.let(onRemoveMember) },
                        onPromote = { member.contactId?.let(onPromoteMember) }
                    )
                }
            }
        }
    }
}

/** The first row of the member list for admins: an empty dashed circle with a thin plus, like a member still to come. */
@Composable
private fun AddMembersRow(onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.Avatar.defaultSize)
                    .dashedCircle(MaterialTheme.colorScheme.outline),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = SparrowIcons.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = stringResource(Res.string.feature_chats_group_add_members),
                modifier = Modifier.padding(start = MaterialTheme.spacing.small),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth().padding(start = Dimens.Avatar.defaultSize + MaterialTheme.spacing.small),
            thickness = Dimens.Base.dividerThickness,
            color = MaterialTheme.colorScheme.divider
        )
    }
}

private fun Modifier.dashedCircle(color: Color): Modifier =
    drawBehind {
        val stroke = Dimens.Avatar.ringStroke.toPx()
        drawCircle(
            color = color,
            radius = (size.minDimension - stroke) / 2f,
            style = Stroke(
                width = stroke,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(Dimens.Avatar.ringDash.toPx(), Dimens.Avatar.ringDash.toPx()))
            )
        )
    }

private fun GroupVerificationSummaryUiState.canVerify(member: GroupMemberVerificationUiState): Boolean =
    isLocalAdmin && member.canVerify

private fun GroupVerificationSummaryUiState.canRemove(member: GroupMemberVerificationUiState): Boolean =
    isLocalAdmin && !member.isGroupAdmin && member.contactId != null

private fun GroupVerificationSummaryUiState.canPromote(member: GroupMemberVerificationUiState): Boolean =
    isLocalAdmin && member.contactId in promotableContactIds

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
private fun GroupDetailsBottomActions(
    canLeaveGroup: Boolean,
    onMediaAndFiles: () -> Unit,
    onLeaveGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(
                horizontal = MaterialTheme.spacing.screenPadding,
                vertical = MaterialTheme.spacing.base
            )
    ) {
        SparrowDetailRow(
            icon = SparrowIcons.Folder,
            title = stringResource(Res.string.feature_attachments_media_and_files),
            showDivider = canLeaveGroup,
            onClick = onMediaAndFiles
        )
        if (canLeaveGroup) {
            SparrowDetailRow(
                icon = SparrowIcons.Leave,
                title = stringResource(Res.string.feature_chats_group_leave),
                iconTint = MaterialTheme.colorScheme.error,
                titleColor = MaterialTheme.colorScheme.error,
                rippleColor = MaterialTheme.colorScheme.errorBright,
                showChevron = false,
                showDivider = false,
                onClick = onLeaveGroup
            )
        }
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

    SparrowBanner(
        icon = Icons.Default.Security,
        accent = MaterialTheme.colorScheme.primary,
        title = stringResource(Res.string.feature_chats_group_verify_admin_title),
        description =
            stringResource(Res.string.feature_chats_group_verify_admin_description) + "\n" +
                admin.verificationStatusText(),
        actions =
            listOfNotNull(
                SparrowBannerAction(
                    text = stringResource(Res.string.base_verify_contact, adminName),
                    onClick = onVerify
                ).takeIf { admin.canVerify && admin.contactId != null }
            )
    )
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
            title = "Sparrow Team",
            canEditTitle = true,
            isSavingTitle = false,
            isEditingTitle = false,
            titleDraft = "Sparrow Team",
            onTitleDraftChanged = {},
            onEditTitle = {},
            onSaveTitle = {},
            onCancelTitle = {},
            onBack = {}
        )
    }
}

@Preview
@Composable
private fun GroupDetailsScreenPreview() {
    SparrowTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            GroupDetailsScreen(
                uiState = GroupDetailsUiState.Content(GroupDetailsPreviewData.summary),
                onUiEvent = {}
            )
        }
    }
}
