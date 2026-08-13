package com.cbgm.securechat.feature.chats.presentation.group

import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.core.ui.navigation.AppRoute
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.chats.domain.model.Conversation
import com.cbgm.securechat.feature.chats.domain.model.GroupAdministrationState
import com.cbgm.securechat.feature.chats.domain.model.GroupConversationState
import com.cbgm.securechat.feature.chats.domain.model.GroupMemberInvitationStatus
import com.cbgm.securechat.feature.chats.domain.model.GroupVerificationState
import com.cbgm.securechat.feature.chats.domain.usecase.AcceptGroupInvitationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.DeclineGroupInvitationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.MarkConversationReadUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveConversationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveGroupAdministrationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveGroupVerificationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveTypingIndicatorUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.RefreshDeliveryStateUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.RetryMessageUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.SendGroupMessageUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.SetTypingIndicatorUseCase
import com.cbgm.securechat.feature.chats.presentation.group.mapper.displayNameForChat
import com.cbgm.securechat.feature.chats.presentation.group.mapper.toMemberCounts
import com.cbgm.securechat.feature.chats.presentation.group.model.GroupMemberProgressUi
import com.cbgm.securechat.feature.chats.presentation.group.model.GroupUiEvent
import com.cbgm.securechat.feature.chats.presentation.group.model.GroupUiState
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContacts
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlin.time.Duration.Companion.milliseconds

