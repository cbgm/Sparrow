package com.cbgm.sparrow.feature.conversationorchestration.engine.failure

import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailure
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureException
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureReason

/** Translates a step error into the failure published to Chats. */
object ConversationFailureMapper {
    fun toFailure(error: Throwable): ConversationFailure =
        when (error) {
            is ConversationFailureException -> error.failure
            is DirectChatAuthorizationRequiredException ->
                failure(ConversationFailureReason.AUTHORIZATION_REQUIRED, error)
            is IllegalArgumentException -> failure(ConversationFailureReason.INVALID_INPUT, error)
            is IllegalStateException -> failure(ConversationFailureReason.NOT_ALLOWED, error)
            else -> failure(ConversationFailureReason.UNEXPECTED, error)
        }

    private fun failure(reason: ConversationFailureReason, error: Throwable): ConversationFailure =
        ConversationFailure(
            reason = reason,
            message = error.message ?: error::class.simpleName.orEmpty(),
            cause = error
        )
}
