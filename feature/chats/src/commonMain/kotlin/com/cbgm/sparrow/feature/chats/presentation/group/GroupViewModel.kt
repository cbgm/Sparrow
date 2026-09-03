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
import com.cbgm.sparrow.feature.chats.domain.model.group.ChatMessageType
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAdministrationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupChatContext
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
import com.cbgm.sparrow.feature.chats.presentation.group.mapper.toGroupUiState
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupUiEvent
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupUiState
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GroupViewModel(
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
    private val addDeviceContact: AddDeviceContactUseCase
) : BaseViewModel() {
    private val groupId =
        savedStateHandle.requireRouteArgument<String>(AppRoute.GroupConversation::conversationId.name)
    private val targetMessageId =
        savedStateHandle.get<String>(AppRoute.GroupConversation::targetMessageId.name)
    private val logger = SparrowLog.withTag("GroupViewModel")
    private val messageText = savedStateHandle.getMutableStateFlow(MESSAGE_TEXT_KEY, "")
    private val replyToMessageId = savedStateHandle.getMutableStateFlow(REPLY_TO_MESSAGE_ID_KEY, "")
    private val editingMessageId = savedStateHandle.getMutableStateFlow(EDITING_MESSAGE_ID_KEY, "")
    private val errorMessage = MutableStateFlow<String?>(null)
    private val selectedMedia = MutableStateFlow<List<MediaSelection>>(emptyList())
    private val attachmentBytes = MutableStateFlow<Map<String, ByteArray>>(emptyMap())
    private val isSending = MutableStateFlow(false)
    private val loadingAttachmentIds = mutableSetOf<String>()

    private val typing =
        GroupTypingController(
            scope = viewModelScope,
            observeMemberTyping = { contactId -> observeMemberTyping(groupId, contactId) },
            sendTypingState = { isTyping -> setGroupTyping(groupId, isTyping) },
            logTag = "GroupViewModel"
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

    private val composerContext =
        combine(
            composerDraft,
            errorMessage,
            typing.typingContactIds,
            selectedMedia,
            isSending
        ) { draft, error, typingIds, media, sending ->
            GroupComposerContext(
                draft.text,
                error,
                draft.replyToMessageId,
                draft.editingMessageId,
                typingIds,
                media,
                sending
            )
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
                currentReplyToMessageId = composer.replyToMessageId,
                observationError = presentation.errorMessage,
                isLoading = presentation is GroupContextObservation.Loading,
                typingContactIds = composer.typingIds,
                safetyAssessments = safetyAssessments,
                attachmentBytes = loadedAttachmentBytes
            ).copy(
                selectedMedia = composer.media,
                isSending = composer.isSending,
                editingMessageId = composer.editingMessageId
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GroupUiState()
        )

    init {
        typing.start(groupContext.map { context -> context.administration.currentMemberContactIds })
    }

    fun onUiEvent(event: GroupUiEvent) {
        when (event) {
            is GroupUiEvent.MessageTextChanged -> onMessageTextChanged(event.text)
            GroupUiEvent.SendClicked -> sendCurrentMessage()
            is GroupUiEvent.ReplyToMessage -> startReply(event.messageId)
            GroupUiEvent.CancelReply -> clearReply()
            is GroupUiEvent.EditMessage -> startEdit(event.messageId)
            GroupUiEvent.CancelEdit -> cancelEdit()
            is GroupUiEvent.MessageReactionSelected -> toggleReaction(event.messageId, event.emoji)
            is GroupUiEvent.DeleteMessage -> deleteMessage(event.messageId)
            is GroupUiEvent.MediaSelected -> updateMediaSelection(event.media)
            is GroupUiEvent.OpenFilePicker -> navigator.navigateTo(AppRoute.FilePicker(event.sessionId))
            is GroupUiEvent.ShareCurrentLocation ->
                sendAttachmentOnly(event.location.toOutgoingMessageAttachment(), fallbackError = "Location could not be sent")

            is GroupUiEvent.ShareContact ->
                sendAttachmentOnly(event.contact.toOutgoingMessageAttachment(), fallbackError = "Contact could not be sent")

            is GroupUiEvent.AddSharedContact -> addSharedContact(event.contact)
            is GroupUiEvent.AttachmentVisible -> loadAttachment(event.attachmentId)
            is GroupUiEvent.AttachmentError -> errorMessage.value = event.message
            GroupUiEvent.HeaderClicked -> navigator.navigateTo(AppRoute.GroupDetails(groupId))
            is GroupUiEvent.RetryMessage -> retryFailedMessage(event.messageId)
            is GroupUiEvent.SafetyWarningClicked ->
                navigator.navigateTo(event.warning.toMessageSafetyDetails(event.messageId, event.contactId))
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

    fun stopTyping() = typing.stopLocalTyping()

    fun markConversationRead() {
        viewModelScope.launch {
            markConversationRead(groupId)
                .onFailure { error -> logger.warn(error) { "Could not mark group conversation as read" } }
        }
    }

    private fun onMessageTextChanged(value: String) {
        messageText.value = value
        errorMessage.value = null
        typing.onLocalTextChanged(value)
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

        val attachments = selections.map(MediaSelection::toOutgoingMessageAttachment)
        dispatchSend(
            text = text,
            attachments = attachments,
            clearComposerOnSuccess = true,
            fallbackError = "Message could not be sent"
        )
    }

    /** Shared entry point for location and contact shares: text is always empty, one attachment. */
    private fun sendAttachmentOnly(attachment: OutgoingMessageAttachment, fallbackError: String) {
        dispatchSend(
            text = "",
            attachments = listOf(attachment),
            clearComposerOnSuccess = false,
            fallbackError = fallbackError
        )
    }

    /**
     * Guards on input-enabled/sending state, flips [isSending] for the duration, sends the
     * payload, and clears either the whole composer or just the reply on success. Shared by
     * [sendCurrentMessage] and [sendAttachmentOnly] so this bookkeeping lives in one place.
     */
    private fun dispatchSend(
        text: String,
        attachments: List<OutgoingMessageAttachment>,
        clearComposerOnSuccess: Boolean,
        fallbackError: String
    ) {
        if (!uiState.value.isMessageInputEnabled || isSending.value) return

        val replyTo = replyToMessageId.value.takeIf(String::isNotBlank)
        errorMessage.value = null
        viewModelScope.launch {
            isSending.value = true
            try {
                sendMessage(groupId, text, attachments, replyTo)
                    .onSuccess { if (clearComposerOnSuccess) clearComposer() else clearReply() }
                    .onFailure { error -> errorMessage.value = error.message ?: fallbackError }
            } finally {
                isSending.value = false
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
                AddDeviceContactResult.AlreadyExists -> errorMessage.value = null

                AddDeviceContactResult.PermissionDenied ->
                    errorMessage.value = "Contacts permission is required to add this contact"

                AddDeviceContactResult.InvalidPhoneNumber ->
                    errorMessage.value = "The shared phone number is invalid"

                is AddDeviceContactResult.Failure ->
                    errorMessage.value = result.throwable.message ?: "Contact could not be added"
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

    private suspend fun clearComposer() {
        messageText.value = ""
        replyToMessageId.value = ""
        editingMessageId.value = ""
        selectedMedia.value = emptyList()
        typing.stopLocalTypingNow()
    }

    private fun startReply(messageId: String) {
        if (uiState.value.messages.none { message -> message.id == messageId }) return
        editingMessageId.value = ""
        replyToMessageId.value = messageId
        errorMessage.value = null
    }

    private fun clearReply() {
        replyToMessageId.value = ""
    }

    private fun startEdit(messageId: String) {
        val message = uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        val text = message.bubble.textPart?.text?.takeIf(String::isNotBlank) ?: return
        if (!message.bubble.isMine || message.type != ChatMessageType.USER) return
        if (message.bubble.deliveryProgress.readCount > 0) return
        if (message.bubble.fileParts.isNotEmpty() || message.bubble.imageVideoParts.isNotEmpty() || message.bubble.locationPart != null || message.bubble.contactPart != null) return

        replyToMessageId.value = ""
        selectedMedia.value = emptyList()
        editingMessageId.value = messageId
        messageText.value = text
        errorMessage.value = null
    }

    private fun cancelEdit() {
        editingMessageId.value = ""
        messageText.value = ""
        errorMessage.value = null
    }

    private fun editCurrentMessage(messageId: String, text: String) {
        if (isSending.value) return
        errorMessage.value = null
        viewModelScope.launch {
            isSending.value = true
            try {
                editMessageUseCase(groupId, messageId, text)
                    .onSuccess { clearComposer() }
                    .onFailure { error -> errorMessage.value = error.message ?: "Message could not be edited" }
            } finally {
                isSending.value = false
            }
        }
    }

    private fun toggleReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            toggleMessageReaction(groupId, messageId, emoji)
                .onFailure { error -> errorMessage.value = error.message ?: "Reaction could not be sent" }
        }
    }

    private fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            deleteMessageUseCase(groupId, messageId)
                .onFailure { error -> errorMessage.value = error.message ?: "Message could not be deleted" }
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

    private data class GroupComposerContext(
        val text: String,
        val error: String?,
        val replyToMessageId: String?,
        val editingMessageId: String?,
        val typingIds: Set<String>,
        val media: List<MediaSelection>,
        val isSending: Boolean
    )

    private companion object {
        const val MESSAGE_TEXT_KEY = "messageText"
        const val REPLY_TO_MESSAGE_ID_KEY = "replyToMessageId"
        const val EDITING_MESSAGE_ID_KEY = "editingMessageId"
    }
}
