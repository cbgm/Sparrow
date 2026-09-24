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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.component.SparrowSwipeRevealItem
import com.cbgm.sparrow.core.ui.component.SwipeRevealAction
import com.cbgm.sparrow.core.ui.helper.BorderSides
import com.cbgm.sparrow.core.ui.helper.drawShapeBorder
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.avatar.presentation.component.SparrowAvatar
import com.cbgm.sparrow.feature.avatar.presentation.editor.AvatarEditor
import com.cbgm.sparrow.feature.avatar.presentation.editor.AvatarEditorStrings
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupDescription
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
        CenterAlignedTopAppBar(
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    scrolledContainerColor = containerColor,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
            title = {
                if (isEditingTitle) {
                    BasicTextField(
                        value = titleDraft,
                        onValueChange = onTitleDraftChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(titleFocusRequester),
                        enabled = !isSavingTitle,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onSaveTitle() }),
                        textStyle =
                            MaterialTheme.typography.titleSmall.copy(
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                } else {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack, enabled = !isSavingTitle) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null
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
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null
                                )
                            }
                        }
                        IconButton(onClick = onCancelTitle, enabled = !isSavingTitle) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null
                            )
                        }
                    }

                    canEditTitle -> {
                        IconButton(onClick = onEditTitle, enabled = !isSavingTitle) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null
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
                size = Dimens.GroupDetailsScreen.avatarSize,
                modifier = Modifier.padding(MaterialTheme.spacing.base)
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
                    Box(modifier = Modifier.size(Dimens.Avatar.editIconSize), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null
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
                BasicTextField(
                    value = draft,
                    onValueChange = { if (it.length <= GroupDescription.MAX_LENGTH) draft = it },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    enabled = !state.isSaving,
                    minLines = 1,
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (draft.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.feature_chats_group_description_placeholder),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                IconButton(
                    onClick = {
                        isEditing = false
                        draft = state.description
                    },
                    enabled = !state.isSaving
                ) {
                    Icon(
                        Icons.Default.Close,
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
                            Icons.Default.Check,
                            contentDescription = stringResource(Res.string.feature_chats_group_description_save),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Text(
                text = "${draft.length}/${GroupDescription.MAX_LENGTH}",
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
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(Res.string.feature_chats_group_description_edit),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
    val statusColor = member.verificationStatusColor()
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
                    backgroundColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    onClick = onPromote
                ) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = promoteDescription,
                        tint = MaterialTheme.colorScheme.onTertiary
                    )
                }
            )
        }
        if (showRemoveAction) {
            add(
                SwipeRevealAction(
                    backgroundColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    onClick = onRemove
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = removeDescription,
                        tint = MaterialTheme.colorScheme.onError
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
                .padding(
                    horizontal = MaterialTheme.spacing.small,
                    vertical = MaterialTheme.spacing.small
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SparrowAvatar(
                name = displayName,
                target = member.contactId?.takeIf(String::isNotBlank)
                    ?.let { AvatarTarget.User(it) },
                size = Dimens.Avatar.defaultSize
            )
            Text(
                text = displayName,
                modifier = Modifier.weight(1f).padding(start = MaterialTheme.spacing.small),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = member.verificationStatusIcon(),
                contentDescription = null,
                tint = statusColor
            )
            /*Text(
                // An admin remains labeled Admin even if their verification is pending.
                // The row itself can still be tapped to verify when showVerifyAction is true.
                text = if (member.isGroupAdmin || member.state == GroupMemberVerificationState.GROUP_ADMIN) {
                    member.verificationStatusText()
                } else if (showVerifyAction) {
                    stringResource(Res.string.base_verify)
                } else {
                    member.verificationStatusText()
                },
                maxLines = 1,
                color = statusColor,
                style = MaterialTheme.typography.labelLarge
            )*/
        }
    }
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth()
                .padding(start = MaterialTheme.spacing.listDividerStart),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Alpha.itemDivider)
        )
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
        MaterialTheme.colorScheme.tertiary
    } else {
        when (state) {
            GroupMemberVerificationState.GROUP_ADMIN ->
                MaterialTheme.colorScheme.tertiary

            GroupMemberVerificationState.MUTUALLY_VERIFIED ->
                MaterialTheme.colorScheme.tertiary

            GroupMemberVerificationState.ADMIN_VERIFIED_PARTICIPANT,
            GroupMemberVerificationState.PARTICIPANT_VERIFIED_ADMIN ->
                MaterialTheme.colorScheme.secondary.copy(alpha = Alpha.GroupDetailsScreen.adminIcon)

            GroupMemberVerificationState.UNVERIFIED,
            GroupMemberVerificationState.UNAVAILABLE ->
                MaterialTheme.colorScheme.onSurfaceVariant

            GroupMemberVerificationState.INVITATION_PENDING ->
                MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

private fun GroupMemberVerificationUiState.verificationStatusIcon(): ImageVector =
    if (isGroupAdmin) {
        Icons.Default.AdminPanelSettings
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
    onPromoteMember: (String) -> Unit,
    modifier: Modifier = Modifier,
    onAddMembers: (() -> Unit)? = null
) {
    // The card itself has the exact Settings border/radius. Do not add any
    // padding around the member rows: swipe actions extend to the card edges.
    SparrowCardNoAnimation(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.09f))
                    .padding(
                        horizontal = MaterialTheme.spacing.small,
                        vertical = MaterialTheme.spacing.base
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stringResource(Res.string.feature_chats_group_details_members)} (${summary.totalMemberCount})",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (onAddMembers != null) {
                    TextButton(onClick = onAddMembers) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Text(
                            text = stringResource(Res.string.feature_chats_group_add_members),
                            modifier = Modifier.padding(start = MaterialTheme.spacing.micro),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Alpha.divider))
            // Only this list scrolls; member count / Add Members remain visible.
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(
                    count = summary.members.size,
                    key = { index -> summary.members[index].contactId ?: "member-$index" }
                ) { index ->
                    val member = summary.members[index]
                    MemberRow(
                        member = member,
                        showVerifyAction = summary.isLocalAdmin && member.canVerify,
                        showRemoveAction = summary.isLocalAdmin && !member.isGroupAdmin && member.contactId != null,
                        showPromoteAction = summary.isLocalAdmin && member.contactId in summary.promotableContactIds,
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
        val topShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        val bottomShape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .drawShapeBorder(
                    shape = topShape,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    strokeWidth = Dimens.Base.borderStrokeWidth,
                    sides = BorderSides(top = true, bottom = false, left = true, right = true)
                ),
            color = MaterialTheme.colorScheme.background,
            shape = topShape
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable(onClick = onMediaAndFiles)
                    .padding(
                        horizontal = MaterialTheme.spacing.small,
                        vertical = MaterialTheme.spacing.small
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.SettingsScreen.primaryIconSize),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(Res.string.feature_attachments_media_and_files),
                    modifier = Modifier.weight(1f).padding(start = MaterialTheme.spacing.small),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.SettingsScreen.secondaryIconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.SettingsScreen.disabledIcon)
                )
            }
        }

        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outlineVariant)

        if (canLeaveGroup) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-1).dp)
                    .drawShapeBorder(
                        shape = bottomShape,
                        color = MaterialTheme.colorScheme.error,
                        strokeWidth = 1.dp,
                        sides = BorderSides(top = false, bottom = true, left = true, right = true)
                    ),
                color = MaterialTheme.colorScheme.background,
                shape = bottomShape // FIXED: Added shape to Surface to prevent corner bleeding
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        // FIXED: Applied bottomShape here so the red tint tint respects the curves
                        .background(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.18f),
                            shape = bottomShape
                        )
                        .clickable(onClick = onLeaveGroup)
                        .padding(
                            horizontal = MaterialTheme.spacing.small,
                            vertical = MaterialTheme.spacing.small
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.SettingsScreen.primaryIconSize),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = stringResource(Res.string.feature_chats_group_leave),
                        modifier = Modifier.weight(1f).padding(start = MaterialTheme.spacing.small),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.small)
    ) {
        Text(
            text = stringResource(Res.string.feature_chats_group_verify_admin_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.small)
        )

        if (admin.canVerify && admin.contactId != null) {
            SparrowApprovalButton(
                onClick = onVerify,
                fillMaxWidth = false,
                text = stringResource(Res.string.base_verify_contact, adminName)
            )
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
