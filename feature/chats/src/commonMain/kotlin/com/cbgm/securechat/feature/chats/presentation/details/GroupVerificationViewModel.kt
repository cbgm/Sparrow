package com.cbgm.securechat.feature.chats.presentation.details

import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.ui.navigation.AppRoute
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.chats.domain.model.group.GroupLeaveRequirement
import com.cbgm.securechat.feature.chats.domain.usecase.group.AddGroupMembersUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.GetGroupLeaveRequirementUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.LeaveGroupUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.ObserveGroupAdministrationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.ObserveGroupVerificationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.PromoteGroupMemberUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.RemoveGroupMemberUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.SynchronizeGroupVerificationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.TransferGroupAdminAndLeaveUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.VerifyGroupMemberUseCase
import com.cbgm.securechat.feature.chats.presentation.details.mapper.buildGroupVerificationSummary
import com.cbgm.securechat.feature.chats.presentation.details.model.AddGroupMembersUiEvent
import com.cbgm.securechat.feature.chats.presentation.details.model.GroupDetailsUiEvent
import com.cbgm.securechat.feature.chats.presentation.details.model.GroupLeavePrompt
import com.cbgm.securechat.feature.chats.presentation.details.model.GroupLeaveUiState
import com.cbgm.securechat.feature.chats.presentation.details.model.GroupMemberManagementUiState
import com.cbgm.securechat.feature.chats.presentation.details.model.GroupMemberVerificationState
import com.cbgm.securechat.feature.chats.presentation.details.model.GroupMemberVerificationUiState
import com.cbgm.securechat.feature.chats.presentation.details.model.GroupVerificationUiState
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContactSafetyNumberUseCase
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContactsUseCase
import com.cbgm.securechat.feature.contacts.presentation.overview.mapper.filterContacts
import com.cbgm.securechat.feature.contacts.presentation.overview.mapper.groupContactsByInitial
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupVerificationViewModel(
    private val conversationId: String,
    observeGroupVerification: ObserveGroupVerificationUseCase,
    private val synchronizeGroupVerification: SynchronizeGroupVerificationUseCase,
    private val verifyGroupMember: VerifyGroupMemberUseCase,
    private val getContactSafetyNumber: GetContactSafetyNumberUseCase,
    observeContacts: ObserveContactsUseCase,
    private val addGroupMembers: AddGroupMembersUseCase,
    private val removeGroupMember: RemoveGroupMemberUseCase,
    private val promoteGroupMember: PromoteGroupMemberUseCase,
    private val transferGroupAdminAndLeave: TransferGroupAdminAndLeaveUseCase,
    observeGroupAdministration: ObserveGroupAdministrationUseCase,
    private val getGroupLeaveRequirement: GetGroupLeaveRequirementUseCase,
    private val leaveGroup: LeaveGroupUseCase
) : BaseViewModel() {
    private val verificationState = MutableStateFlow(GroupVerificationSelectionState())
    private val memberManagementState = MutableStateFlow(GroupMemberManagementState())
    private val leaveState = MutableStateFlow(GroupLeaveUiState())
    private val contactsFlow = observeContacts()
    private val summaryFlow =
        combine(
            observeGroupVerification(conversationId),
            observeGroupAdministration(conversationId)
        ) { groupState, administration ->
            buildGroupVerificationSummary(
                isLocalAdmin = administration.isLocalAdmin || groupState.context.isLocalAdmin,
                isLocalMemberActive = groupState.context.isLocalMemberActive,
                ownerContactId = groupState.context.ownerContactId,
                ownerDisplayName = groupState.ownerDisplayName,
                ownInvitationId = groupState.context.ownInvitationId,
                isLeavePending = groupState.context.isLeavePending,
                rows = groupState.pairs,
                remoteAdminContactIds = administration.adminContactIds,
                currentMemberContactIds = administration.currentMemberContactIds,
                promotableContactIds = administration.promotableContactIds,
                requiresAdminPromotionBeforeLeave = administration.requiresPromotionBeforeLeave
            )
        }

    val uiState: StateFlow<GroupVerificationUiState> =
        combine(
            summaryFlow,
            verificationState,
            contactsFlow,
            memberManagementState,
            leaveState
        ) { summary, verification, contacts, memberManagement, leave ->
            val blockedContactIds =
                summary.currentMemberContactIds.toMutableSet().also { blocked ->
                    summary.members
                        .filterNot(GroupMemberVerificationUiState::isActive)
                        .filter { member -> member.state == GroupMemberVerificationState.INVITATION_PENDING }
                        .mapNotNullTo(blocked, GroupMemberVerificationUiState::contactId)
                }
            val availableContacts =
                contacts.filterNot { contact -> contact.id in blockedContactIds }
            GroupVerificationUiState(
                summary = summary,
                selectedMember =
                    verification.selectedContactId?.let { selectedContactId ->
                        summary.members.firstOrNull { member ->
                            member.contactId == selectedContactId &&
                                member.canVerify
                        }
                    },
                safetyNumber = verification.safetyNumber,
                isLoadingSafetyNumber = verification.isLoadingSafetyNumber,
                isVerifying = verification.isVerifying,
                errorMessage = verification.errorMessage,
                memberManagement =
                    GroupMemberManagementUiState(
                        availableContactGroups =
                            availableContacts
                                .filterContacts(memberManagement.searchQuery)
                                .groupContactsByInitial(),
                        selectedContactIds =
                            memberManagement.selectedContactIds.filterTo(mutableSetOf()) { contactId ->
                                availableContacts.any { contact -> contact.id == contactId }
                            },
                        searchQuery = memberManagement.searchQuery,
                        removalCandidate =
                            memberManagement.removalCandidateContactId?.let { contactId ->
                                summary.members.firstOrNull { member ->
                                    !member.isGroupAdmin && member.contactId == contactId
                                }
                            },
                        promotionCandidate =
                            memberManagement.promotionCandidateContactId?.let { contactId ->
                                summary.members.firstOrNull { member ->
                                    !member.isGroupAdmin && member.contactId == contactId
                                }
                            },
                        promotionRequiredForLeave = summary.requiresAdminPromotionBeforeLeave,
                        isUpdating = memberManagement.isUpdating,
                        errorMessage = memberManagement.errorMessage,
                        completedRevision = memberManagement.completedRevision
                    ),
                leave = leave
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = GroupVerificationUiState()
        )

    init {
        synchronize()
    }

    fun onUiEvent(event: GroupDetailsUiEvent) {
        when (event) {
            GroupDetailsUiEvent.BackClicked -> navigator.popBackStack()
            is GroupDetailsUiEvent.VerifyMemberClicked -> selectMember(event.contactId)
            is GroupDetailsUiEvent.RemoveMemberClicked -> requestMemberRemoval(event.contactId)
            is GroupDetailsUiEvent.PromoteMemberClicked -> requestMemberPromotion(event.contactId)
            is GroupDetailsUiEvent.PromoteMemberAndLeaveClicked -> promoteMemberAndLeave(event.contactId)
            GroupDetailsUiEvent.VerifySelectedMemberClicked -> verifySelectedMember()
            is GroupDetailsUiEvent.ScanMemberQrClicked -> {
                dismissVerification()
                scanMemberQr(event.contactId)
            }
            GroupDetailsUiEvent.VerificationBackClicked -> dismissVerification()
            GroupDetailsUiEvent.MemberRemovalConfirmed -> confirmMemberRemoval()
            GroupDetailsUiEvent.MemberRemovalDismissed -> dismissMemberRemoval()
            GroupDetailsUiEvent.MemberPromotionConfirmed -> confirmMemberPromotion()
            GroupDetailsUiEvent.MemberPromotionDismissed -> dismissMemberPromotion()
            GroupDetailsUiEvent.LeaveGroupConfirmed -> leaveGroup()
            GroupDetailsUiEvent.LeaveGroupDismissed -> dismissLeavePrompt()
            GroupDetailsUiEvent.LeaveGroupClicked -> requestLeave()
            GroupDetailsUiEvent.AddMembersClicked -> Unit
        }
    }

    fun onUiEvent(event: AddGroupMembersUiEvent) {
        when (event) {
            is AddGroupMembersUiEvent.SearchQueryChanged -> updateMemberSearchQuery(event.query)
            is AddGroupMembersUiEvent.ContactSelected -> toggleMemberSelection(event.contactId)
            AddGroupMembersUiEvent.AddMembersClicked -> addSelectedMembers()
            AddGroupMembersUiEvent.BackClicked -> Unit
        }
    }

    private fun scanMemberQr(contactId: String) {
        navigator.navigateTo(
            AppRoute.VerifyIdentityQr(
                contactId = contactId,
                groupId = conversationId
            )
        )
    }

    private fun synchronize() {
        viewModelScope.launch {
            synchronizeGroupVerification(conversationId)
                .onFailure { error ->
                    verificationState.update { state ->
                        state.copy(
                            errorMessage =
                                error.message
                                    ?: "Group verification state could not be synchronized"
                        )
                    }
                }
        }
    }

    private fun selectMember(contactId: String) {
        val canVerify =
            uiState.value.summary.members.any { candidate ->
                candidate.contactId == contactId &&
                    candidate.canVerify
            }
        if (!canVerify || verificationState.value.isLoadingSafetyNumber) {
            return
        }

        verificationState.value =
            GroupVerificationSelectionState(
                selectedContactId = contactId,
                isLoadingSafetyNumber = true
            )

        viewModelScope.launch {
            getContactSafetyNumber
                .invoke(contactId = contactId)
                .onSuccess { safetyNumber ->
                    verificationState.update { current ->
                        if (current.selectedContactId != contactId) {
                            current
                        } else {
                            current.copy(
                                safetyNumber = safetyNumber.singleLine,
                                isLoadingSafetyNumber = false
                            )
                        }
                    }
                }.onFailure { error ->
                    verificationState.update { current ->
                        if (current.selectedContactId != contactId) {
                            current
                        } else {
                            current.copy(
                                safetyNumber = "",
                                isLoadingSafetyNumber = false,
                                errorMessage =
                                    error.message
                                        ?: "Safety number could not be generated"
                            )
                        }
                    }
                }
        }
    }

    private fun verifySelectedMember() {
        val current = verificationState.value
        val contactId = current.selectedContactId ?: return

        if (
            current.safetyNumber.isBlank() ||
            current.isLoadingSafetyNumber ||
            current.isVerifying
        ) {
            return
        }

        verificationState.update { state ->
            state.copy(
                isVerifying = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            verifyGroupMember(
                groupId = conversationId,
                contactId = contactId
            ).onSuccess {
                verificationState.value = GroupVerificationSelectionState()
            }.onFailure { error ->
                verificationState.update { state ->
                    state.copy(
                        isVerifying = false,
                        errorMessage =
                            error.message
                                ?: "Group member could not be verified"
                    )
                }
            }
        }
    }

    private fun dismissVerification() {
        if (!verificationState.value.isVerifying) {
            verificationState.value = GroupVerificationSelectionState()
        }
    }

    private fun updateMemberSearchQuery(query: String) {
        memberManagementState.update { state ->
            state.copy(searchQuery = query, errorMessage = null)
        }
    }

    private fun toggleMemberSelection(contactId: String) {
        if (memberManagementState.value.isUpdating) {
            return
        }
        memberManagementState.update { state ->
            val selected =
                state.selectedContactIds.toMutableSet().apply {
                    if (!add(contactId)) {
                        remove(contactId)
                    }
                }
            state.copy(
                selectedContactIds = selected,
                errorMessage = null
            )
        }
    }

    private fun addSelectedMembers() {
        val selectedContactIds = memberManagementState.value.selectedContactIds
        if (selectedContactIds.isEmpty() || memberManagementState.value.isUpdating) {
            return
        }

        memberManagementState.update { state ->
            state.copy(isUpdating = true, errorMessage = null)
        }
        viewModelScope.launch {
            addGroupMembers(conversationId, selectedContactIds)
                .onSuccess {
                    memberManagementState.update { state ->
                        state.copy(
                            selectedContactIds = emptySet(),
                            searchQuery = "",
                            isUpdating = false,
                            completedRevision = state.completedRevision + 1
                        )
                    }
                }.onFailure { error ->
                    memberManagementState.update { state ->
                        state.copy(
                            isUpdating = false,
                            errorMessage = error.message ?: "Group members could not be added"
                        )
                    }
                }
        }
    }

    private fun requestMemberRemoval(contactId: String) {
        if (!uiState.value.summary.isLocalAdmin || memberManagementState.value.isUpdating) {
            return
        }
        memberManagementState.update { state ->
            state.copy(
                removalCandidateContactId = contactId,
                errorMessage = null
            )
        }
    }

    private fun dismissMemberRemoval() {
        if (!memberManagementState.value.isUpdating) {
            memberManagementState.update { state ->
                state.copy(
                    removalCandidateContactId = null,
                    errorMessage = null
                )
            }
        }
    }

    private fun confirmMemberRemoval() {
        val contactId = memberManagementState.value.removalCandidateContactId ?: return
        if (memberManagementState.value.isUpdating) {
            return
        }

        memberManagementState.update { state ->
            state.copy(isUpdating = true, errorMessage = null)
        }
        viewModelScope.launch {
            removeGroupMember(conversationId, contactId)
                .onSuccess {
                    memberManagementState.update { state ->
                        state.copy(
                            removalCandidateContactId = null,
                            isUpdating = false,
                            completedRevision = state.completedRevision + 1
                        )
                    }
                }.onFailure { error ->
                    memberManagementState.update { state ->
                        state.copy(
                            isUpdating = false,
                            errorMessage = error.message ?: "Group member could not be removed"
                        )
                    }
                }
        }
    }

    private fun requestMemberPromotion(contactId: String) {
        val summary = uiState.value.summary
        if (!summary.isLocalAdmin || contactId !in summary.promotableContactIds || memberManagementState.value.isUpdating) {
            return
        }
        memberManagementState.update { state ->
            state.copy(promotionCandidateContactId = contactId, errorMessage = null)
        }
    }

    private fun dismissMemberPromotion() {
        if (!memberManagementState.value.isUpdating) {
            memberManagementState.update { state ->
                state.copy(promotionCandidateContactId = null, errorMessage = null)
            }
        }
    }

    private fun confirmMemberPromotion() {
        val contactId = memberManagementState.value.promotionCandidateContactId ?: return
        if (memberManagementState.value.isUpdating) return
        memberManagementState.update { state -> state.copy(isUpdating = true, errorMessage = null) }
        viewModelScope.launch {
            promoteGroupMember(conversationId, contactId)
                .onSuccess {
                    memberManagementState.update { state ->
                        state.copy(
                            promotionCandidateContactId = null,
                            isUpdating = false,
                            completedRevision = state.completedRevision + 1
                        )
                    }
                }.onFailure { error ->
                    memberManagementState.update { state ->
                        state.copy(isUpdating = false, errorMessage = error.message ?: "Group member could not be promoted")
                    }
                }
        }
    }

    private fun promoteMemberAndLeave(contactId: String) {
        if (memberManagementState.value.isUpdating || leaveState.value.isLeaving) return
        memberManagementState.update { state -> state.copy(isUpdating = true, errorMessage = null) }
        leaveState.update { state -> state.copy(isLeaving = true, errorMessage = null) }
        viewModelScope.launch {
            transferGroupAdminAndLeave(conversationId, contactId)
                .onSuccess {
                    memberManagementState.update { state -> state.copy(isUpdating = false) }
                    leaveState.value = GroupLeaveUiState(isLeaveRequested = true)
                    navigator.popBackStackTo(AppRoute.Main)
                }.onFailure { error ->
                    leaveState.update { state ->
                        state.copy(
                            prompt = GroupLeavePrompt.PROMOTE_ADMIN,
                            isLeaving = false
                        )
                    }
                    memberManagementState.update { state ->
                        state.copy(isUpdating = false, errorMessage = error.message ?: "Group member could not be promoted")
                    }
                }
        }
    }

    private fun requestLeave() {
        if (leaveState.value.isLeaving || leaveState.value.isLeaveRequested) return

        viewModelScope.launch {
            getGroupLeaveRequirement(conversationId)
                .onSuccess { requirement ->
                    val prompt =
                        when (requirement) {
                            GroupLeaveRequirement.CanLeave -> GroupLeavePrompt.CONFIRM
                            is GroupLeaveRequirement.PromoteAdminFirst -> GroupLeavePrompt.PROMOTE_ADMIN
                        }
                    leaveState.value = GroupLeaveUiState(prompt = prompt)
                }.onFailure { error ->
                    leaveState.value =
                        GroupLeaveUiState(
                            errorMessage = error.message ?: "The group leave state could not be loaded"
                        )
                }
        }
    }

    private fun leaveGroup() {
        if (
            leaveState.value.isLeaving ||
            leaveState.value.isLeaveRequested
        ) {
            return
        }

        leaveState.update { state ->
            state.copy(
                prompt = GroupLeavePrompt.CONFIRM,
                isLeaving = true,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            leaveGroup(conversationId)
                .onSuccess {
                    leaveState.value = GroupLeaveUiState(isLeaveRequested = true)
                    navigator.popBackStackTo(AppRoute.Main)
                }.onFailure { error ->
                    leaveState.value =
                        GroupLeaveUiState(
                            prompt = GroupLeavePrompt.CONFIRM,
                            errorMessage = error.message ?: "The group could not be left"
                        )
                }
        }
    }

    private fun dismissLeavePrompt() {
        if (!leaveState.value.isLeaving) {
            leaveState.value = GroupLeaveUiState()
        }
    }

    private data class GroupVerificationSelectionState(
        val selectedContactId: String? = null,
        val safetyNumber: String = "",
        val isLoadingSafetyNumber: Boolean = false,
        val isVerifying: Boolean = false,
        val errorMessage: String? = null
    )

    private data class GroupMemberManagementState(
        val selectedContactIds: Set<String> = emptySet(),
        val searchQuery: String = "",
        val removalCandidateContactId: String? = null,
        val promotionCandidateContactId: String? = null,
        val isUpdating: Boolean = false,
        val errorMessage: String? = null,
        val completedRevision: Int = 0
    )
}
