package com.cbgm.sparrow.feature.chats.presentation.details

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.core.ui.component.IdentityVerificationScreen
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowDialogListItem
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.chats.presentation.details.model.AddGroupMembersUiEvent
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupDetailsUiEvent
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupDetailsUiState
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupLeavePrompt
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupMemberVerificationUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_chats_group_leave
import com.cbgm.sparrow.resources.feature_chats_group_leave_description
import com.cbgm.sparrow.resources.feature_chats_group_promote_admin
import com.cbgm.sparrow.resources.feature_chats_group_promote_admin_description
import com.cbgm.sparrow.resources.feature_chats_group_promote_before_leave
import com.cbgm.sparrow.resources.feature_chats_group_promote_before_leave_description
import com.cbgm.sparrow.resources.feature_chats_group_remove_member
import com.cbgm.sparrow.resources.feature_chats_group_remove_member_description
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private enum class DetailsContent {
    Overview,
    VerifyIdentity,
    AddMembers
}

@Composable
fun GroupDetailsFlow(
    conversationId: String,
    modifier: Modifier = Modifier,
    requestLeave: Boolean = false
) {
    val verificationViewModel =
        koinViewModel<GroupVerificationViewModel> {
            parametersOf(conversationId)
        }
    val uiState by verificationViewModel.uiState.collectAsStateWithLifecycle()
    val avatarViewModel =
        koinViewModel<GroupAvatarViewModel> {
            parametersOf(conversationId)
        }
    val groupAvatarState by avatarViewModel.uiState.collectAsStateWithLifecycle()
    var content by rememberSaveable {
        mutableStateOf(DetailsContent.Overview)
    }
    var observedMembershipRevision by rememberSaveable {
        mutableIntStateOf(uiState.memberManagement.completedRevision)
    }

    var initialLeaveRequestHandled by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(requestLeave, uiState.summary.hasAuthoritativeState) {
        if (requestLeave && uiState.summary.hasAuthoritativeState && !initialLeaveRequestHandled) {
            initialLeaveRequestHandled = true
            verificationViewModel.onUiEvent(GroupDetailsUiEvent.LeaveGroupClicked)
        }
    }

    LaunchedEffect(uiState.memberManagement.completedRevision) {
        val revision = uiState.memberManagement.completedRevision
        if (revision > observedMembershipRevision) {
            observedMembershipRevision = revision
            content = DetailsContent.Overview
        }
    }

    val visibleContent =
        when {
            content == DetailsContent.VerifyIdentity && uiState.selectedMember == null ->
                DetailsContent.Overview
            else -> content
        }

    AnimatedContent(
        targetState = visibleContent,
        modifier = modifier,
        transitionSpec = {
            if (targetState != DetailsContent.Overview) {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) togetherWith
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
            } else {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) togetherWith
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
            }
        }
    ) { target ->
        when (target) {
            DetailsContent.Overview -> {
                GroupDetailsScreen(
                    uiState = GroupDetailsUiState.Content(uiState.summary),
                    groupAvatarState = groupAvatarState,
                    onGroupAvatarEvent = avatarViewModel::onUiEvent,
                    onUiEvent = { event ->
                        handleOverviewUiEvent(
                            event = event,
                            viewModel = verificationViewModel,
                            onContentChanged = { content = it },
                            onLeaveRequested = {
                                verificationViewModel.onUiEvent(GroupDetailsUiEvent.LeaveGroupClicked)
                            }
                        )
                    }
                )
            }

            DetailsContent.VerifyIdentity -> {
                uiState.selectedMember?.let { member ->
                    IdentityVerificationScreen(
                        contactName = member.displayName,
                        safetyNumber = uiState.safetyNumber,
                        isLoadingSafetyNumber = uiState.isLoadingSafetyNumber,
                        isVerifying = uiState.isVerifying,
                        errorMessage = uiState.errorMessage,
                        onConfirm = {
                            verificationViewModel.onUiEvent(GroupDetailsUiEvent.VerifySelectedMemberClicked)
                        },
                        onScanQrCode = {
                            member.contactId?.let { contactId ->
                                verificationViewModel.onUiEvent(
                                    GroupDetailsUiEvent.ScanMemberQrClicked(contactId)
                                )
                            }
                        },
                        onBack = {
                            verificationViewModel.onUiEvent(GroupDetailsUiEvent.VerificationBackClicked)
                            content = DetailsContent.Overview
                        }
                    )
                }
            }

            DetailsContent.AddMembers -> {
                AddGroupMembersScreen(
                    uiState = uiState.memberManagement,
                    onUiEvent = { event ->
                        if (event == AddGroupMembersUiEvent.BackClicked) {
                            content = DetailsContent.Overview
                        } else {
                            verificationViewModel.onUiEvent(event)
                        }
                    }
                )
            }
        }
    }
    uiState.memberManagement.removalCandidate?.let { member ->
        RemoveDialog(
            member = member,
            isRemoving = uiState.memberManagement.isUpdating,
            errorMessage = uiState.memberManagement.errorMessage,
            onApprove = { verificationViewModel.onUiEvent(GroupDetailsUiEvent.MemberRemovalConfirmed) },
            onDismiss = { verificationViewModel.onUiEvent(GroupDetailsUiEvent.MemberRemovalDismissed) }
        )
    }

    uiState.memberManagement.promotionCandidate?.let { member ->
        PromoteDialog(
            member = member,
            isUpdating = uiState.memberManagement.isUpdating,
            errorMessage = uiState.memberManagement.errorMessage,
            onApprove = { verificationViewModel.onUiEvent(GroupDetailsUiEvent.MemberPromotionConfirmed) },
            onDismiss = { verificationViewModel.onUiEvent(GroupDetailsUiEvent.MemberPromotionDismissed) }
        )
    }

    if (uiState.leave.prompt == GroupLeavePrompt.PROMOTE_ADMIN) {
        val promotableMembers =
            uiState.summary.members.filter { member ->
                member.contactId in uiState.summary.promotableContactIds
            }
        PromoteBeforeLeaveDialog(
            members = promotableMembers,
            isUpdating = uiState.memberManagement.isUpdating,
            errorMessage = uiState.memberManagement.errorMessage,
            onSelect = { contactId ->
                verificationViewModel.onUiEvent(
                    GroupDetailsUiEvent.PromoteMemberAndLeaveClicked(contactId)
                )
            },
            onDismiss = { verificationViewModel.onUiEvent(GroupDetailsUiEvent.LeaveGroupDismissed) }
        )
    }

    if (uiState.leave.prompt == GroupLeavePrompt.CONFIRM) {
        LeaveDialog(
            isRemoving = uiState.leave.isLeaving,
            errorMessage = uiState.leave.errorMessage,
            onApprove = { verificationViewModel.onUiEvent(GroupDetailsUiEvent.LeaveGroupConfirmed) },
            onDismiss = {
                verificationViewModel.onUiEvent(GroupDetailsUiEvent.LeaveGroupDismissed)
            }
        )
    }
}

