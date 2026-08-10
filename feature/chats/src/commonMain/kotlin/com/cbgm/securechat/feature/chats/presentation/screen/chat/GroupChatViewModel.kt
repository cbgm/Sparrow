package com.cbgm.securechat.feature.chats.presentation.screen.chat

import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.core.ui.navigation.AppRoute
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.chats.domain.model.Conversation
import com.cbgm.securechat.feature.chats.domain.model.GroupConversationState
import com.cbgm.securechat.feature.chats.domain.model.GroupMemberInvitationStatus
import com.cbgm.securechat.feature.chats.domain.usecase.AcceptGroupInvitation
import com.cbgm.securechat.feature.chats.domain.usecase.DeclineGroupInvitation
import com.cbgm.securechat.feature.chats.domain.usecase.MarkConversationRead
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveConversation
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveTypingIndicator
import com.cbgm.securechat.feature.chats.domain.usecase.RetryMessage
import com.cbgm.securechat.feature.chats.domain.usecase.SendGroupMessage
import com.cbgm.securechat.feature.chats.domain.usecase.SetTypingIndicator
import com.cbgm.securechat.feature.chats.presentation.model.ChatUiEvent
import com.cbgm.securechat.feature.chats.presentation.model.ChatUiState
import com.cbgm.securechat.feature.chats.presentation.model.GroupMemberProgressUi
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

class GroupChatViewModel(
    private val conversationId: String,
    observeConversation: ObserveConversation,
    private val sendGroupMessage: SendGroupMessage,
    private val markConversationReadUseCase: MarkConversationRead,
    private val retryMessageUseCase: RetryMessage,
    private val acceptGroupInvitation: AcceptGroupInvitation,
    private val declineGroupInvitation: DeclineGroupInvitation,
    observeContacts: ObserveContacts,
    private val observeTypingIndicator: ObserveTypingIndicator,
    private val setTypingIndicator: SetTypingIndicator
) : BaseViewModel() {
    private val logger = SecureChatLog.withTag("GroupChatViewModel")

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

    val uiState: StateFlow<ChatUiState> =
        combine(
            conversationFlow,
            contactsFlow,
            messageText,
            errorMessage,
            typingContactIds
        ) { observation, contacts, currentMessageText, currentError, currentTypingContactIds ->
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
            val groupState = conversation?.groupState ?: GroupConversationState.READY
            val messageInputEnabled =
                conversation != null &&
                    (
                        conversation.isGroupReady ||
                            (
                                !conversation.isIncomingGroupInvitation &&
                                    (
                                        groupState == GroupConversationState.WAITING_FOR_MEMBERS ||
                                            groupState == GroupConversationState.DISTRIBUTING_KEYS
                                    )
                            )
                    )

            ChatUiState(
                contactName = conversation?.contactName.orEmpty(),
                messages = messages.reversed(),
                messageText = currentMessageText,
                isContactTyping = currentTypingContactIds.isNotEmpty(),
                typingDisplayName = typingDisplayName,
                errorMessage = currentError ?: observation.errorMessage,
                isLoadingContact = observation is ConversationObservation.Loading,
                isGroup = true,
                isMessageInputEnabled = messageInputEnabled,
                groupState = groupState,
                groupMemberCount = memberCount,
                groupReadyMemberCount =
                    conversation
                        ?.groupMemberInvitationStates
                        .orEmpty()
                        .count { member -> member.status == GroupMemberInvitationStatus.ACTIVE },
                groupPendingCount = conversation?.pendingParticipantCount ?: 0,
                showGroupInvitationActions = groupState == GroupConversationState.INVITED,
                groupMemberProgress =
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
            initialValue = ChatUiState(isGroup = true)
        )

    init {
        observeParticipants()
    }

    fun onUiEvent(event: ChatUiEvent) {
        when (event) {
            is ChatUiEvent.MessageTextChanged -> onMessageTextChanged(event.text)
            ChatUiEvent.SendClicked -> sendMessage()
            ChatUiEvent.HeaderClicked -> openGroupDetails()
            is ChatUiEvent.RetryMessage -> retryMessage(event.messageId)
            ChatUiEvent.VerifyIdentityClicked -> Unit
            ChatUiEvent.ManualIdentitySetupClicked,
            ChatUiEvent.ShareIdentityClicked,
            ChatUiEvent.ImportIdentityClicked -> Unit
            ChatUiEvent.BackClicked -> navigateBack()
            ChatUiEvent.AcceptGroupInvitation -> acceptInvitation()
            ChatUiEvent.DeclineGroupInvitation -> declineInvitation()
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

    private fun Contact?.displayNameForChat(isInContacts: Boolean): String {
        if (this == null) return "Unknown contact"

        return if (isInContacts) {
            displayName?.takeIf(String::isNotBlank)
                ?: preferredPhoneNumber?.value
                ?: "Unknown contact"
        } else {
            preferredPhoneNumber?.value
                ?: displayName?.takeIf(String::isNotBlank)
                ?: "Unknown contact"
        }
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

    private companion object {
        const val LOCAL_TYPING_TIMEOUT_MILLISECONDS = 1500
        const val REMOTE_TYPING_TIMEOUT_MILLISECONDS = 3000
    }
}
