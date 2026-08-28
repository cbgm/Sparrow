package com.cbgm.sparrow.feature.chats.presentation.group

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.attachments.domain.usecase.LoadMessageAttachmentUseCase
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toOutgoingLocationAttachment
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toOutgoingMessageAttachment
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAdministrationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupChatContext
import com.cbgm.sparrow.feature.chats.domain.usecase.group.AcceptGroupInvitationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.DeclineGroupInvitationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.MarkGroupConversationReadUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupChatContextUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupMemberTypingUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.RetryGroupMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.SendGroupMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.SetGroupTypingUseCase
import com.cbgm.sparrow.feature.chats.presentation.group.mapper.toGroupUiState
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupUiEvent
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupUiState
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.safety.domain.usecase.ObserveMessageSafetyAssessmentsUseCase
import com.cbgm.sparrow.feature.safety.presentation.details.mapper.toDetailsRoute
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
    savedStateHandle: SavedStateHandle,
    observeChatContext: ObserveGroupChatContextUseCase,
    private val sendMessage: SendGroupMessageUseCase,
    private val markConversationRead: MarkGroupConversationReadUseCase,
    private val retryMessage: RetryGroupMessageUseCase,
    private val acceptInvitation: AcceptGroupInvitationUseCase,
    private val declineInvitation: DeclineGroupInvitationUseCase,
    private val observeMemberTyping: ObserveGroupMemberTypingUseCase,
    private val setGroupTyping: SetGroupTypingUseCase,
    observeMessageSafetyAssessments: ObserveMessageSafetyAssessmentsUseCase,
    private val loadMessageAttachment: LoadMessageAttachmentUseCase
) : BaseViewModel() {
    private val groupId =
        savedStateHandle.requireRouteArgument<String>(AppRoute.GroupConversation::conversationId.name)
    private val targetMessageId =
        savedStateHandle.get<String>(AppRoute.GroupConversation::targetMessageId.name)
    private val logger = SparrowLog.withTag("GroupViewModel")
    private val messageText = savedStateHandle.getMutableStateFlow(MESSAGE_TEXT_KEY, "")
    private val errorMessage = MutableStateFlow<String?>(null)
    private val typingContactIds = MutableStateFlow<Set<String>>(emptySet())
    private val selectedMedia = MutableStateFlow<List<MediaSelection>>(emptyList())
    private val attachmentBytes = MutableStateFlow<Map<String, ByteArray>>(emptyMap())
    private val isSending = MutableStateFlow(false)
    private val loadingAttachmentIds = mutableSetOf<String>()
    private val typingObserverJobs = mutableMapOf<String, Job>()
    private val typingTimeoutJobs = mutableMapOf<String, Job>()
    private var localTypingStopJob: Job? = null
    private var isLocalTyping = false

    private val groupContext = observeChatContext(groupId)

    private val presentationContext: Flow<GroupContextObservation> =
        groupContext
            .map<GroupChatContext, GroupContextObservation> { context ->
                GroupContextObservation.Loaded(context)
            }.onStart { emit(GroupContextObservation.Loading) }
            .catch { error ->
                emit(GroupContextObservation.Failed(error.message ?: "Group conversation could not be loaded"))
            }

    private val composerContext =
        combine(
            messageText,
            errorMessage,
            typingContactIds,
            selectedMedia,
            isSending
        ) { text, error, typingIds, media, sending ->
            GroupComposerContext(text, error, typingIds, media, sending)
        }

    val uiState: StateFlow<GroupUiState> =
        combine(
            presentationContext,
            composerContext,
            attachmentBytes,
            observeMessageSafetyAssessments()
        ) { presentation, composer, loadedAttachmentBytes, safetyAssessments ->
            toGroupUiState(
                conversation = presentation.context?.conversation,
                administration = presentation.context?.administration ?: GroupAdministrationState(),
                contacts = presentation.context?.contacts.orEmpty(),
                profilePictures = presentation.context?.profilePictures.orEmpty(),
                avatarBytes = presentation.context?.avatarBytes,
                currentText = composer.text,
                currentError = composer.error,
                observationError = presentation.errorMessage,
                isLoading = presentation is GroupContextObservation.Loading,
                typingContactIds = composer.typingIds,
                safetyAssessments = safetyAssessments,
                attachmentBytes = loadedAttachmentBytes
            ).copy(
                selectedMedia = composer.media,
                isSending = composer.isSending
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
            is GroupUiEvent.MediaSelected -> updateMediaSelection(event.media)
            is GroupUiEvent.OpenFilePicker -> navigator.navigateTo(AppRoute.FilePicker(event.sessionId))
            is GroupUiEvent.ShareCurrentLocation -> sendCurrentLocation(event.location.toOutgoingLocationAttachment())
            is GroupUiEvent.AttachmentVisible -> loadAttachment(event.attachmentId)
            is GroupUiEvent.AttachmentError -> errorMessage.value = event.message
            GroupUiEvent.HeaderClicked -> navigator.navigateTo(AppRoute.GroupDetails(groupId))
            is GroupUiEvent.RetryMessage -> retryFailedMessage(event.messageId)
            is GroupUiEvent.SafetyWarningClicked ->
                navigator.navigateTo(event.warning.toDetailsRoute(event.messageId, event.contactId))
            GroupUiEvent.BackClicked ->
                if (targetMessageId != null) {
                    navigator.popBackStack()
                } else {
                    navigator.popBackStackTo(AppRoute.Main)
                }
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
            groupContext
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
        if (!uiState.value.isMessageInputEnabled || isSending.value) return
        val text = messageText.value.trim()
        val selections = selectedMedia.value
        if (text.isEmpty() && selections.isEmpty()) return

        viewModelScope.launch {
            isSending.value = true
            try {
                val attachments = selections.map(MediaSelection::toOutgoingMessageAttachment)
                sendMessage(groupId, text, attachments)
                    .onSuccess {
                        messageText.value = ""
                        selectedMedia.value = emptyList()
                        stopTypingNow()
                    }
                    .onFailure { error ->
                        errorMessage.value = error.message ?: "Message could not be sent"
                    }
            } finally {
                isSending.value = false
            }
        }
    }

    private fun sendCurrentLocation(locationAttachment: OutgoingMessageAttachment) {
        if (!uiState.value.isMessageInputEnabled || isSending.value) return

        errorMessage.value = null
        viewModelScope.launch {
            isSending.value = true
            try {
                sendMessage(groupId, "", listOf(locationAttachment))
                    .onFailure { error ->
                        errorMessage.value = error.message ?: "Location could not be sent"
                    }
            } finally {
                isSending.value = false
            }
        }
    }

    private fun updateMediaSelection(media: List<MediaSelection>) {
        runCatching {
            MessageAttachmentPolicy.requireValid(
                media.map(MediaSelection::toOutgoingMessageAttachment)
            )
        }.onSuccess {
            selectedMedia.value = media
            errorMessage.value = null
        }.onFailure { error ->
            errorMessage.value = error.message ?: "Selected attachments could not be attached"
        }
    }

    private fun loadAttachment(attachmentId: String) {
        if (attachmentId.isBlank() || attachmentBytes.value.containsKey(attachmentId)) return
        if (!loadingAttachmentIds.add(attachmentId)) return

        viewModelScope.launch {
            loadMessageAttachment(attachmentId)
                .onSuccess { bytes -> attachmentBytes.value = attachmentBytes.value + (attachmentId to bytes) }
                .onFailure { error -> logger.warn(error) { "Could not load message attachment $attachmentId" } }
            loadingAttachmentIds.remove(attachmentId)
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

    private sealed interface GroupContextObservation {
        val context: GroupChatContext?
        val errorMessage: String?

        data object Loading : GroupContextObservation {
            override val context: GroupChatContext? = null
            override val errorMessage: String? = null
        }

        data class Loaded(
            override val context: GroupChatContext
        ) : GroupContextObservation {
            override val errorMessage: String? =
                context.conversationError?.message
                    ?: if (context.conversation == null) "Group conversation was not found" else null
        }

        data class Failed(
            override val errorMessage: String
        ) : GroupContextObservation {
            override val context: GroupChatContext? = null
        }
    }

    private data class GroupComposerContext(
        val text: String,
        val error: String?,
        val typingIds: Set<String>,
        val media: List<MediaSelection>,
        val isSending: Boolean
    )

    private companion object {
        const val MESSAGE_TEXT_KEY = "messageText"
        const val LOCAL_TYPING_TIMEOUT_MILLISECONDS = 1500
        const val REMOTE_TYPING_TIMEOUT_MILLISECONDS = 3000
    }
}
