package com.cbgm.sparrow.feature.chats.domain.model.direct

sealed interface DirectMessageDispatchResult {
    data object Sent : DirectMessageDispatchResult

    data object Queued : DirectMessageDispatchResult

    data class QueuedWithIdentityExchangeFailure(
        val throwable: Throwable
    ) : DirectMessageDispatchResult
}
