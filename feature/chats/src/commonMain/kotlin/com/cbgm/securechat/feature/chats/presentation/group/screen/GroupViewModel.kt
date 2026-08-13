package com.cbgm.securechat.feature.chats.presentation.group.screen

import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.core.ui.navigation.AppRoute
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.chats.domain.model.group.GroupAdministrationState
import com.cbgm.securechat.feature.chats.domain.model.group.GroupConversation
import com.cbgm.securechat.feature.chats.domain.model.group.GroupConversationState
import com.cbgm.securechat.feature.chats.domain.usecase.group.AcceptGroupInvitationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.DeclineGroupInvitationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.MarkGroupConversationReadUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.ObserveGroupAdministrationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.ObserveGroupConversationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.ObserveGroupMemberTypingUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.RefreshGroupDeliveryStateUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.RetryGroupMessageUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.SendGroupMessageUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.group.SetGroupTypingUseCase
import com.cbgm.securechat.feature.chats.presentation.group.mapper.displayNameForChat
import com.cbgm.securechat.feature.chats.presentation.group.mapper.toUiModel
import com.cbgm.securechat.feature.chats.presentation.group.model.GroupMemberProgressUi
import com.cbgm.securechat.feature.chats.presentation.group.model.GroupUiEvent
import com.cbgm.securechat.feature.chats.presentation.group.model.GroupUiState
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContactsUseCase
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
import kotlin.time.Duration.Companion.milliseconds