private fun handleOverviewUiEvent(
    event: GroupDetailsUiEvent,
    viewModel: GroupVerificationViewModel,
    onContentChanged: (DetailsContent) -> Unit,
    onLeaveRequested: () -> Unit
) {
    when (event) {
        GroupDetailsUiEvent.AddMembersClicked -> onContentChanged(DetailsContent.AddMembers)
        GroupDetailsUiEvent.LeaveGroupClicked -> onLeaveRequested()
        is GroupDetailsUiEvent.VerifyMemberClicked -> {
            viewModel.onUiEvent(event)
            onContentChanged(DetailsContent.VerifyIdentity)
        }
        else -> viewModel.onUiEvent(event)
    }
}

@Composable
private fun LeaveDialog(
    isRemoving: Boolean,
    errorMessage: String?,
    onApprove: () -> Unit,
    onDismiss: () -> Unit
) {
    SparrowAlertDialog(
        onDismissRequest = {},
        title = stringResource(Res.string.feature_chats_group_leave),
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(Res.string.feature_chats_group_leave_description)
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.medium),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            SparrowApprovalButton(
                onClick = onApprove,
                fillMaxWidth = false,
                content = {
                    if (isRemoving) {
                        CircularProgressIndicator(
                            modifier =
                                Modifier
                                    .size(20.dp)
                                    .padding(2.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = stringResource(Res.string.feature_chats_group_leave))
                    }
                }
            )
        },
        dismissButton = {
            SparrowOutlinedButton(
                onClick = onDismiss,
                text = stringResource(Res.string.base_cancel),
                fillMaxWidth = false
            )
        }
    )
}

