package com.cbgm.sparrow.feature.chats.presentation.group

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.ChatOpenTrace
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toOutgoingMessageAttachment
import com.cbgm.sparrow.feature.chats.domain.model.ForwardingTarget
import com.cbgm.sparrow.feature.chats.domain.model.IndicatorType
import com.cbgm.sparrow.feature.chats.domain.model.LocationShareEvent
import com.cbgm.sparrow.feature.chats.domain.model.LocationShareState
import com.cbgm.sparrow.feature.chats.domain.model.LocationShareStateMachine
import com.cbgm.sparrow.feature.chats.domain.model.MessageComposerPolicy
import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryCursor
import com.cbgm.sparrow.feature.chats.domain.model.group.ChatMessageType
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupChatContext
import com.cbgm.sparrow.feature.chats.domain.usecase.FindMessageHistoryCursorUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.forward.ForwardMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.forward.LoadOlderMessagesUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.DeleteGroupMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.EditGroupMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.MarkGroupConversationReadUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupChatContextUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupMemberIndicatorUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.PinGroupMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.RetryGroupMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.SendGroupMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.SetGroupIndicatorUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ToggleGroupMessageReactionUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.UnpinGroupMessageUseCase
import com.cbgm.sparrow.feature.chats.presentation.common.composer.mapper.toComposerAvailabilityUi
import com.cbgm.sparrow.feature.chats.presentation.common.composer.mapper.toIndicatorUiType
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.IndicatorUiState
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.MessageComposerUiState
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageBubbleUi
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageContextUiState
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageHistoryUiState
import com.cbgm.sparrow.feature.chats.presentation.group.mapper.toGroupConversationUiState
import com.cbgm.sparrow.feature.chats.presentation.group.mapper.toGroupMembershipUiState
import com.cbgm.sparrow.feature.chats.presentation.group.mapper.toGroupReplyPreview
import com.cbgm.sparrow.feature.chats.presentation.group.mapper.toIndicatorDisplayName
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupConversationUiEvent
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupConversationUiState
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupMembershipUiState
import com.cbgm.sparrow.feature.contacts.domain.model.device.AddDeviceContactResult
import com.cbgm.sparrow.feature.contacts.domain.usecase.AddDeviceContactUseCase
import com.cbgm.sparrow.feature.media.domain.repository.MediaSelectionFileRepository
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionType
import com.cbgm.sparrow.feature.membership.domain.model.GroupAdministrationState
import com.cbgm.sparrow.feature.safety.domain.usecase.ObserveMessageSafetyAssessmentsUseCase
import com.cbgm.sparrow.feature.safety.presentation.details.mapper.toMessageSafetyDetails
import com.cbgm.sparrow.feature.voice.domain.usecase.GetRecordedVoiceAttachmentUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.ObserveVoiceRecordingActiveUseCase
import com.cbgm.sparrow.feature.voice.domain.usecase.ResetVoiceComposerUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

