package com.cbgm.sparrow.feature.chats.presentation.group

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.attachments.domain.usecase.LoadMessageAttachmentUseCase
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toOutgoingMessageAttachment
import com.cbgm.sparrow.feature.chats.domain.model.ForwardingTarget
import com.cbgm.sparrow.feature.chats.domain.model.LocationShareEvent
import com.cbgm.sparrow.feature.chats.domain.model.LocationShareState
import com.cbgm.sparrow.feature.chats.domain.model.LocationShareStateMachine
import com.cbgm.sparrow.feature.chats.domain.model.MessageComposerPolicy
import com.cbgm.sparrow.feature.chats.domain.model.group.ChatMessageType
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAdministrationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupChatContext
import com.cbgm.sparrow.feature.chats.domain.usecase.ForwardMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.AcceptGroupInvitationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.DeclineGroupInvitationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.DeleteGroupMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.EditGroupMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.MarkGroupConversationReadUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupChatContextUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupMemberTypingUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.RetryGroupMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.SendGroupMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.SetGroupTypingUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ToggleGroupMessageReactionUseCase
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageComposerUiState
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageContextUiState
import com.cbgm.sparrow.feature.chats.presentation.component.model.TypingUiState
import com.cbgm.sparrow.feature.chats.presentation.group.mapper.toGroupConversationUiState
import com.cbgm.sparrow.feature.chats.presentation.group.mapper.toGroupMembershipUiState
import com.cbgm.sparrow.feature.chats.presentation.group.mapper.toGroupReplyPreview
import com.cbgm.sparrow.feature.chats.presentation.group.mapper.toTypingDisplayName
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupConversationUiEvent
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupConversationUiState
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupMembershipUiState
import com.cbgm.sparrow.feature.contacts.domain.model.device.AddDeviceContactResult
import com.cbgm.sparrow.feature.contacts.domain.usecase.AddDeviceContactUseCase
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.safety.domain.usecase.ObserveMessageSafetyAssessmentsUseCase
import com.cbgm.sparrow.feature.safety.presentation.details.mapper.toMessageSafetyDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GroupConversationViewModel(
    savedStateHandle: SavedStateHandle,
    observeChatContext: ObserveGroupChatContextUseCase,
    private val sendMessage: SendGroupMessageUseCase,
    private val markConversationRead: MarkGroupConversationReadUseCase,
    private val retryMessage: RetryGroupMessageUseCase,
    private val toggleMessageReaction: ToggleGroupMessageReactionUseCase,
    private val deleteMessageUseCase: DeleteGroupMessageUseCase,
    private val editMessageUseCase: EditGroupMessageUseCase,
    private val acceptInvitation: AcceptGroupInvitationUseCase,
    private val declineInvitation: DeclineGroupInvitationUseCase,
    observeMemberTyping: ObserveGroupMemberTypingUseCase,
    setGroupTyping: SetGroupTypingUseCase,
    observeMessageSafetyAssessments: ObserveMessageSafetyAssessmentsUseCase,
    private val loadMessageAttachment: LoadMessageAttachmentUseCase,
    private val addDeviceContact: AddDeviceContactUseCase,
    private val forwardMessageUseCase: ForwardMessageUseCase
) : BaseViewModel() {
    private val groupId =
        savedStateHandle.requireRouteArgument<String>(AppRoute.GroupConversation::conversationId.name)
    private val targetMessageId =
        savedStateHandle.get<String>(AppRoute.GroupConversation::targetMessageId.name)
    private val logger = SparrowLog.withTag("GroupConversationViewModel")
    private val messageText = savedStateHandle.getMutableStateFlow(MESSAGE_TEXT_KEY, "")
    private val replyToMessageId = savedStateHandle.getMutableStateFlow(REPLY_TO_MESSAGE_ID_KEY, "")
    private val editingMessageId = savedStateHandle.getMutableStateFlow(EDITING_MESSAGE_ID_KEY, "")
    private val mutableErrorMessage = MutableStateFlow<String?>(null)
    private val selectedMedia = MutableStateFlow<List<MediaSelection>>(emptyList())
    private val attachmentBytes = MutableStateFlow<Map<String, ByteArray>>(emptyMap())
    private val isSending = MutableStateFlow(false)
    private val contextMessageId = MutableStateFlow<String?>(null)
    private val locationShareState = MutableStateFlow(LocationShareState.IDLE)
    private val loadingAttachmentIds = mutableSetOf<String>()

    private val typing =
        GroupTypingController(
            scope = viewModelScope,
            observeMemberTyping = { contactId -> observeMemberTyping(groupId, contactId) },
            sendTypingState = { isTyping -> setGroupTyping(groupId, isTyping) },
            logTag = "GroupConversationViewModel"
        )

    private val groupContext = observeChatContext(groupId)

    private val presentationContext: Flow<GroupContextObservation> =
        groupContext
            .map<GroupChatContext, GroupContextObservation> { context ->
                GroupContextObservation.Loaded(context)
            }.onStart { emit(GroupContextObservation.Loading) }
            .catch { error ->
                emit(GroupContextObservation.Failed(error.message ?: "Group conversation could not be loaded"))
            }

    private val composerDraft =
        combine(messageText, replyToMessageId, editingMessageId) { text, replyId, editId ->
            GroupComposerDraft(
                text = text,
                replyToMessageId = replyId.takeIf(String::isNotBlank),
                editingMessageId = editId.takeIf(String::isNotBlank)
            )
        }

    private val composerRuntime =
        combine(selectedMedia, isSending, locationShareState) { media, sending, locationState ->
            GroupComposerRuntime(
                media = media,
                isSending = sending,
                locationShareState = locationState
            )
        }

    val conversationState: StateFlow<GroupConversationUiState> =
        combine(
            presentationContext,
            attachmentBytes,
            observeMessageSafetyAssessments()
        ) { presentation, loadedAttachmentBytes, safetyAssessments ->
            toGroupConversationUiState(
                conversation = presentation.context?.conversation,
                contacts = presentation.context?.contacts.orEmpty(),
                profilePictures = presentation.context?.profilePictures.orEmpty(),
                avatarBytes = presentation.context?.avatarBytes,
                isLoading = presentation is GroupContextObservation.Loading,
                safetyAssessments = safetyAssessments,
                attachmentBytes = loadedAttachmentBytes
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GroupConversationUiState()
        )

    val membershipState: StateFlow<GroupMembershipUiState> =
        presentationContext
            .map { presentation ->
                toGroupMembershipUiState(
                    conversation = presentation.context?.conversation,
                    administration = presentation.context?.administration ?: GroupAdministrationState(),
                    contacts = presentation.context?.contacts.orEmpty()
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = GroupMembershipUiState()
            )

    val composerState: StateFlow<MessageComposerUiState> =
        combine(
            conversationState,
            presentationContext,
            composerDraft,
            composerRuntime
        ) { conversation, presentation, draft, runtime ->
            val availability =
                MessageComposerPolicy.resolve(
                    isInputAllowed = !conversation.isLoading && conversation.composerState.isInputEnabled,
                    isSendAllowed = !conversation.isLoading && conversation.composerState.isSendActionEnabled,
                    isSending = runtime.isSending,
                    isEditing = draft.editingMessageId != null,
                    selectedAttachmentCount = runtime.media.size,
                    locationShareState = runtime.locationShareState
                )

            MessageComposerUiState(
                messageText = draft.text,
                replyTo =
                    draft.replyToMessageId.toGroupReplyPreview(
                        conversation = presentation.context?.conversation,
                        contacts = presentation.context?.contacts.orEmpty()
                    ),
                editingMessageId = draft.editingMessageId,
                selectedMedia = runtime.media,
                isSending = runtime.isSending,
                locationShareState = runtime.locationShareState,
                availability = availability
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MessageComposerUiState()
        )

    val contextState: StateFlow<MessageContextUiState<MessageBubbleUi>> =
        combine(conversationState, contextMessageId) { conversation, selectedMessageId ->
            val message = conversation.messages.firstOrNull { it.id == selectedMessageId }
            MessageContextUiState(
                message = message,
                canEdit = message?.canEdit == true
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MessageContextUiState()
        )

    val typingState: StateFlow<TypingUiState> =
        combine(typing.typingContactIds, presentationContext) { typingIds, presentation ->
            TypingUiState(
                isTyping = typingIds.isNotEmpty(),
                displayName = typingIds.toTypingDisplayName(presentation.context?.contacts.orEmpty())
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TypingUiState()
        )

    val errorMessage: StateFlow<String?> =
        combine(mutableErrorMessage, presentationContext) { currentError, presentation ->
            currentError ?: presentation.errorMessage
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    init {
        typing.start(groupContext.map { context -> context.administration.currentMemberContactIds })
    }

    fun onUiEvent(event: GroupConversationUiEvent) {
        when (event) {
            is GroupConversationUiEvent.MessageTextChanged -> onMessageTextChanged(event.text)
            GroupConversationUiEvent.SendClicked -> sendCurrentMessage()
            is GroupConversationUiEvent.ReplyToMessage -> startReply(event.messageId)
            GroupConversationUiEvent.CancelReply -> clearReply()
            is GroupConversationUiEvent.EditMessage -> startEdit(event.messageId)
            is GroupConversationUiEvent.MessageContextRequested -> contextMessageId.value = event.messageId
            GroupConversationUiEvent.MessageContextDismissed -> contextMessageId.value = null
            GroupConversationUiEvent.CancelEdit -> cancelEdit()
            is GroupConversationUiEvent.MessageReactionSelected -> toggleReaction(event.messageId, event.emoji)
            is GroupConversationUiEvent.DeleteMessage -> deleteMessage(event.messageId)
            is GroupConversationUiEvent.ForwardMessage -> forwardMessage(event.messageId, event.target)
            is GroupConversationUiEvent.MediaSelected -> updateMediaSelection(event.media)
            is GroupConversationUiEvent.OpenFilePicker -> navigator.navigateTo(AppRoute.FilePicker(event.sessionId))
            GroupConversationUiEvent.LocationCaptureStarted -> transitionLocationShare(LocationShareEvent.CAPTURE_STARTED)
            is GroupConversationUiEvent.ShareCurrentLocation -> shareCurrentLocation(event.location.toOutgoingMessageAttachment())
            is GroupConversationUiEvent.LocationCaptureFailed -> {
                transitionLocationShare(LocationShareEvent.FAILED)
                setError(event.message)
            }
            is GroupConversationUiEvent.ShareContact ->
                sendAttachmentOnly(
                    attachment = event.contact.toOutgoingMessageAttachment(),
                    fallbackError = "Contact could not be sent"
                )
            is GroupConversationUiEvent.AddSharedContact -> addSharedContact(event.contact)
            is GroupConversationUiEvent.AttachmentVisible -> loadAttachment(event.attachmentId)
            is GroupConversationUiEvent.AttachmentError -> setError(event.message)
            GroupConversationUiEvent.HeaderClicked -> navigator.navigateTo(AppRoute.GroupDetails(groupId))
            is GroupConversationUiEvent.RetryMessage -> retryFailedMessage(event.messageId)
            is GroupConversationUiEvent.SafetyWarningClicked ->
                navigator.navigateTo(event.warning.toMessageSafetyDetails(event.messageId, event.contactId))
            GroupConversationUiEvent.BackClicked ->
                if (targetMessageId != null) {
                    navigator.popBackStack()
                } else {
                    navigator.popBackStackTo(AppRoute.Main)
                }
            GroupConversationUiEvent.AcceptInvitation -> acceptCurrentInvitation()
            GroupConversationUiEvent.DeclineInvitation -> declineCurrentInvitation()
        }
    }

    fun stopTyping() = typing.stopLocalTyping()

    fun markConversationRead() {
        viewModelScope.launch {
            markConversationRead(groupId)
                .onFailure { error -> logger.warn(error) { "Could not mark group conversation as read" } }
        }
    }

    private fun forwardMessage(
        messageId: String,
        target: ForwardingTarget
    ) {
        viewModelScope.launch {
            val message =
                groupContext
                    .first()
                    .conversation
                    ?.messages
                    ?.firstOrNull { message -> message.id == messageId }
                    ?.takeIf { message -> message.type == ChatMessageType.USER }
                    ?: return@launch

            forwardMessageUseCase(
                parts = message.parts,
                target = target
            ).onFailure { error ->
                setError(error.message ?: "Message could not be forwarded")
            }
        }
    }

    private fun onMessageTextChanged(value: String) {
        if (!composerState.value.availability.isInputEnabled) return

        messageText.value = value
        clearError()
        if (conversationState.value.composerState.sendsTypingIndicators) {
            typing.onLocalTextChanged(value)
        }
    }

    private fun sendCurrentMessage() {
        val text = messageText.value.trim()
        val editMessageId = editingMessageId.value.takeIf(String::isNotBlank)
        if (editMessageId != null) {
            if (text.isEmpty()) return
            editCurrentMessage(editMessageId, text)
            return
        }

        val selections = selectedMedia.value
        if (text.isEmpty() && selections.isEmpty()) return

        dispatchSend(
            text = text,
            attachments = selections.map(MediaSelection::toOutgoingMessageAttachment),
            clearComposerOnSuccess = true,
            fallbackError = "Message could not be sent"
        )
    }

    private fun shareCurrentLocation(attachment: OutgoingMessageAttachment) {
        transitionLocationShare(LocationShareEvent.LOCATION_CAPTURED)
        sendAttachmentOnly(
            attachment = attachment,
            fallbackError = "Location could not be sent",
            isLocationShare = true
        )
    }

    private fun sendAttachmentOnly(
        attachment: OutgoingMessageAttachment,
        fallbackError: String,
        isLocationShare: Boolean = false
    ) {
        dispatchSend(
            text = "",
            attachments = listOf(attachment),
            clearComposerOnSuccess = false,
            fallbackError = fallbackError,
            isLocationShare = isLocationShare
        )
    }

    private fun dispatchSend(
        text: String,
        attachments: List<OutgoingMessageAttachment>,
        clearComposerOnSuccess: Boolean,
        fallbackError: String,
        isLocationShare: Boolean = false
    ) {
        val sendAllowed =
            if (isLocationShare) {
                !conversationState.value.isLoading &&
                    conversationState.value.composerState.isSendActionEnabled &&
                    !isSending.value
            } else {
                composerState.value.availability.isSendEnabled
            }

        if (!sendAllowed) {
            if (isLocationShare) transitionLocationShare(LocationShareEvent.FAILED)
            return
        }

        val replyTo = replyToMessageId.value.takeIf(String::isNotBlank)
        clearError()
        viewModelScope.launch {
            if (isLocationShare) transitionLocationShare(LocationShareEvent.SEND_STARTED)
            isSending.value = true
            try {
                sendMessage(groupId, text, attachments, replyTo)
                    .onSuccess { if (clearComposerOnSuccess) clearComposer() else clearReply() }
                    .onFailure { error -> setError(error.message ?: fallbackError) }
            } finally {
                isSending.value = false
                if (isLocationShare) transitionLocationShare(LocationShareEvent.COMPLETED)
            }
        }
    }

    private fun addSharedContact(contact: SharedContact) {
        viewModelScope.launch {
            when (
                val result =
                    addDeviceContact(
                        displayName = contact.displayName,
                        phoneNumber = contact.phoneNumber
                    )
            ) {
                AddDeviceContactResult.Added,
                AddDeviceContactResult.AlreadyExists -> clearError()

                AddDeviceContactResult.PermissionDenied ->
                    setError("Contacts permission is required to add this contact")

                AddDeviceContactResult.InvalidPhoneNumber ->
                    setError("The shared phone number is invalid")

                is AddDeviceContactResult.Failure ->
                    setError(result.throwable.message ?: "Contact could not be added")
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
            clearError()
        }.onFailure { error ->
            setError(error.message ?: "Selected attachments could not be attached")
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

    private suspend fun clearComposer() {
        messageText.value = ""
        replyToMessageId.value = ""
        editingMessageId.value = ""
        selectedMedia.value = emptyList()
        typing.stopLocalTypingNow()
    }

    private fun startReply(messageId: String) {
        if (conversationState.value.messages.none { message -> message.id == messageId }) return
        editingMessageId.value = ""
        replyToMessageId.value = messageId
        clearError()
    }

    private fun clearReply() {
        replyToMessageId.value = ""
    }

    private fun startEdit(messageId: String) {
        val message = conversationState.value.messages.firstOrNull { it.id == messageId } ?: return
        if (!message.canEdit) return

        val text = message.textPart?.text?.takeIf(String::isNotBlank) ?: return

        replyToMessageId.value = ""
        selectedMedia.value = emptyList()
        editingMessageId.value = messageId
        messageText.value = text
        clearError()
    }

    private fun cancelEdit() {
        editingMessageId.value = ""
        messageText.value = ""
        clearError()
    }

    private fun editCurrentMessage(messageId: String, text: String) {
        if (isSending.value) return
        clearError()
        viewModelScope.launch {
            isSending.value = true
            try {
                editMessageUseCase(groupId, messageId, text)
                    .onSuccess { clearComposer() }
                    .onFailure { error -> setError(error.message ?: "Message could not be edited") }
            } finally {
                isSending.value = false
            }
        }
    }

    private fun toggleReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            toggleMessageReaction(groupId, messageId, emoji)
                .onFailure { error -> setError(error.message ?: "Reaction could not be sent") }
        }
    }

    private fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            deleteMessageUseCase(groupId, messageId)
                .onFailure { error -> setError(error.message ?: "Message could not be deleted") }
        }
    }

    private fun retryFailedMessage(messageId: String) {
        viewModelScope.launch {
            retryMessage(messageId)
                .onFailure { error -> setError(error.message ?: "Message could not be queued again") }
        }
    }

    private fun acceptCurrentInvitation() {
        viewModelScope.launch {
            acceptInvitation(groupId)
                .onFailure { error -> setError(error.message ?: "Group invitation could not be accepted") }
        }
    }

    private fun declineCurrentInvitation() {
        viewModelScope.launch {
            declineInvitation(groupId)
                .onSuccess { navigator.popBackStackTo(AppRoute.Main) }
                .onFailure { error -> setError(error.message ?: "Group invitation could not be declined") }
        }
    }

    private fun transitionLocationShare(event: LocationShareEvent) {
        locationShareState.value =
            LocationShareStateMachine.transition(
                state = locationShareState.value,
                event = event
            )
    }

    private fun setError(message: String) {
        logger.error { message }
        mutableErrorMessage.value = message
    }

    private fun clearError() {
        mutableErrorMessage.value = null
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

    private data class GroupComposerDraft(
        val text: String,
        val replyToMessageId: String?,
        val editingMessageId: String?
    )

    private data class GroupComposerRuntime(
        val media: List<MediaSelection>,
        val isSending: Boolean,
        val locationShareState: LocationShareState
    )

    private companion object {
        const val MESSAGE_TEXT_KEY = "messageText"
        const val REPLY_TO_MESSAGE_ID_KEY = "replyToMessageId"
        const val EDITING_MESSAGE_ID_KEY = "editingMessageId"
    }
}
