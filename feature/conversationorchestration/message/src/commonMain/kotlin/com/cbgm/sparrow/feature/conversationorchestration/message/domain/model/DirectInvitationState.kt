package com.cbgm.sparrow.feature.conversationorchestration.message.domain.model

/** Whether an unauthorized contact already has a usable invitation in flight. */
enum class DirectInvitationState {
    /** An outgoing invitation is still pending; messages simply wait for it. */
    PENDING,

    /** No usable invitation exists; a fresh one may be started. */
    RETRYABLE,

    /** Authorization is missing and no invitation may be started automatically. */
    UNAVAILABLE
}
