package com.cbgm.sparrow.feature.chats.presentation.group

import com.cbgm.sparrow.core.logging.ChatOpenTrace
import com.cbgm.sparrow.feature.chats.domain.model.IndicatorType
import com.cbgm.sparrow.feature.chats.domain.model.MessageComposerPolicy
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupChatContext
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
import com.cbgm.sparrow.feature.chats.presentation.group.mapper.toGroupConversationUiState
import com.cbgm.sparrow.feature.chats.presentation.group.mapper.toGroupMembershipUiState
import com.cbgm.sparrow.feature.chats.presentation.group.mapper.toGroupReplyPreview
import com.cbgm.sparrow.feature.chats.presentation.group.mapper.toIndicatorDisplayName
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupConversationUiState
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupMembershipUiState
import com.cbgm.sparrow.feature.membership.domain.model.GroupAdministrationState
import com.cbgm.sparrow.feature.safety.domain.usecase.ObserveMessageSafetyAssessmentsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.time.TimeSource

internal class GroupConversationStateController(
    private val groupId: String,
    private val scope: CoroutineScope,
    presentationContext: StateFlow<GroupContextObservation>,
    observeMessageSafetyAssessments: ObserveMessageSafetyAssessmentsUseCase,
    private val localVoterDisplayName: StateFlow<String?>,
    composerController: ConversationComposerController,
    indicatorController: IndicatorController,
    contextMessageId: StateFlow<String?>,
    historyController: ConversationHistoryController,
    errors: ConversationErrors
) {
    private val composerDraft = composerController.draft
    private val composerRuntime = composerController.runtime

    val conversationState: StateFlow<GroupConversationUiState> =
        combine(
            presentationContext,
            observeMessageSafetyAssessments(),
            localVoterDisplayName
        ) { presentation, safetyAssessments, localDisplayName ->
            val mappingStarted = TimeSource.Monotonic.markNow()
            val mapped = toGroupConversationUiState(
                groupId = groupId,
                conversation = presentation.context?.conversation,
                contacts = presentation.context?.contacts.orEmpty(),
                isLoading = presentation is GroupContextObservation.Loading,
                safetyAssessments = safetyAssessments,
                administration = presentation.context?.administration ?: GroupAdministrationState(),
                pin = presentation.context?.pin,
                localVoterDisplayName = localDisplayName
            )
            ChatOpenTrace.event("group UI mapping completed messages=${mapped.messages.size} duration=${mappingStarted.elapsedNow().inWholeMilliseconds}ms loading=${mapped.isLoading}")
            mapped
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GroupConversationUiState()
        )

    val membershipState: StateFlow<GroupMembershipUiState> =
        presentationContext
            .map { presentation ->
                toGroupMembershipUiState(
                    conversation = presentation.context?.conversation,
                    administration = presentation.context?.administration
                        ?: GroupAdministrationState(),
                    contacts = presentation.context?.contacts.orEmpty()
                )
            }.stateIn(
                scope = scope,
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
            scope = scope,
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
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MessageContextUiState()
        )

    val indicatorState: StateFlow<IndicatorUiState> =
        combine(
            indicatorController.memberIndicators,
            presentationContext
        ) { indicators, presentation ->
            val indicatorType = indicators.preferredIndicatorType()
            val indicatorContactIds = indicators.filterValues { it == indicatorType }.keys
            IndicatorUiState(
                type = indicatorType.toIndicatorUiType(),
                displayName = indicatorContactIds.toIndicatorDisplayName(presentation.context?.contacts.orEmpty())
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = IndicatorUiState()
        )

    val historyState: StateFlow<MessageHistoryUiState> = historyController.observe(scope)

    val errorMessage: StateFlow<String?> =
        combine(errors.messages, presentationContext) { currentError, presentation ->
            currentError ?: presentation.errorMessage
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )
}

internal sealed interface GroupContextObservation {
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

private fun Map<String, IndicatorType>.preferredIndicatorType(): IndicatorType =
    when {
        values.any { it == IndicatorType.VOICE } -> IndicatorType.VOICE
        values.any { it == IndicatorType.TYPING } -> IndicatorType.TYPING
        else -> IndicatorType.NONE
    }
