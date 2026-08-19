package com.cbgm.sparrow.feature.chats.presentation.group.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAdministrationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversation
import com.cbgm.sparrow.feature.chats.domain.usecase.group.AcceptGroupInvitationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.DeclineGroupInvitationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.MarkGroupConversationReadUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupAdministrationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupAvatarUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupMemberTypingUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.RetryGroupMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.SendGroupMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.SetGroupTypingUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.profile.ObserveRemoteProfilePicturesUseCase
import com.cbgm.sparrow.feature.chats.presentation.group.mapper.toGroupUiState
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupUiEvent
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupUiState
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class GroupViewModel(
    savedStateHandle: SavedStateHandle,
    observeConversation: ObserveGroupConversationUseCase,
    observeAdministration: ObserveGroupAdministrationUseCase,
    private val sendMessage: SendGroupMessageUseCase,
    private val markConversationRead: MarkGroupConversationReadUseCase,
    private val retryMessage: RetryGroupMessageUseCase,
    private val acceptInvitation: AcceptGroupInvitationUseCase,
    private val declineInvitation: DeclineGroupInvitationUseCase,
    observeContacts: ObserveContactsUseCase,
    private val observeProfilePictures: ObserveRemoteProfilePicturesUseCase,
    observeGroupAvatar: ObserveGroupAvatarUseCase,
    private val observeMemberTyping: ObserveGroupMemberTypingUseCase,
    private val setGroupTyping: SetGroupTypingUseCase
) : BaseViewModel() {
    private val groupId =
        savedStateHandle.requireRouteArgument<String>(AppRoute.GroupConversation::conversationId.name)
    private val logger = SparrowLog.withTag("GroupViewModel")
    private val messageText = savedStateHandle.getMutableStateFlow(MESSAGE_TEXT_KEY, "")
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

    private val groupAvatarFlow: Flow<ByteArray?> =
        observeGroupAvatar(groupId).map { avatar -> avatar.bytes }

    private val profilePicturesFlow: Flow<Map<String, ByteArray?>> =
        conversationFlow
            .map { observation ->
                observation.conversation
                    ?.messages
                    .orEmpty()
                    .asSequence()
                    .mapNotNull { message -> message.senderContactId }
                    .filter(String::isNotBlank)
                    .toSet()
            }.distinctUntilChanged()
            .flatMapLatest { contactIds -> observeProfilePictures(contactIds) }

    private val presentationContext =
        combine(
            contextFlow,
            contactsFlow,
            profilePicturesFlow,
            groupAvatarFlow
        ) { context, contacts, profilePictures, avatarBytes ->
            GroupPresentationContext(
                context = context,
                contacts = contacts,
                profilePictures = profilePictures,
                avatarBytes = avatarBytes
            )
        }

    val uiState: StateFlow<GroupUiState> =
        combine(
            presentationContext,
            messageText,
            errorMessage,
            typingContactIds
        ) { presentation, text, error, typingIds ->
            toGroupUiState(
                conversation = presentation.context.observation.conversation,
                administration = presentation.context.administration,
                contacts = presentation.contacts,
                profilePictures = presentation.profilePictures,
                avatarBytes = presentation.avatarBytes,
                currentText = text,
                currentError = error,
                observationError = presentation.context.observation.errorMessage,
                isLoading = presentation.context.observation is GroupConversationObservation.Loading,
                typingContactIds = typingIds
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GroupUiState()
        )

    init {
        observeParticipants()
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

    private data class GroupPresentationContext(
        val context: GroupContext,
        val contacts: List<Contact>,
        val profilePictures: Map<String, ByteArray?>,
        val avatarBytes: ByteArray?
    )

    private companion object {
        const val MESSAGE_TEXT_KEY = "messageText"
        const val LOCAL_TYPING_TIMEOUT_MILLISECONDS = 1500
        const val REMOTE_TYPING_TIMEOUT_MILLISECONDS = 3000
    }
}
