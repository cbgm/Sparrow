package com.cbgm.sparrow.feature.chats.presentation.group

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.ChatOpenTrace
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toMessagePart
import com.cbgm.sparrow.feature.chats.domain.model.LocationShareEvent
import com.cbgm.sparrow.feature.chats.domain.model.group.ChatMessageType
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupChatContext
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupChatContextUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.ObserveGroupMemberIndicatorUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.SetGroupIndicatorUseCase
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationComposerController
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationErrors
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationHistoryController
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationMediaController
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupConversationUiEvent
import com.cbgm.sparrow.feature.identity.domain.usecase.GetLocalIdentityNameUseCase
import com.cbgm.sparrow.feature.safety.domain.usecase.ObserveMessageSafetyAssessmentsUseCase
import com.cbgm.sparrow.feature.safety.presentation.details.mapper.toMessageSafetyDetails
import com.cbgm.sparrow.feature.voice.domain.usecase.ObserveVoiceRecordingActiveUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class GroupConversationViewModel(
    savedStateHandle: SavedStateHandle,
    observeChatContext: ObserveGroupChatContextUseCase,
    observeMemberIndicator: ObserveGroupMemberIndicatorUseCase,
    setGroupIndicator: SetGroupIndicatorUseCase,
    observeMessageSafetyAssessments: ObserveMessageSafetyAssessmentsUseCase,
    observeVoiceRecordingActive: ObserveVoiceRecordingActiveUseCase,
    getLocalIdentityName: GetLocalIdentityNameUseCase,
    private val historyController: ConversationHistoryController,
    private val mediaController: ConversationMediaController,
    composerController: ConversationComposerController,
    private val actionsController: GroupConversationActionsController,
    private val pollController: GroupPollController,
    private val pinController: GroupPinController
) : BaseViewModel() {
    private val groupId =
        savedStateHandle.requireRouteArgument<String>(AppRoute.GroupConversation::conversationId.name)
    private val targetMessageId =
        savedStateHandle.get<String>(AppRoute.GroupConversation::targetMessageId.name)
    private val logger = SparrowLog.withTag("GroupConversationViewModel")
    private val localVoterDisplayName = MutableStateFlow<String?>(null)
    private val errors = ConversationErrors("GroupConversationViewModel")

    init {
        ChatOpenTrace.event("group ViewModel constructed")
        viewModelScope.launch {
            localVoterDisplayName.value = getLocalIdentityName().getOrNull()
        }
    }

    private val contextMessageId = MutableStateFlow<String?>(null)
    private val historyCursor = historyController.cursor
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
                historyController.markObserved(cursor)
            }
        }

    // Shares the error-aware presentation stream, so errors still become Failed.
    private val presentationContext: StateFlow<GroupContextObservation> =
        groupContext
            .map<GroupChatContext, GroupContextObservation> { context ->
                GroupContextObservation.Loaded(context)
            }.onStart { emit(GroupContextObservation.Loading) }
            .catch { error ->
                emit(
                    GroupContextObservation.Failed(
                        error.message ?: "Group conversation could not be loaded"
                    )
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = GroupContextObservation.Loading
            )

    private val state = GroupConversationStateController(
        groupId,
        viewModelScope,
        presentationContext,
        observeMessageSafetyAssessments,
        localVoterDisplayName,
        composerController,
        indicatorController,
        contextMessageId,
        historyController,
        errors
    )
    val conversationState = state.conversationState
    val membershipState = state.membershipState
    val composerState = state.composerState
    val contextState = state.contextState
    val indicatorState = state.indicatorState
    val historyState = state.historyState
    val errorMessage = state.errorMessage

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
        actionsController.bind(
            groupId = groupId,
            scope = viewModelScope,
            conversationState = conversationState,
            composerState = composerState,
            indicatorController = indicatorController,
            errors = errors,
            lookupForwardMessage = { messageId ->
                presentationContext.first { it !is GroupContextObservation.Loading }
                    .context?.conversation?.messages?.firstOrNull {
                        it.id == messageId && it.type == ChatMessageType.USER
                    }
            }
        )
        pollController.bind(viewModelScope, groupId, actionsController, errors)
        pinController.bind(viewModelScope, groupId, errors)
        historyController.ensureTargetMessageLoaded(
            scope = viewModelScope,
            conversationId = groupId,
            messageId = targetMessageId,
            ready = conversationState.map { !it.isLoading },
            containsMessage = { conversationState.value.messages.any { it.id == targetMessageId } },
            onError = { logger.error(it) { "Could not load target group message" } }
        )
    }

    fun onUiEvent(event: GroupConversationUiEvent) {
        when (event) {
            is GroupConversationUiEvent.MessageTextChanged -> actionsController.onMessageTextChanged(
                event.text
            )

            GroupConversationUiEvent.SendClicked -> actionsController.sendCurrentMessage()
            GroupConversationUiEvent.VoiceSendClicked -> actionsController.sendVoiceMessage()
            GroupConversationUiEvent.LoadOlderMessages -> historyController.loadOlder(
                viewModelScope,
                groupId
            ) { logger.error(it) { "Could not load older messages" } }

            is GroupConversationUiEvent.MessageHistoryTargetRequested -> historyController.requestTarget(
                viewModelScope,
                groupId,
                event.messageId
            ) { logger.error(it) { "History target could not be found" } }

            is GroupConversationUiEvent.ReplyToMessage -> actionsController.startReply(event.messageId)
            GroupConversationUiEvent.CancelReply -> actionsController.clearReply()
            is GroupConversationUiEvent.EditMessage -> actionsController.startEdit(event.messageId)
            is GroupConversationUiEvent.MessageContextRequested ->
                contextMessageId.value =
                    event.messageId

            GroupConversationUiEvent.MessageContextDismissed -> contextMessageId.value = null
            GroupConversationUiEvent.CancelEdit -> actionsController.cancelEdit()
            is GroupConversationUiEvent.MessageReactionSelected -> actionsController.toggleReaction(
                event.messageId,
                event.emoji
            )

            is GroupConversationUiEvent.PollVoteSubmitted ->
                pollController.vote(event.messageId, event.pollId, event.selectedOptionIds)

            is GroupConversationUiEvent.PollCloseRequested -> pollController.close(
                event.messageId,
                event.pollId
            )

            is GroupConversationUiEvent.DeleteMessage -> actionsController.deleteMessage(event.messageId)
            is GroupConversationUiEvent.PinMessage -> pinController.pin(event.messageId)
            GroupConversationUiEvent.UnpinMessage -> pinController.unpin()
            is GroupConversationUiEvent.ForwardMessage -> actionsController.forwardMessage(
                event.messageId,
                event.target
            )

            is GroupConversationUiEvent.MediaSelected -> actionsController.updateMediaSelection(
                event.media
            )

            is GroupConversationUiEvent.OpenFilePicker -> navigator.navigateTo(
                AppRoute.FilePicker(
                    event.sessionId
                )
            )

            GroupConversationUiEvent.LocationCaptureStarted -> actionsController.transitionLocationShare(
                LocationShareEvent.CAPTURE_STARTED
            )

            is GroupConversationUiEvent.ShareCurrentLocation ->
                actionsController.shareCurrentLocation(event.location.toMessagePart())

            is GroupConversationUiEvent.LocationCaptureFailed -> {
                actionsController.transitionLocationShare(LocationShareEvent.FAILED)
                errors.report(event.message)
            }

            is GroupConversationUiEvent.ShareContact ->
                actionsController.sendAttachmentOnly(
                    part = event.contact.toMessagePart(),
                    fallbackError = "Contact could not be sent"
                )

            is GroupConversationUiEvent.AddSharedContact -> actionsController.addSharedContact(event.contact)
            is GroupConversationUiEvent.AttachmentError -> errors.report(event.message)
            GroupConversationUiEvent.HeaderClicked -> navigator.navigateTo(
                AppRoute.GroupDetails(
                    groupId
                )
            )

            GroupConversationUiEvent.CreatePollClicked -> navigator.navigateTo(AppRoute.CreatePoll)
            GroupConversationUiEvent.ActivateExpensesClicked -> pinController.activateExpenseBoard()
            GroupConversationUiEvent.CloseExpensesClicked -> pinController.closeExpenseBoard()
            is GroupConversationUiEvent.RetryMessage -> actionsController.retryFailedMessage(event.messageId)
            is GroupConversationUiEvent.SafetyWarningClicked ->
                navigator.navigateTo(
                    event.warning.toMessageSafetyDetails(
                        event.messageId,
                        event.contactId
                    )
                )

            GroupConversationUiEvent.BackClicked ->
                if (targetMessageId != null) {
                    navigator.popBackStack()
                } else {
                    navigator.popBackStackTo(AppRoute.Main)
                }
        }
    }

    fun stopIndicator() = indicatorController.stopLocalIndicator()

    fun markConversationRead() = actionsController.markConversationRead()

    override fun onCleared() {
        mediaController.discardSelection()
    }
}
