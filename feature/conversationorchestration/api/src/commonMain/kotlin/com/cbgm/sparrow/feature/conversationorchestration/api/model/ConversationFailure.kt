package com.cbgm.sparrow.feature.conversationorchestration.api.model

data class ConversationFailure(
    val reason: ConversationFailureReason,
    val message: String,
    val cause: Throwable? = null
)

enum class ConversationFailureReason {
    /** Identity exchange or contact authorization is missing. */
    AUTHORIZATION_REQUIRED,

    /** The command was valid, but the current conversation state does not allow it. */
    NOT_ALLOWED,

    /** The command itself contained invalid input. */
    INVALID_INPUT,

    /** An unexpected persistence, crypto or transport error. */
    UNEXPECTED
}

class ConversationFailureException(
    val failure: ConversationFailure
) : Exception(failure.message, failure.cause)
