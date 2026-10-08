package com.cbgm.sparrow.feature.chats.presentation.group

import com.cbgm.sparrow.core.messagepart.domain.model.Poll
import com.cbgm.sparrow.feature.chats.domain.usecase.group.CloseGroupPollUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.VoteInGroupPollUseCase
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationErrors
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationMediaController
import com.cbgm.sparrow.feature.polls.domain.usecase.ClearFinishedPollUseCase
import com.cbgm.sparrow.feature.polls.domain.usecase.ObserveFinishedPollUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class GroupPollController(
    private val voteInPoll: VoteInGroupPollUseCase,
    private val closePoll: CloseGroupPollUseCase,
    private val observeFinishedPoll: ObserveFinishedPollUseCase,
    private val clearFinishedPoll: ClearFinishedPollUseCase,
    private val media: ConversationMediaController
) {
    private lateinit var scope: CoroutineScope
    private lateinit var errors: ConversationErrors
    private lateinit var actions: GroupConversationActionsController
    private lateinit var groupId: String

    internal fun bind(
        scope: CoroutineScope,
        groupId: String,
        actions: GroupConversationActionsController,
        errors: ConversationErrors
    ) {
        this.scope = scope
        this.groupId = groupId
        this.actions = actions
        this.errors = errors
        scope.launch { observeFinishedPoll().collect { sendFinishedPoll(it) } }
    }

    fun vote(messageId: String, pollId: String, selectedOptionIds: Set<String>) {
        scope.launch {
            voteInPoll(groupId, messageId, pollId, selectedOptionIds)
                .onFailure { errors.report(it.message ?: "Poll vote could not be sent") }
        }
    }

    fun close(messageId: String, pollId: String) {
        scope.launch {
            closePoll(groupId, messageId, pollId)
                .onFailure { errors.report(it.message ?: "Poll could not be closed") }
        }
    }

    private fun sendFinishedPoll(poll: Poll) {
        actions.sendAttachmentOnly(poll, "Poll could not be sent") {
            media.deleteLocalPaths(
                poll.images.flatMap { listOfNotNull(it.localFilePath, it.thumbnailFilePath) }
            )
            clearFinishedPoll()
        }
    }
}
