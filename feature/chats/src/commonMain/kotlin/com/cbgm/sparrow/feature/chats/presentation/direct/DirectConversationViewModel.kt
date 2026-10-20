package com.cbgm.sparrow.feature.chats.presentation.direct

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.core.util.logging.ChatOpenTrace
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toMessagePart
import com.cbgm.sparrow.feature.chats.domain.model.LocationShareEvent
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ObserveDirectChatContextUseCase
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationComposerController
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationErrors
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationHistoryController
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationMediaController
import com.cbgm.sparrow.feature.chats.presentation.common.controller.DirectIdentityController
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectConversationUiEvent
import com.cbgm.sparrow.feature.conversationorchestration.api.ConversationOrchestrator
import com.cbgm.sparrow.feature.conversationorchestration.api.awaitResult
import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand
import com.cbgm.sparrow.feature.safety.presentation.details.mapper.toMessageSafetyDetails
import com.cbgm.sparrow.feature.voice.domain.usecase.ObserveVoiceRecordingActiveUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DirectConversationViewModel(
    savedStateHandle: SavedStateHandle,
    observeChatContext: ObserveDirectChatContextUseCase,
    private val orchestrator: ConversationOrchestrator,
    observeVoiceRecordingActive: ObserveVoiceRecordingActiveUseCase,
    private val historyController: ConversationHistoryController,
    private val mediaController: ConversationMediaController,
    composerController: ConversationComposerController,
    private val identityController: DirectIdentityController,
    private val indicatorController: DirectIndicatorController,
    stateController: DirectConversationStateController,
    private val actionsController: DirectConversationActionsController
) : BaseViewModel() {
    private val conversationId =
        savedStateHandle.requireRouteArgument<String>(AppRoute.Chat::conversationId.name)
    private val contactId =
        savedStateHandle.requireRouteArgument<String>(AppRoute.Chat::contactId.name)
    private val fallbackContactName =
        savedStateHandle.requireRouteArgument<String>(AppRoute.Chat::contactName.name)
    private val targetMessageId =
        savedStateHandle.get<String>(AppRoute.Chat::targetMessageId.name)
    private val logger = SparrowLog.withTag("DirectConversationViewModel")

    init {
        ChatOpenTrace.event("direct ViewModel constructed")
    }

    private val errors = ConversationErrors("DirectConversationViewModel")
    private val contextMessageId = MutableStateFlow<String?>(null)
    private val historyCursor = historyController.cursor
    private val conversationContext =
        historyCursor.flatMapLatest { cursor ->
            observeChatContext(
                conversationId = conversationId,
                contactId = contactId,
                oldestCursor = cursor
            ).onStart {
                ChatOpenTrace.event("direct context collection started (cursorInitial=${cursor == null})")
            }.onEach { context ->
                ChatOpenTrace.event("direct context emitted messages=${context.conversation?.messages?.size ?: 0}")
                historyController.markObserved(cursor)
            }
        }.shareIn(
            scope = viewModelScope,
            // Start before the screen collects its states; no pre-navigation wait.
            started = SharingStarted.Eagerly,
            replay = 1
        )

    init {
        composerController.bind(savedStateHandle)
        historyController.bind(viewModelScope, conversationId)
        identityController.bind(viewModelScope, contactId)
        indicatorController.bind(viewModelScope, contactId)
        stateController.bind(
            contactId = contactId,
            fallbackContactName = fallbackContactName,
            scope = viewModelScope,
            conversationContext = conversationContext,
            contextMessageId = contextMessageId,
            errors = errors
        )
        identityController.recoverAfterManualImport(conversationContext) {
            logger.error(it) { "Manual identity exchange recovery failed" }
        }
    }

    val conversationState = stateController.conversationState
    val composerState = stateController.composerState
    val contextState = stateController.contextState
    val indicatorState = stateController.indicatorState
    val historyState = stateController.historyState
    val errorMessage = stateController.errorMessage

    init {
        viewModelScope.launch {
            observeVoiceRecordingActive().collect { isRecording ->
                indicatorController.onLocalVoiceRecordingChanged(
                    isRecording = isRecording,
                    sendsIndicators = conversationState.value.composerState.sendsIndicators
                )
            }
        }
        actionsController.bind(
            conversationId = conversationId,
            contactId = contactId,
            scope = viewModelScope,
            conversationState = conversationState,
            composerState = composerState,
            indicatorController = indicatorController,
            errors = errors,
            lookupForwardMessage = { messageId ->
                conversationContext.first().conversation?.messages?.firstOrNull { it.id == messageId }
            }
        )
        historyController.ensureTargetMessageLoaded(
            messageId = targetMessageId,
            ready = conversationState.map { !it.isLoading },
            containsMessage = { conversationState.value.messages.any { it.id == targetMessageId } },
            onError = { logger.error(it) { "Could not load target direct message" } }
        )
    }

    fun onUiEvent(event: DirectConversationUiEvent) {
        when (event) {
            is DirectConversationUiEvent.MessageTextChanged -> actionsController.onMessageTextChanged(
                event.text
            )

            DirectConversationUiEvent.SendClicked -> actionsController.sendCurrentMessage()
            DirectConversationUiEvent.VoiceSendClicked -> actionsController.sendVoiceMessage()
            DirectConversationUiEvent.LoadOlderMessages -> historyController.loadOlder { logger.error(it) { "Could not load older messages" } }

            is DirectConversationUiEvent.MessageHistoryTargetRequested -> historyController.requestTarget(
                event.messageId
            ) { logger.error(it) { "History target could not be found" } }

            is DirectConversationUiEvent.ReplyToMessage -> actionsController.startReply(event.messageId)
            DirectConversationUiEvent.CancelReply -> actionsController.clearReply()
            is DirectConversationUiEvent.EditMessage -> actionsController.startEdit(event.messageId)
            is DirectConversationUiEvent.MessageContextRequested ->
                contextMessageId.value =
                    event.messageId

            DirectConversationUiEvent.MessageContextDismissed -> contextMessageId.value = null
            DirectConversationUiEvent.CancelEdit -> actionsController.cancelEdit()
            is DirectConversationUiEvent.MessageReactionSelected -> actionsController.toggleReaction(
                event.messageId,
                event.emoji
            )

            is DirectConversationUiEvent.DeleteMessage -> actionsController.deleteMessage(event.messageId)
            is DirectConversationUiEvent.ForwardMessage -> actionsController.forwardMessage(
                event.messageId,
                event.target
            )

            is DirectConversationUiEvent.MediaSelected -> actionsController.updateMediaSelection(
                event.media
            )

            is DirectConversationUiEvent.OpenFilePicker -> navigator.navigateTo(
                AppRoute.FilePicker(
                    event.sessionId
                )
            )

            DirectConversationUiEvent.LocationCaptureStarted -> actionsController.transitionLocationShare(
                LocationShareEvent.CAPTURE_STARTED
            )

            is DirectConversationUiEvent.ShareCurrentLocation ->
                actionsController.shareCurrentLocation(event.location.toMessagePart())

            is DirectConversationUiEvent.LocationCaptureFailed -> {
                actionsController.transitionLocationShare(LocationShareEvent.FAILED)
                errors.report(event.message)
            }

            is DirectConversationUiEvent.ShareContact ->
                actionsController.sendAttachmentOnly(event.contact.toMessagePart())

            is DirectConversationUiEvent.AddSharedContact -> actionsController.addSharedContact(
                event.contact
            )

            is DirectConversationUiEvent.AttachmentError -> errors.report(event.message)
            DirectConversationUiEvent.HeaderClicked -> navigator.navigateTo(
                AppRoute.ContactDetails(
                    conversationId,
                    contactId
                )
            )

            is DirectConversationUiEvent.RetryMessage -> actionsController.retryFailedMessage(event.messageId)
            is DirectConversationUiEvent.SafetyWarningClicked ->
                navigator.navigateTo(
                    event.warning.toMessageSafetyDetails(
                        event.messageId,
                        contactId
                    )
                )

            DirectConversationUiEvent.VerifyIdentityClicked -> navigator.navigateTo(
                AppRoute.ContactDetails(
                    conversationId,
                    contactId,
                    openVerification = true
                )
            )

            DirectConversationUiEvent.ShareIdentityClicked ->
                identityController.shareIdentity(
                    onSuccess = { navigator.navigateTo(AppRoute.ShareIdentity) },
                    onFailure = {
                        errors.report(
                            it.message ?: "Could not save identity sharing progress"
                        )
                    }
                )

            DirectConversationUiEvent.ImportIdentityClicked -> navigator.navigateTo(
                AppRoute.ImportContact(
                    contactId
                )
            )

            DirectConversationUiEvent.BackClicked ->
                if (targetMessageId != null) {
                    navigator.popBackStack()
                } else {
                    navigator.popBackStackTo(AppRoute.Main)
                }
        }
    }

    fun stopIndicator() = indicatorController.stopLocalIndicator()

    fun markConversationRead() = actionsController.markConversationRead()

    /** Asks the contact for a fresh invitation while this chat and its history stay. */
    suspend fun requestReconnect(): Result<Unit> =
        orchestrator.awaitResult(DirectConversationCommand.ReconnectConversation(contactId)).map { }

    override fun onCleared() {
        mediaController.discardSelection()
    }
}
