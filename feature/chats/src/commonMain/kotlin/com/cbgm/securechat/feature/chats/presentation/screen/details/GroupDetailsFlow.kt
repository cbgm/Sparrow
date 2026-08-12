package com.cbgm.securechat.feature.chats.presentation.screen.details

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.core.ui.component.IdentityVerificationScreen
import com.cbgm.securechat.feature.chats.presentation.model.AddGroupMembersUiEvent
import com.cbgm.securechat.feature.chats.presentation.model.GroupDetailsUiEvent
import com.cbgm.securechat.feature.chats.presentation.model.GroupDetailsUiState
import com.cbgm.securechat.feature.chats.presentation.screen.details.component.LeaveGroupDialog
import com.cbgm.securechat.feature.chats.presentation.screen.details.component.PromoteAdminBeforeLeaveDialog
import com.cbgm.securechat.feature.chats.presentation.screen.details.component.PromoteMemberDialog
import com.cbgm.securechat.feature.chats.presentation.screen.details.component.RemoveMemberDialog
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
    requestLeave: Boolean = false,
    modifier: Modifier = Modifier
) {
    val verificationViewModel =
        koinViewModel<GroupVerificationViewModel> {
            parametersOf(conversationId)
        }
    val uiState by verificationViewModel.uiState.collectAsStateWithLifecycle()
    var content by rememberSaveable {
        mutableStateOf(DetailsContent.Overview)
    }
    var observedMembershipRevision by rememberSaveable {
        mutableIntStateOf(uiState.memberManagement.completedRevision)
    }

    var showLeaveDialog by remember { mutableStateOf(false) }
    var showPromoteBeforeLeaveDialog by remember { mutableStateOf(false) }

    var initialLeaveRequestHandled by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(requestLeave, uiState.summary.hasAuthoritativeState) {
        if (requestLeave && uiState.summary.hasAuthoritativeState && !initialLeaveRequestHandled) {
            initialLeaveRequestHandled = true
            if (uiState.summary.requiresAdminPromotionBeforeLeave) {
                showPromoteBeforeLeaveDialog = true
            } else {
                showLeaveDialog = true
            }
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
                    onUiEvent = { event ->
                        handleOverviewUiEvent(
                            event = event,
                            viewModel = verificationViewModel,
                            onContentChanged = { content = it },
                            onLeaveRequested = {
                                if (uiState.summary.requiresAdminPromotionBeforeLeave) {
                                    showPromoteBeforeLeaveDialog = true
                                } else {
                                    showLeaveDialog = true
                                }
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
        RemoveMemberDialog(
            member = member,
            isRemoving = uiState.memberManagement.isUpdating,
            errorMessage = uiState.memberManagement.errorMessage,
            onApprove = { verificationViewModel.onUiEvent(GroupDetailsUiEvent.MemberRemovalConfirmed) },
            onDismiss = { verificationViewModel.onUiEvent(GroupDetailsUiEvent.MemberRemovalDismissed) }
        )
    }

    uiState.memberManagement.promotionCandidate?.let { member ->
        PromoteMemberDialog(
            member = member,
            isUpdating = uiState.memberManagement.isUpdating,
            errorMessage = uiState.memberManagement.errorMessage,
            onApprove = { verificationViewModel.onUiEvent(GroupDetailsUiEvent.MemberPromotionConfirmed) },
            onDismiss = { verificationViewModel.onUiEvent(GroupDetailsUiEvent.MemberPromotionDismissed) }
        )
    }

    if (showPromoteBeforeLeaveDialog) {
        val promotableMembers =
            uiState.summary.members.filter { member ->
                member.contactId in uiState.summary.promotableContactIds
            }
        PromoteAdminBeforeLeaveDialog(
            members = promotableMembers,
            isUpdating = uiState.memberManagement.isUpdating,
            errorMessage = uiState.memberManagement.errorMessage,
            onSelect = { contactId ->
                verificationViewModel.onUiEvent(
                    GroupDetailsUiEvent.PromoteMemberAndLeaveClicked(contactId)
                )
            },
            onDismiss = { showPromoteBeforeLeaveDialog = false }
        )
    }

    if (showLeaveDialog) {
        LeaveGroupDialog(
            isRemoving = uiState.memberManagement.isUpdating,
            errorMessage = uiState.memberManagement.errorMessage,
            onApprove = { verificationViewModel.onUiEvent(GroupDetailsUiEvent.LeaveGroupConfirmed) },
            onDismiss = {
                verificationViewModel.onUiEvent(GroupDetailsUiEvent.LeaveGroupDismissed)
                showLeaveDialog = false
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
