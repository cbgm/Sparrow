package com.cbgm.sparrow.feature.chats.presentation.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupLeaveRequirement
import com.cbgm.sparrow.feature.chats.domain.usecase.group.AddGroupMembersUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.GetGroupLeaveRequirementUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.LeaveGroupUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupDetailsContextUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.PromoteGroupMemberUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.RemoveGroupAvatarUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.RemoveGroupMemberUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.SetGroupAvatarUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.SynchronizeGroupVerificationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.TransferGroupAdminAndLeaveUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.VerifyGroupMemberUseCase
import com.cbgm.sparrow.feature.chats.presentation.details.mapper.buildGroupVerificationSummary
import com.cbgm.sparrow.feature.chats.presentation.details.mapper.toGroupAvatarUiState
import com.cbgm.sparrow.feature.chats.presentation.details.mapper.toGroupVerificationUiState
import com.cbgm.sparrow.feature.chats.presentation.details.model.AddGroupMembersUiEvent
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupAvatarUiState
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupDetailsUiEvent
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupLeavePrompt
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupLeaveUiState
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupVerificationSummaryUiState
import com.cbgm.sparrow.feature.chats.presentation.details.model.GroupVerificationUiState
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetContactSafetyNumberUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactsWithProfilePicturesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupVerificationViewModel(
    private val savedStateHandle: SavedStateHandle,
    observeGroupDetailsContext: ObserveGroupDetailsContextUseCase,
    private val synchronizeGroupVerification: SynchronizeGroupVerificationUseCase,
    private val verifyGroupMember: VerifyGroupMemberUseCase,
    private val getContactSafetyNumber: GetContactSafetyNumberUseCase,
    observeContactsWithProfilePictures: ObserveContactsWithProfilePicturesUseCase,
    private val addGroupMembers: AddGroupMembersUseCase,
    private val removeGroupMember: RemoveGroupMemberUseCase,
    private val promoteGroupMember: PromoteGroupMemberUseCase,
    private val transferGroupAdminAndLeave: TransferGroupAdminAndLeaveUseCase,
    private val setGroupAvatar: SetGroupAvatarUseCase,
    private val removeGroupAvatar: RemoveGroupAvatarUseCase,
    private val getGroupLeaveRequirement: GetGroupLeaveRequirementUseCase,
    private val leaveGroup: LeaveGroupUseCase
) : BaseViewModel() {
    private val conversationId =
        savedStateHandle.requireRouteArgument<String>(AppRoute.GroupDetails::conversationId.name)
    private val restoredVerificationContactId =
        savedStateHandle.get<String>(VERIFICATION_CONTACT_ID_KEY)
    private val verificationState =
        MutableStateFlow(
            GroupVerificationSelectionState(
                selectedContactId = restoredVerificationContactId,
                isLoadingSafetyNumber = restoredVerificationContactId != null
            )
        )
    private val memberManagementState =
        MutableStateFlow(
            GroupMemberManagementState(
                selectedContactIds =
                    savedStateHandle
                        .get<Array<String>>(SELECTED_CONTACT_IDS_KEY)
                        .orEmpty()
                        .toSet(),
                searchQuery = savedStateHandle.get<String>(MEMBER_SEARCH_QUERY_KEY).orEmpty()
            )
        )
    private val leaveState = MutableStateFlow(GroupLeaveUiState())
    private val avatarActionState = MutableStateFlow(GroupAvatarActionState())
    private val contactsWithProfilePictures = observeContactsWithProfilePictures()

    private val groupOverviewFlow =
        combine(
            observeGroupDetailsContext(conversationId),
            avatarActionState
        ) { context, avatarAction ->
            val groupState = context.verification
            val administration = context.administration
            val summary =
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

            GroupOverviewSnapshot(
                summary = summary,
                avatar =
                    toGroupAvatarUiState(
                        title = context.conversation?.title.orEmpty(),
                        avatarBytes = context.avatar.bytes,
                        canEdit = summary.isLocalAdmin,
                        isSaving = avatarAction.isSaving,
                        errorMessage = avatarAction.errorMessage
                    )
            )
        }

    val uiState: StateFlow<GroupVerificationUiState> =
        combine(
            groupOverviewFlow,
            verificationState,
            contactsWithProfilePictures,
            memberManagementState,
            leaveState
        ) { overview, verification, contactsSnapshot, memberManagement, leave ->
            toGroupVerificationUiState(
                summary = overview.summary,
                groupAvatar = overview.avatar,
                contacts = contactsSnapshot.contacts,
                profilePictures = contactsSnapshot.profilePictures,
                selectedContactId = verification.selectedContactId,
                safetyNumber = verification.safetyNumber,
                isLoadingSafetyNumber = verification.isLoadingSafetyNumber,
                isVerifying = verification.isVerifying,
                verificationError = verification.errorMessage,
                selectedContactIds = memberManagement.selectedContactIds,
                searchQuery = memberManagement.searchQuery,
                removalCandidateContactId = memberManagement.removalCandidateContactId,
                promotionCandidateContactId = memberManagement.promotionCandidateContactId,
                isUpdatingMembers = memberManagement.isUpdating,
                memberManagementError = memberManagement.errorMessage,
                completedRevision = memberManagement.completedRevision,
                leave = leave
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = GroupVerificationUiState()
        )

    init {
        persistMemberSelection()
        restoredVerificationContactId?.let(::loadSafetyNumber)
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
            is GroupDetailsUiEvent.AvatarSelected -> saveGroupAvatar(event.bytes)
            GroupDetailsUiEvent.RemoveGroupAvatarClicked -> removeCurrentGroupAvatar()
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

    private fun saveGroupAvatar(bytes: ByteArray) {
        if (avatarActionState.value.isSaving || bytes.isEmpty()) return

        avatarActionState.value = GroupAvatarActionState(isSaving = true)
        viewModelScope.launch {
            setGroupAvatar(conversationId, bytes)
                .onSuccess { avatarActionState.value = GroupAvatarActionState() }
                .onFailure { error ->
                    avatarActionState.value =
                        GroupAvatarActionState(
                            errorMessage = error.message ?: "Group avatar could not be saved"
                        )
                }
        }
    }

    private fun removeCurrentGroupAvatar() {
        if (avatarActionState.value.isSaving) return

        avatarActionState.value = GroupAvatarActionState(isSaving = true)
        viewModelScope.launch {
            removeGroupAvatar(conversationId)
                .onSuccess { avatarActionState.value = GroupAvatarActionState() }
                .onFailure { error ->
                    avatarActionState.value =
                        GroupAvatarActionState(
                            errorMessage = error.message ?: "Group avatar could not be removed"
                        )
                }
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

    private fun persistMemberSelection() {
        viewModelScope.launch {
            memberManagementState.collect { state ->
                savedStateHandle[MEMBER_SEARCH_QUERY_KEY] = state.searchQuery
                savedStateHandle[SELECTED_CONTACT_IDS_KEY] = state.selectedContactIds.toTypedArray()
            }
        }
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

        selectVerificationContact(contactId)
        loadSafetyNumber(contactId)
    }

    private fun selectVerificationContact(contactId: String) {
        savedStateHandle[VERIFICATION_CONTACT_ID_KEY] = contactId
        verificationState.value =
            GroupVerificationSelectionState(
                selectedContactId = contactId,
                isLoadingSafetyNumber = true
            )
    }

    private fun loadSafetyNumber(contactId: String) {
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
                clearVerificationSelection()
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
            clearVerificationSelection()
        }
    }

    private fun clearVerificationSelection() {
        savedStateHandle.remove<String>(VERIFICATION_CONTACT_ID_KEY)
        verificationState.value = GroupVerificationSelectionState()
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

    private data class GroupOverviewSnapshot(
        val summary: GroupVerificationSummaryUiState,
        val avatar: GroupAvatarUiState
    )

    private data class GroupAvatarActionState(
        val isSaving: Boolean = false,
        val errorMessage: String? = null
    )

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

    private companion object {
        const val VERIFICATION_CONTACT_ID_KEY = "verificationContactId"
        const val MEMBER_SEARCH_QUERY_KEY = "memberSearchQuery"
        const val SELECTED_CONTACT_IDS_KEY = "selectedContactIds"
    }
}
