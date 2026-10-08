package com.cbgm.sparrow.feature.chats.presentation.direct

import com.cbgm.sparrow.core.logging.ChatOpenTrace
import com.cbgm.sparrow.feature.chats.domain.model.MessageComposerPolicy
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectChatContext
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectComposerState
import com.cbgm.sparrow.feature.chats.presentation.common.composer.mapper.toComposerAvailabilityUi
import com.cbgm.sparrow.feature.chats.presentation.common.composer.mapper.toIndicatorUiType
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.IndicatorUiState
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.MessageComposerUiState
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationComposerController
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationErrors
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationHistoryController
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageBubbleUi
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageContextUiState
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageHistoryUiState
import com.cbgm.sparrow.feature.chats.presentation.direct.mapper.toDirectConversationUiState
import com.cbgm.sparrow.feature.chats.presentation.direct.mapper.toDirectReplyPreview
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectConversationUiState
import com.cbgm.sparrow.feature.safety.domain.usecase.ObserveMessageSafetyAssessmentsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.time.TimeSource

internal class DirectConversationStateController(
    private val contactId: String,
    private val fallbackContactName: String,
    private val scope: CoroutineScope,
    conversationContext: Flow<DirectChatContext>,
    observeMessageSafetyAssessments: ObserveMessageSafetyAssessmentsUseCase,
    composerController: ConversationComposerController,
    indicatorController: IndicatorController,
    contextMessageId: StateFlow<String?>,
    historyController: ConversationHistoryController,
    errors: ConversationErrors
) {
    private val composerDraft = composerController.draft
    private val composerRuntime = composerController.runtime

    val conversationState: StateFlow<DirectConversationUiState> =
        combine(
            conversationContext,
            observeMessageSafetyAssessments()
        ) { context, safetyAssessments ->
            val mappingStarted = TimeSource.Monotonic.markNow()
            val mapped = toDirectConversationUiState(
                contactId = contactId,
                fallbackContactName = fallbackContactName,
                conversation = context.conversation,
                contact = context.contact,
                remoteIdentity = context.remoteIdentity,
                handshake = context.handshake,
                canQueueMessages = context.canQueueMessages,
                setupMode = context.setupMode,
                localIdentityShared = context.localIdentityShared,
                safetyAssessments = safetyAssessments
            )
            ChatOpenTrace.event("direct UI mapping completed messages=${mapped.messages.size} duration=${mappingStarted.elapsedNow().inWholeMilliseconds}ms loading=${mapped.isLoading}")
            mapped
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue =
                DirectConversationUiState(
                    contactId = contactId,
                    contactName = fallbackContactName,
                    composerState = DirectComposerState.DISABLED
                )
        )

    val composerState: StateFlow<MessageComposerUiState> =
        combine(
            conversationState,
            conversationContext,
            composerDraft,
            composerRuntime
        ) { conversation, context, draft, runtime ->
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
                    draft.replyToMessageId.toDirectReplyPreview(
                        conversation = context.conversation,
                        contactName = conversation.contactName
                    ),
                editingMessageId = draft.editingMessageId,
                selectedMedia = runtime.media,
                isSending = runtime.isSending,
                isLocationInProgress = runtime.locationShareState.isInProgress,
                availability = availability.toComposerAvailabilityUi()
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
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
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = MessageContextUiState()
        )

    val indicatorState: StateFlow<IndicatorUiState> =
        combine(
            indicatorController.remoteIndicatorType,
            conversationState
        ) { indicatorType, conversation ->
            IndicatorUiState(
                type = indicatorType.toIndicatorUiType(),
                displayName = conversation.contactName
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = IndicatorUiState(displayName = fallbackContactName)
        )

    val historyState: StateFlow<MessageHistoryUiState> = historyController.observe(scope)

    val errorMessage: StateFlow<String?> = errors.messages
}