@OptIn(ExperimentalCoroutinesApi::class)
class GroupConversationViewModel(
    savedStateHandle: SavedStateHandle,
    observeChatContext: ObserveGroupChatContextUseCase,
    private val sendMessage: SendGroupMessageUseCase,
    private val markConversationRead: MarkGroupConversationReadUseCase,
    private val retryMessage: RetryGroupMessageUseCase,
    private val toggleMessageReaction: ToggleGroupMessageReactionUseCase,
    private val deleteMessageUseCase: DeleteGroupMessageUseCase,
    private val editMessageUseCase: EditGroupMessageUseCase,
    private val pinMessageUseCase: PinGroupMessageUseCase,
    private val unpinMessageUseCase: UnpinGroupMessageUseCase,
    observeMemberIndicator: ObserveGroupMemberIndicatorUseCase,
    setGroupIndicator: SetGroupIndicatorUseCase,
    observeMessageSafetyAssessments: ObserveMessageSafetyAssessmentsUseCase,
    private val addDeviceContact: AddDeviceContactUseCase,
    private val forwardMessageUseCase: ForwardMessageUseCase,
    private val loadOlderMessageHistory: LoadOlderMessagesUseCase,
    private val findMessageHistoryCursor: FindMessageHistoryCursorUseCase,
    private val getRecordedVoiceAttachment: GetRecordedVoiceAttachmentUseCase,
    private val resetVoiceComposer: ResetVoiceComposerUseCase,
    observeVoiceRecordingActive: ObserveVoiceRecordingActiveUseCase,
    private val mediaFiles: MediaSelectionFileRepository
) : BaseViewModel() {
    private val groupId =
        savedStateHandle.requireRouteArgument<String>(AppRoute.GroupConversation::conversationId.name)
    private val targetMessageId =
        savedStateHandle.get<String>(AppRoute.GroupConversation::targetMessageId.name)
    private val logger = SparrowLog.withTag("GroupConversationViewModel")

    init {
        ChatOpenTrace.event("group ViewModel constructed")
    }

    private val messageText = savedStateHandle.getMutableStateFlow(MESSAGE_TEXT_KEY, "")
    private val replyToMessageId = savedStateHandle.getMutableStateFlow(REPLY_TO_MESSAGE_ID_KEY, "")
    private val editingMessageId = savedStateHandle.getMutableStateFlow(EDITING_MESSAGE_ID_KEY, "")
    private val mutableErrorMessage = MutableStateFlow<String?>(null)
    private val selectedMedia = MutableStateFlow<List<MediaSelection>>(emptyList())
    private var preparingMediaSend = false

    // Draft cleanup must survive ViewModel clearing (viewModelScope is cancelled).
    private val mediaCleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val isSending = MutableStateFlow(false)
    private val contextMessageId = MutableStateFlow<String?>(null)
    private val locationShareState = MutableStateFlow(LocationShareState.IDLE)
    private val historyCursor = MutableStateFlow<MessageHistoryCursor?>(null)
    private val observedHistoryCursor = MutableStateFlow<MessageHistoryCursor?>(null)
    private val isLoadingOlderMessages = MutableStateFlow(false)
    private val hasMoreMessages = MutableStateFlow(true)
    private val indicatorController =
        IndicatorController(
            scope = viewModelScope,
            observeMemberIndicator = { contactId -> observeMemberIndicator(groupId, contactId) },
            sendIndicatorState = { indicatorType -> setGroupIndicator(groupId, indicatorType) },
            logTag = "GroupConversationViewModel"
        )

    private val groupContext =
        historyCursor.flatMapLatest { cursor ->
            observeChatContext(
                groupId = groupId,
                oldestCursor = cursor
            ).onStart {
                ChatOpenTrace.event("group context collection started (cursorInitial=${cursor == null})")
            }.onEach { context ->
                ChatOpenTrace.event("group context emitted messages=${context.conversation?.messages?.size ?: 0}")
                observedHistoryCursor.value = cursor
            }
        }

    // Share the error-aware presentation stream, so errors still become Failed.
    // All UI states, indicators and forwarding now consume the same observation.
    private val presentationContext: StateFlow<GroupContextObservation> =
        groupContext
            .map<GroupChatContext, GroupContextObservation> { context ->
                GroupContextObservation.Loaded(context)
            }.onStart { emit(GroupContextObservation.Loading) }
            .catch { error ->
                emit(GroupContextObservation.Failed(error.message ?: "Group conversation could not be loaded"))
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = GroupContextObservation.Loading
            )

    private val composerDraft =
        combine(messageText, replyToMessageId, editingMessageId) { text, replyId, editId ->
            GroupComposerDraft(
                text = text,
                replyToMessageId = replyId.takeIf(String::isNotBlank),
                editingMessageId = editId.takeIf(String::isNotBlank)
            )
        }

    private val composerRuntime =
        combine(
            selectedMedia,
            isSending,
            locationShareState
        ) { media, sending, locationState ->
            GroupComposerRuntime(
                media = media,
                isSending = sending,
                locationShareState = locationState
            )
        }

    val conversationState: StateFlow<GroupConversationUiState> =
        combine(
            presentationContext,
            observeMessageSafetyAssessments()
        ) { presentation, safetyAssessments ->
            val mappingStarted = TimeSource.Monotonic.markNow()
            val mapped = toGroupConversationUiState(
                groupId = groupId,
                conversation = presentation.context?.conversation,
                contacts = presentation.context?.contacts.orEmpty(),
                isLoading = presentation is GroupContextObservation.Loading,
                safetyAssessments = safetyAssessments,
                administration = presentation.context?.administration ?: GroupAdministrationState(),
                pin = presentation.context?.pin
            )
            ChatOpenTrace.event("group UI mapping completed messages=${mapped.messages.size} duration=${mappingStarted.elapsedNow().inWholeMilliseconds}ms loading=${mapped.isLoading}")
            mapped
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
                isLocationInProgress = runtime.locationShareState.isInProgress,
                availability = availability.toComposerAvailabilityUi()
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

    val indicatorState: StateFlow<IndicatorUiState> =
        combine(indicatorController.memberIndicators, presentationContext) { indicators, presentation ->
            val indicatorType = indicators.preferredIndicatorType()
            val indicatorContactIds = indicators.filterValues { it == indicatorType }.keys
            IndicatorUiState(
                type = indicatorType.toIndicatorUiType(),
                displayName = indicatorContactIds.toIndicatorDisplayName(presentation.context?.contacts.orEmpty())
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = IndicatorUiState()
        )

    val historyState: StateFlow<MessageHistoryUiState> =
        combine(
            isLoadingOlderMessages,
            hasMoreMessages,
            observedHistoryCursor
        ) { isLoadingOlder, hasMore, loadedCursor ->
            MessageHistoryUiState(
                isLoadingOlder = isLoadingOlder,
                hasMore = hasMore,
                loadedThroughMessageId = loadedCursor?.messageId
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MessageHistoryUiState()
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
        // Context-load failures are not guaranteed to pass through setError().
        viewModelScope.launch {
            presentationContext
                .map { observation -> observation.errorMessage }
                .distinctUntilChanged()
                .collect { message ->
                    if (message != null) SparrowLog.error("GroupConversationViewModel", message)
                }
        }
        indicatorController.start(
            presentationContext.map { observation ->
                observation.context?.administration?.currentMemberContactIds.orEmpty()
            }
        )
        viewModelScope.launch {
            observeVoiceRecordingActive().collect { isRecording ->
                indicatorController.onLocalVoiceRecordingChanged(
                    isRecording = isRecording,
                    sendsIndicators = conversationState.value.composerState.sendsIndicators
                )
            }
        }
        ensureTargetMessageLoaded()
    }

    fun onUiEvent(event: GroupConversationUiEvent) {
        when (event) {
            is GroupConversationUiEvent.MessageTextChanged -> onMessageTextChanged(event.text)
            GroupConversationUiEvent.SendClicked -> sendCurrentMessage()
            GroupConversationUiEvent.VoiceSendClicked -> sendVoiceMessage()
            GroupConversationUiEvent.LoadOlderMessages -> loadOlderMessages()
            is GroupConversationUiEvent.MessageHistoryTargetRequested -> loadMessageHistoryTarget(event.messageId)
            is GroupConversationUiEvent.ReplyToMessage -> startReply(event.messageId)
            GroupConversationUiEvent.CancelReply -> clearReply()
            is GroupConversationUiEvent.EditMessage -> startEdit(event.messageId)
            is GroupConversationUiEvent.MessageContextRequested -> contextMessageId.value = event.messageId
            GroupConversationUiEvent.MessageContextDismissed -> contextMessageId.value = null
            GroupConversationUiEvent.CancelEdit -> cancelEdit()
            is GroupConversationUiEvent.MessageReactionSelected -> toggleReaction(event.messageId, event.emoji)
            is GroupConversationUiEvent.DeleteMessage -> deleteMessage(event.messageId)
            is GroupConversationUiEvent.PinMessage -> pinMessage(event.messageId)
            GroupConversationUiEvent.UnpinMessage -> unpinMessage()
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
        }
    }

    fun stopIndicator() = indicatorController.stopLocalIndicator()

    fun markConversationRead() {
        viewModelScope.launch {
            markConversationRead(groupId)
                .onFailure { error -> logger.error(error) { "Could not mark group conversation as read" } }
        }
    }

    private fun loadOlderMessages() {
        if (isLoadingOlderMessages.value || !hasMoreMessages.value) return

        isLoadingOlderMessages.value = true
        viewModelScope.launch {
            try {
                loadOlderMessageHistory(
                    conversationId = groupId,
                    currentOldestCursor = historyCursor.value
                ).onSuccess { result ->
                    result.oldestCursor?.let { cursor -> historyCursor.value = cursor }
                    hasMoreMessages.value = result.hasMore
                }.onFailure { error ->
                    logger.error(error) { "Could not load older group messages" }
                }
            } finally {
                isLoadingOlderMessages.value = false
            }
        }
    }

    private fun loadMessageHistoryTarget(messageId: String) {
        viewModelScope.launch {
            findMessageHistoryCursor(groupId, messageId)
                .onSuccess { cursor ->
                    cursor ?: return@onSuccess
                    val currentCursor = historyCursor.value
                    if (currentCursor == null || cursor.isOlderThan(currentCursor)) {
                        historyCursor.value = cursor
                    }
                }.onFailure { error ->
                    logger.error(error) { "Could not load message history target $messageId" }
                }
        }
    }

    private fun ensureTargetMessageLoaded() {
        val messageId = targetMessageId ?: return
        viewModelScope.launch {
            conversationState.filter { state -> !state.isLoading }.first()
            if (conversationState.value.messages.any { message -> message.id == messageId }) return@launch

            findMessageHistoryCursor(groupId, messageId)
                .onSuccess { cursor ->
                    cursor?.let { historyCursor.value = it }
                }.onFailure { error ->
                    logger.error(error) { "Could not load target group message $messageId" }
                }
        }
    }

    private fun pinMessage(messageId: String) {
        viewModelScope.launch {
            pinMessageUseCase(groupId, messageId)
                .onFailure { error ->
                    setError(error.message ?: "Message could not be pinned")
                }
        }
    }

    private fun unpinMessage() {
        viewModelScope.launch {
            unpinMessageUseCase(groupId)
                .onFailure { error ->
                    setError(error.message ?: "Pinned message could not be removed")
                }
        }
    }

    private fun forwardMessage(
        messageId: String,
        target: ForwardingTarget
    ) {
        viewModelScope.launch {
            val message =
                presentationContext
                    .first { observation -> observation !is GroupContextObservation.Loading }
                    .context
                    ?.conversation
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
        indicatorController.onLocalTextChanged(
            value = value,
            sendsIndicators = conversationState.value.composerState.sendsIndicators
        )
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
        if (preparingMediaSend) return
        if (selections.isEmpty()) {
            dispatchSend(
                text = text,
                attachments = emptyList(),
                clearComposerOnSuccess = true,
                fallbackError = "Message could not be sent"
            )
            return
        }

        preparingMediaSend = true
        viewModelScope.launch {
            try {
                val attachments = selections.map { it.toOutgoingMessageAttachment(mediaFiles) }
                // Selection could change while the files are being read.
                if (selectedMedia.value != selections) return@launch
                dispatchSend(
                    text = text,
                    attachments = attachments,
                    clearComposerOnSuccess = true,
                    fallbackError = "Message could not be sent"
                )
            } catch (error: Exception) {
                setError(error.message ?: "Selected media could not be read")
            } finally {
                preparingMediaSend = false
            }
        }
    }

    private fun sendVoiceMessage() {
        getRecordedVoiceAttachment()
            .onSuccess { attachment ->
                dispatchSend(
                    text = "",
                    attachments = listOf(attachment),
                    clearComposerOnSuccess = false,
                    clearVoiceOnSuccess = true,
                    fallbackError = "Voice message could not be sent"
                )
            }.onFailure { error ->
                setError(error.message ?: "Voice message is not ready to send")
            }
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
        clearVoiceOnSuccess: Boolean = false,
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
                    .onSuccess {
                        when {
                            clearComposerOnSuccess -> clearComposer()
                            clearVoiceOnSuccess -> {
                                resetVoiceComposer()
                                clearReply()
                            }
                            else -> clearReply()
                        }
                    }
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
            require(media.size <= MessageAttachmentPolicy.MAX_ATTACHMENTS_PER_MESSAGE) {
                "Too many attachments selected"
            }
            require(media.map(MediaSelection::id).distinct().size == media.size) {
                "Attachment IDs must be unique"
            }
            require(media.sumOf(MediaSelection::byteSize) <= MessageAttachmentPolicy.MAX_TOTAL_ATTACHMENT_BYTES) {
                "Selected attachments exceed the total attachment size limit"
            }
            media.forEach { item ->
                require(item.byteSize > 0L) { "Selected attachment is empty" }
                when (item.type) {
                    MediaSelectionType.IMAGE -> {
                        require(item.byteSize <= MessageAttachmentPolicy.MAX_IMAGE_BYTES) { "Image attachment too large" }
                        require(item.mimeType.startsWith("image/") && item.width != null && item.height != null) {
                            "Invalid image attachment"
                        }
                    }
                    MediaSelectionType.VIDEO -> {
                        require(item.byteSize <= MessageAttachmentPolicy.MAX_VIDEO_BYTES && item.mimeType.startsWith("video/")) {
                            "Invalid video attachment"
                        }
                    }
                    MediaSelectionType.FILE -> {
                        require(item.byteSize <= MessageAttachmentPolicy.MAX_FILE_BYTES && !item.fileName.isNullOrBlank()) {
                            "Invalid file attachment"
                        }
                    }
                }
            }
        }.onSuccess {
            val removed = selectedMedia.value.filterNot { current -> media.any { it.id == current.id } }
            selectedMedia.value = media
            clearError()
            deletePendingSelections(removed)
        }.onFailure { error ->
            // A rejected selection may already have been copied to private storage.
            // Do not remove any file still referenced by the accepted composer state.
            val acceptedIds = selectedMedia.value.mapTo(mutableSetOf(), MediaSelection::id)
            deletePendingSelections(media.filterNot { it.id in acceptedIds })
            setError(error.message ?: "Selected attachments could not be attached")
        }
    }

    private fun clearComposer() {
        val consumed = selectedMedia.value
        messageText.value = ""
        replyToMessageId.value = ""
        editingMessageId.value = ""
        selectedMedia.value = emptyList()
        indicatorController.stopLocalIndicator()
        deletePendingSelections(consumed)
    }

    private fun deletePendingSelections(media: List<MediaSelection>) {
        if (media.isEmpty()) return
        mediaCleanupScope.launch {
            media.forEach { item ->
                // Delete each path independently: a missing original must not leak the thumbnail.
                for (path in listOfNotNull(item.localFilePath, item.thumbnailFilePath).distinct()) {
                    runCatching { mediaFiles.delete(path) }
                        .onFailure { error -> logger.error(error) { "Could not clean up pending media" } }
                }
            }
        }
    }

    override fun onCleared() {
        // The composer is ViewModel-owned; leaving the conversation discards unsent copies.
        // Do not launch this in viewModelScope: that scope is cancelled during onCleared.
        deletePendingSelections(selectedMedia.value)
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

private fun Map<String, IndicatorType>.preferredIndicatorType(): IndicatorType =
    when {
        values.any { it == IndicatorType.VOICE } -> IndicatorType.VOICE
        values.any { it == IndicatorType.TYPING } -> IndicatorType.TYPING
        else -> IndicatorType.NONE
    }