class GroupViewModel(
    private val groupId: String,
    observeConversation: ObserveGroupConversationUseCase,
    observeAdministration: ObserveGroupAdministrationUseCase,
    private val sendMessage: SendGroupMessageUseCase,
    private val markConversationRead: MarkGroupConversationReadUseCase,
    private val retryMessage: RetryGroupMessageUseCase,
    private val refreshDeliveryState: RefreshGroupDeliveryStateUseCase,
    private val acceptInvitation: AcceptGroupInvitationUseCase,
    private val declineInvitation: DeclineGroupInvitationUseCase,
    observeContacts: ObserveContactsUseCase,
    private val observeMemberTyping: ObserveGroupMemberTypingUseCase,
    private val setGroupTyping: SetGroupTypingUseCase
) : BaseViewModel() {
    private val logger = SecureChatLog.withTag("GroupViewModel")
    private val messageText = MutableStateFlow("")
    private val errorMessage = MutableStateFlow<String?>(null)
    private val typingContactIds = MutableStateFlow<Set<String>>(emptySet())
    private val typingObserverJobs = mutableMapOf<String, Job>()
    private val typingTimeoutJobs = mutableMapOf<String, Job>()
    private var localTypingStopJob: Job? = null
    private var isLocalTyping = false

    private val conversationFlow: Flow<GroupConversationObservation> =
        observeConversation(groupId)
            .map<GroupConversation?, GroupConversationObservation> { GroupConversationObservation.Loaded(it) }
            .onStart { emit(GroupConversationObservation.Loading) }
            .catch { error ->
                emit(GroupConversationObservation.Failed(error.message ?: "Group conversation could not be loaded"))
            }

    private val contactsFlow: Flow<List<Contact>> =
        observeContacts()
            .onStart { emit(emptyList()) }
            .catch { emit(emptyList()) }

    private val contextFlow =
        combine(
            conversationFlow,
            observeAdministration(groupId).onStart { emit(GroupAdministrationState()) }
        ) { observation, administration ->
            GroupContext(observation, administration)
        }

    val uiState: StateFlow<GroupUiState> =
        combine(
            contextFlow,
            contactsFlow,
            messageText,
            errorMessage,
            typingContactIds
        ) { context, contacts, text, error, typingIds ->
            context.toUiState(contacts, text, error, typingIds)
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
            GroupUiEvent.SendClicked -> sendCurrentMessage()
            GroupUiEvent.HeaderClicked -> navigator.navigateTo(AppRoute.GroupDetails(groupId))
            is GroupUiEvent.RetryMessage -> retryFailedMessage(event.messageId)
            GroupUiEvent.BackClicked -> navigator.popBackStackTo(AppRoute.Main)
            GroupUiEvent.AcceptInvitation -> acceptCurrentInvitation()
            GroupUiEvent.DeclineInvitation -> declineCurrentInvitation()
        }
    }

    fun stopTyping() {
        localTypingStopJob?.cancel()
        localTypingStopJob = null
        if (!isLocalTyping) return

        isLocalTyping = false
        sendTypingState(isTyping = false)
    }

    fun markConversationRead() {
        viewModelScope.launch {
            markConversationRead(groupId)
                .onFailure { error -> logger.warn(error) { "Could not mark group conversation as read" } }
        }
    }

    private fun GroupContext.toUiState(
        contacts: List<Contact>,
        currentText: String,
        currentError: String?,
        currentTypingIds: Set<String>
    ): GroupUiState {
        val conversation = observation.conversation
        val contactsById = contacts.associateBy(Contact::id)
        val groupState = resolveGroupState(conversation, administration)

        return GroupUiState(
            title = conversation?.title.orEmpty(),
            messages = conversation.toMessageUiModels(contactsById),
            messageText = currentText,
            isSomeoneTyping = currentTypingIds.isNotEmpty(),
            typingDisplayName = typingDisplayName(currentTypingIds, contactsById),
            errorMessage = currentError ?: observation.errorMessage,
            isLoading = observation is GroupConversationObservation.Loading,
            isMessageInputEnabled = isMessageInputEnabled(conversation, groupState, administration),
            state = groupState,
            memberCount = administration.activeMemberCount + (conversation?.pendingParticipantCount ?: 0),
            readyMemberCount = administration.activeMemberCount,
            pendingMemberCount = conversation?.pendingParticipantCount ?: 0,
            showInvitationActions = groupState == GroupConversationState.INVITED,
            memberProgress = conversation.toMemberProgress(contactsById)
        )
    }

    private fun GroupConversation?.toMessageUiModels(contactsById: Map<String, Contact>) =
        this
            ?.messages
            .orEmpty()
            .asReversed()
            .map { message ->
                val sender = message.senderContactId?.let(contactsById::get)
                val senderIsInContacts = sender?.deviceContactLinkStatus == DeviceContactLinkStatus.LINKED
                message.toUiModel(
                    senderName = sender.displayNameForChat(senderIsInContacts),
                    senderIsInContacts = senderIsInContacts
                )
            }

    private fun GroupConversation?.toMemberProgress(contactsById: Map<String, Contact>): List<GroupMemberProgressUi> =
        this
            ?.memberInvitationStates
            .orEmpty()
            .takeIf { this?.isIncomingInvitation == false }
            .orEmpty()
            .map { member ->
                val contact = contactsById[member.contactId]
                val isInContacts = contact?.deviceContactLinkStatus == DeviceContactLinkStatus.LINKED
                GroupMemberProgressUi(
                    displayName = contact.displayNameForChat(isInContacts),
                    status = member.status
                )
            }

    private fun resolveGroupState(
        conversation: GroupConversation?,
        administration: GroupAdministrationState
    ): GroupConversationState {
        val state = conversation?.state ?: GroupConversationState.READY
        return if (conversation != null && state == GroupConversationState.READY && administration.isOrphaned) {
            GroupConversationState.ORPHANED
        } else {
            state
        }
    }

    private fun isMessageInputEnabled(
        conversation: GroupConversation?,
        state: GroupConversationState,
        administration: GroupAdministrationState
    ): Boolean {
        conversation ?: return false
        val readyForMessaging =
            if (state == GroupConversationState.ORPHANED) {
                administration.currentMemberContactIds.isNotEmpty()
            } else {
                conversation.isReady
            }
        return readyForMessaging ||
            (!conversation.isIncomingInvitation && state.canQueueMessagesWhilePreparing())
    }

    private fun GroupConversationState.canQueueMessagesWhilePreparing(): Boolean =
        this == GroupConversationState.WAITING_FOR_MEMBERS ||
            this == GroupConversationState.DISTRIBUTING_KEYS

    private fun typingDisplayName(
        contactIds: Set<String>,
        contactsById: Map<String, Contact>
    ): String =
        contactIds
            .mapNotNull(contactsById::get)
            .map { contact ->
                val isInContacts = contact.deviceContactLinkStatus == DeviceContactLinkStatus.LINKED
                contact.displayNameForChat(isInContacts)
            }.filter(String::isNotBlank)
            .joinToString(", ")

    private fun observeParticipants() {
        viewModelScope.launch {
            contextFlow
                .map { context -> context.administration.currentMemberContactIds }
                .distinctUntilChanged()
                .collect(::updateTypingObservers)
        }
    }

    private fun updateTypingObservers(contactIds: Set<String>) {
        removeTypingObservers(typingObserverJobs.keys - contactIds)
        (contactIds - typingObserverJobs.keys).forEach(::observeTypingForMember)
    }

    private fun removeTypingObservers(contactIds: Set<String>) {
        contactIds.forEach { contactId ->
            typingObserverJobs.remove(contactId)?.cancel()
            typingTimeoutJobs.remove(contactId)?.cancel()
            typingContactIds.update { it - contactId }
        }
    }

    private fun observeTypingForMember(contactId: String) {
        typingObserverJobs[contactId] =
            viewModelScope.launch {
                observeMemberTyping(groupId, contactId).collect { isTyping ->
                    updateRemoteTyping(contactId, isTyping)
                }
            }
    }

    private fun updateRemoteTyping(contactId: String, isTyping: Boolean) {
        typingTimeoutJobs.remove(contactId)?.cancel()
        typingContactIds.update { current -> if (isTyping) current + contactId else current - contactId }
        if (!isTyping) return

        typingTimeoutJobs[contactId] =
            viewModelScope.launch {
                delay(REMOTE_TYPING_TIMEOUT_MILLISECONDS.milliseconds)
                typingContactIds.update { it - contactId }
            }
    }

    private fun onMessageTextChanged(value: String) {
        messageText.value = value
        errorMessage.value = null
        localTypingStopJob?.cancel()

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

    private fun sendCurrentMessage() {
        if (!uiState.value.isMessageInputEnabled) return
        val text = messageText.value.trim()
        if (text.isEmpty()) return

        messageText.value = ""
        stopTyping()
        viewModelScope.launch {
            sendMessage(groupId, text)
                .onFailure { error ->
                    messageText.value = text
                    errorMessage.value = error.message ?: "Message could not be sent"
                }
        }
    }

    private fun retryFailedMessage(messageId: String) {
        viewModelScope.launch {
            retryMessage(messageId)
                .onFailure { error -> errorMessage.value = error.message ?: "Message could not be queued again" }
        }
    }

    private fun acceptCurrentInvitation() {
        viewModelScope.launch {
            acceptInvitation(groupId)
                .onFailure { error -> errorMessage.value = error.message ?: "Group invitation could not be accepted" }
        }
    }

    private fun declineCurrentInvitation() {
        viewModelScope.launch {
            declineInvitation(groupId)
                .onSuccess { navigator.popBackStackTo(AppRoute.Main) }
                .onFailure { error -> errorMessage.value = error.message ?: "Group invitation could not be declined" }
        }
    }

    private fun observeDeliveryTimeouts() {
        viewModelScope.launch {
            while (true) {
                refreshDeliveryState(groupId)
                delay(DELIVERY_REFRESH_INTERVAL_MILLISECONDS.milliseconds)
            }
        }
    }

    private fun sendTypingState(isTyping: Boolean) {
        viewModelScope.launch { sendTypingStateNow(isTyping) }
    }

    private suspend fun sendTypingStateNow(isTyping: Boolean) {
        setGroupTyping(groupId, isTyping)
            .onFailure { error -> logger.warn(error) { "Could not send group typing state" } }
    }

    private suspend fun stopTypingNow() {
        if (!isLocalTyping) return
        isLocalTyping = false
        sendTypingStateNow(isTyping = false)
    }

    private sealed interface GroupConversationObservation {
        val conversation: GroupConversation?
        val errorMessage: String?

        data object Loading : GroupConversationObservation {
            override val conversation: GroupConversation? = null
            override val errorMessage: String? = null
        }

        data class Loaded(
            override val conversation: GroupConversation?
        ) : GroupConversationObservation {
            override val errorMessage: String? =
                if (conversation == null) "Group conversation was not found" else null
        }

        data class Failed(
            override val errorMessage: String
        ) : GroupConversationObservation {
            override val conversation: GroupConversation? = null
        }
    }

    private data class GroupContext(
        val observation: GroupConversationObservation,
        val administration: GroupAdministrationState
    )

    private companion object {
        const val DELIVERY_REFRESH_INTERVAL_MILLISECONDS = 15_000L
        const val LOCAL_TYPING_TIMEOUT_MILLISECONDS = 1500
        const val REMOTE_TYPING_TIMEOUT_MILLISECONDS = 3000
    }
}