@Preview
@Composable
private fun LeaveDialogPreview() {
    SparrowTheme {
        LeaveDialog(
            isRemoving = true,
            errorMessage = null,
            onApprove = {},
            onDismiss = {}
        )
    }
}

@Composable
private fun PromoteBeforeLeaveDialog(
    members: List<GroupMemberVerificationUiState>,
    isUpdating: Boolean,
    errorMessage: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    SparrowAlertDialog(
        onDismissRequest = {},
        title = stringResource(Res.string.feature_chats_group_promote_before_leave),
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.feature_chats_group_promote_before_leave_description))
                members.forEachIndexed { index, member ->
                    val contactId = member.contactId ?: return@forEachIndexed
                    SparrowDialogListItem(
                        text = member.displayName,
                        isEnabled = !isUpdating,
                        onClick = { onSelect(contactId) }
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
            SparrowOutlinedButton(
                onClick = onDismiss,
                text = stringResource(Res.string.base_cancel),
                fillMaxWidth = false
            )
        }
    )
}

@Preview
@Composable
private fun PromoteBeforeLeaveDialogPreview() {
    SparrowTheme {
        PromoteBeforeLeaveDialog(
            members = GroupDetailsPreviewData.summary.members,
            isUpdating = false,
            errorMessage = null,
            onSelect = {},
            onDismiss = {}
        )
    }
}

@Composable
private fun PromoteDialog(
    member: GroupMemberVerificationUiState,
    isUpdating: Boolean,
    errorMessage: String?,
    onApprove: () -> Unit,
    onDismiss: () -> Unit
) {
    SparrowAlertDialog(
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
            SparrowApprovalButton(
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
            SparrowOutlinedButton(
                onClick = onDismiss,
                text = stringResource(Res.string.base_cancel),
                fillMaxWidth = false
            )
        }
    )
}

@Preview
@Composable
private fun PromoteDialogPreview() {
    SparrowTheme {
        PromoteDialog(
            member = GroupDetailsPreviewData.participant,
            isUpdating = false,
            errorMessage = null,
            onApprove = {},
            onDismiss = {}
        )
    }
}

@Composable
private fun RemoveDialog(
    member: GroupMemberVerificationUiState,
    isRemoving: Boolean,
    errorMessage: String?,
    onApprove: () -> Unit,
    onDismiss: () -> Unit
) {
    SparrowAlertDialog(
        onDismissRequest = {},
        title = stringResource(Res.string.feature_chats_group_remove_member),
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text =
                        stringResource(
                            Res.string.feature_chats_group_remove_member_description,
                            member.displayName
                        )
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.medium),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            SparrowApprovalButton(
                onClick = onApprove,
                fillMaxWidth = false,
                content = {
                    if (isRemoving) {
                        CircularProgressIndicator(
                            modifier =
                                Modifier
                                    .size(20.dp)
                                    .padding(2.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = stringResource(Res.string.feature_chats_group_remove_member))
                    }
                }
            )
        },
        dismissButton = {
            SparrowOutlinedButton(
                onClick = onDismiss,
                text = stringResource(Res.string.base_cancel),
                fillMaxWidth = false
            )
        }
    )
}

@Preview
@Composable
private fun RemoveDialogPreview() {
    SparrowTheme {
        RemoveDialog(
            member = GroupDetailsPreviewData.participant,
            isRemoving = true,
            errorMessage = null,
            onApprove = {},
            onDismiss = {}
        )
    }
}