class GroupViewModel(
    private val conversationId: String,
    observeConversation: ObserveConversationUseCase,
    observeGroupAdministration: ObserveGroupAdministrationUseCase,
    observeGroupVerification: ObserveGroupVerificationUseCase,
    private val sendGroupMessage: SendGroupMessageUseCase,
    private val markConversationReadUseCase: MarkConversationReadUseCase,
    private val retryMessageUseCase: RetryMessageUseCase,
    private val refreshDeliveryState: RefreshDeliveryStateUseCase,
    private val acceptGroupInvitation: AcceptGroupInvitationUseCase,
    private val declineGroupInvitation: DeclineGroupInvitationUseCase,
    observeContacts: ObserveContacts,
    private val observeTypingIndicator: ObserveTypingIndicatorUseCase,
    private val setTypingIndicator: SetTypingIndicatorUseCase
) : BaseViewModel() {
    private val logger = SecureChatLog.withTag("GroupViewModel")

    private val messageText = MutableStateFlow("")
    private val errorMessage = MutableStateFlow<String?>(null)
    private val typingContactIds = MutableStateFlow<Set<String>>(emptySet())
    private val participantContactIds = MutableStateFlow<Set<String>>(emptySet())

    private var localTypingStopJob: Job? = null
    private var isLocalTyping = false
    private val typingObserverJobs = mutableMapOf<String, Job>()
    private val remoteTypingTimeoutJobs = mutableMapOf<String, Job>()

    private val conversationFlow: Flow<ConversationObservation> =
        observeConversation(conversationId)
            .map<Conversation?, ConversationObservation> { conversation ->
                ConversationObservation.Loaded(conversation)
            }.onStart { emit(ConversationObservation.Loading) }
            .catch { error ->
                emit(
                    ConversationObservation.Failed(
                        errorMessage = error.message ?: "Group conversation could not be loaded"
                    )
                )
            }
    private val contactsFlow: Flow<List<Contact>> =
        observeContacts()
            .onStart { emit(emptyList()) }
            .catch { emit(emptyList()) }

    private val groupContextFlow =
        combine(
            conversationFlow,
            observeGroupAdministration(conversationId)
                .onStart { emit(GroupAdministrationState()) },
            observeGroupVerification(conversationId)
                .map<GroupVerificationState, GroupVerificationState?> { state -> state }
                .onStart { emit(null) }
                .catch { emit(null) }
        ) { observation, administration, verification ->
            GroupContextState(
                observation = observation,
                administration = administration,
                verification = verification
            )
        }

    val uiState: StateFlow<GroupUiState> =
        combine(
            groupContextFlow,
            contactsFlow,
            messageText,
            errorMessage,
            typingContactIds
        ) { groupContext, contacts, currentMessageText, currentError, currentTypingContactIds ->
            val observation = groupContext.observation
            val administration = groupContext.administration
            val conversation = observation.conversation
            val contactsById = contacts.associateBy { it.id }
            val messages =
                conversation?.messages.orEmpty().map { message ->
                    val sender = message.senderContactId?.let(contactsById::get)
                    val senderIsInContacts =
                        sender?.deviceContactLinkStatus == DeviceContactLinkStatus.LINKED
                    val senderName = sender.displayNameForChat(senderIsInContacts)

                    message.copy(
                        senderName = senderName,
                        senderIsInContacts = senderIsInContacts
                    )
                }
            val typingDisplayName =
                currentTypingContactIds
                    .mapNotNull(contactsById::get)
                    .map { contact ->
                        val isInContacts =
                            contact.deviceContactLinkStatus == DeviceContactLinkStatus.LINKED
                        contact.displayNameForChat(isInContacts)
                    }.filter(String::isNotBlank)
                    .joinToString(", ")
            val memberCount =
                conversation?.let {
                    (
                        it.participantContactIds +
                            it.groupMemberInvitationStates.map { member -> member.contactId }
                    ).distinct().size + 1
                } ?: 0
            val verificationCounts =
                groupContext.verification.toMemberCounts(
                    currentMemberContactIds = administration.currentMemberContactIds
                )
            val baseGroupState = conversation?.groupState ?: GroupConversationState.READY
            val groupState =
                if (conversation != null && baseGroupState == GroupConversationState.READY && administration.isOrphaned) {
                    GroupConversationState.ORPHANED
                } else {
                    baseGroupState
                }
            val readyForMessaging =
                if (groupState == GroupConversationState.ORPHANED) {
                    administration.currentMemberContactIds.isNotEmpty()
                } else {
                    conversation?.isGroupReady == true
                }
            val messageInputEnabled =
                conversation != null &&
                    (
                        readyForMessaging ||
                            (
                                !conversation.isIncomingGroupInvitation &&
                                    (
                                        groupState == GroupConversationState.WAITING_FOR_MEMBERS ||
                                            groupState == GroupConversationState.DISTRIBUTING_KEYS
                                    )
                            )
                    )

            GroupUiState(
                title = conversation?.contactName.orEmpty(),
                messages = messages.reversed(),
                messageText = currentMessageText,
                isSomeoneTyping = currentTypingContactIds.isNotEmpty(),
                typingDisplayName = typingDisplayName,
                errorMessage = currentError ?: observation.errorMessage,
                isLoading = observation is ConversationObservation.Loading,
                isMessageInputEnabled = messageInputEnabled,
                state = groupState,
                memberCount = verificationCounts?.total ?: memberCount,
                readyMemberCount =
                    verificationCounts?.active
                        ?: conversation
                            ?.groupMemberInvitationStates
                            .orEmpty()
                            .count { member -> member.status == GroupMemberInvitationStatus.ACTIVE },
                pendingMemberCount = conversation?.pendingParticipantCount ?: 0,
                showInvitationActions = groupState == GroupConversationState.INVITED,
                memberProgress =
                    conversation
                        ?.groupMemberInvitationStates
                        .orEmpty()
                        .takeIf { conversation?.isIncomingGroupInvitation == false }
                        .orEmpty()
                        .map { member ->
                            val contact = contactsById[member.contactId]
                            val isInContacts =
                                contact?.deviceContactLinkStatus == DeviceContactLinkStatus.LINKED
                            GroupMemberProgressUi(
                                displayName = contact.displayNameForChat(isInContacts),
                                status = member.status
                            )
                        }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GroupUiState()
        )

    init {
        observeParticipants()
        observeDeliveryTimeouts()
    }

    fun onUiEvent(event: GroupUiEvent) {
        when (event) {
            is GroupUiEvent.MessageTextChanged -> onMessageTextChanged(event.text)
            GroupUiEvent.SendClicked -> sendMessage()
            GroupUiEvent.HeaderClicked -> openGroupDetails()
            is GroupUiEvent.RetryMessage -> retryMessage(event.messageId)
            GroupUiEvent.BackClicked -> navigateBack()
            GroupUiEvent.AcceptInvitation -> acceptInvitation()
            GroupUiEvent.DeclineInvitation -> declineInvitation()
        }
    }

    private fun observeDeliveryTimeouts() {
        viewModelScope.launch {
            while (true) {
                refreshDeliveryState(conversationId)
                delay(DELIVERY_REFRESH_INTERVAL_MILLISECONDS.milliseconds)
            }
        }
    }

    private fun navigateBack() {
        navigator.popBackStackTo(AppRoute.Main)
    }

    private fun openGroupDetails() {
        navigator.navigateTo(AppRoute.GroupDetails(conversationId = conversationId))
    }

    private fun onMessageTextChanged(value: String) {
        messageText.value = value
        errorMessage.value = null

        localTypingStopJob?.cancel()
        localTypingStopJob = null

        if (value.isBlank()) {
            stopTyping()
            return
        }

        if (!isLocalTyping) {
            isLocalTyping = true
            sendTypingState(isTyping = true)
        }

        localTypingStopJob =
            viewModelScope.launch {
                delay(LOCAL_TYPING_TIMEOUT_MILLISECONDS.milliseconds)
                stopTypingNow()
            }
    }

    fun stopTyping() {
        localTypingStopJob?.cancel()
        localTypingStopJob = null

        if (!isLocalTyping) return

        isLocalTyping = false
        sendTypingState(isTyping = false)
    }

    private fun sendMessage() {
        if (!uiState.value.isMessageInputEnabled) return

        val text = messageText.value.trim()
        if (text.isEmpty()) return

        messageText.value = ""
        stopTyping()

        viewModelScope.launch {
            sendGroupMessage(conversationId, text).onFailure { error ->
                messageText.value = text
                errorMessage.value = error.message ?: "Message could not be sent"
            }
        }
    }

    private fun retryMessage(messageId: String) {
        viewModelScope.launch {
            retryMessageUseCase(messageId).onFailure { error ->
                errorMessage.value = error.message ?: "Message could not be queued again"
            }
        }
    }

    private fun acceptInvitation() {
        viewModelScope.launch {
            acceptGroupInvitation(conversationId).onFailure { error ->
                errorMessage.value = error.message ?: "Group invitation could not be accepted"
            }
        }
    }

    private fun declineInvitation() {
        viewModelScope.launch {
            declineGroupInvitation(conversationId)
                .onSuccess {
                    navigator.popBackStackTo(AppRoute.Main)
                }.onFailure { error ->
                    errorMessage.value = error.message ?: "Group invitation could not be declined"
                }
        }
    }

    fun markConversationRead() {
        viewModelScope.launch { markConversationReadUseCase(conversationId) }
    }

    private fun observeParticipants() {
        viewModelScope.launch {
            conversationFlow
                .map { observation ->
                    observation.conversation
                        ?.participantContactIds
                        .orEmpty()
                        .toSet()
                }.distinctUntilChanged()
                .collect { contactIds ->
                    participantContactIds.value = contactIds
                    updateTypingObservers(contactIds)
                }
        }
    }

    private fun updateTypingObservers(contactIds: Set<String>) {
        val removedContactIds = typingObserverJobs.keys - contactIds
        removedContactIds.forEach { contactId ->
            typingObserverJobs.remove(contactId)?.cancel()
            remoteTypingTimeoutJobs.remove(contactId)?.cancel()
            typingContactIds.update { it - contactId }
        }

        (contactIds - typingObserverJobs.keys).forEach { contactId ->
            typingObserverJobs[contactId] =
                viewModelScope.launch {
                    observeTypingIndicator(contactId).collect { isTyping ->
                        remoteTypingTimeoutJobs.remove(contactId)?.cancel()
                        typingContactIds.update { current ->
                            if (isTyping) current + contactId else current - contactId
                        }

                        if (isTyping) {
                            remoteTypingTimeoutJobs[contactId] =
                                viewModelScope.launch {
                                    delay(REMOTE_TYPING_TIMEOUT_MILLISECONDS.milliseconds)
                                    typingContactIds.update { it - contactId }
                                }
                        }
                    }
                }
        }
    }

    private fun sendTypingState(isTyping: Boolean) {
        viewModelScope.launch { sendTypingStateNow(isTyping) }
    }

    private suspend fun sendTypingStateNow(isTyping: Boolean) {
        supervisorScope {
            participantContactIds.value.forEach { contactId ->
                launch {
                    setTypingIndicator(contactId, isTyping)
                        .onFailure { error ->
                            logger.warn(error) {
                                "Could not send group typing state for $contactId"
                            }
                        }
                }
            }
        }
    }

    private suspend fun stopTypingNow() {
        if (!isLocalTyping) return

        isLocalTyping = false
        sendTypingStateNow(isTyping = false)
    }

    private sealed interface ConversationObservation {
        val conversation: Conversation?
        val errorMessage: String?

        data object Loading : ConversationObservation {
            override val conversation: Conversation? = null
            override val errorMessage: String? = null
        }

        data class Loaded(
            override val conversation: Conversation?
        ) : ConversationObservation {
            override val errorMessage: String? =
                if (conversation == null) {
                    "Group conversation was not found"
                } else {
                    null
                }
        }

        data class Failed(
            override val errorMessage: String
        ) : ConversationObservation {
            override val conversation: Conversation? = null
        }
    }

    private data class GroupContextState(
        val observation: ConversationObservation,
        val administration: GroupAdministrationState,
        val verification: GroupVerificationState?
    )

    private companion object {
        const val DELIVERY_REFRESH_INTERVAL_MILLISECONDS = 15_000L
        const val LOCAL_TYPING_TIMEOUT_MILLISECONDS = 1500
        const val REMOTE_TYPING_TIMEOUT_MILLISECONDS = 3000
    }
}
