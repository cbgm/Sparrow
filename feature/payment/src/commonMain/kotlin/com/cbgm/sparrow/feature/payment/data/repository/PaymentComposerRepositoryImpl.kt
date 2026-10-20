package com.cbgm.sparrow.feature.payment.data.repository

import com.cbgm.sparrow.core.base.messagepart.domain.model.PaymentTransfer
import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.core.util.result.safeCall
import com.cbgm.sparrow.feature.payment.domain.model.PaymentContext
import com.cbgm.sparrow.feature.payment.domain.model.PaymentRequest
import com.cbgm.sparrow.feature.payment.domain.repository.PaymentComposerRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet

internal class PaymentComposerRepositoryImpl : PaymentComposerRepository {
    private val state = MutableStateFlow(ComposerState())
    private val channels = MutableStateFlow<Map<String, Channel<PaymentRequest>>>(emptyMap())

    override fun observeContext(groupId: String): Flow<PaymentContext?> =
        state.filter { groupId in it.contexts }
            .map { it.contexts[groupId] }
            .distinctUntilChanged()

    override fun publishContext(groupId: String, context: PaymentContext?): Result<Unit> = safeCall {
        updateContext(groupId, context)
    }

    override fun clearContext(groupId: String): Result<Unit> = safeCall {
        updateContext(groupId, null)
    }

    override fun observeRequests(groupId: String): Flow<PaymentRequest> =
        channel(groupId).receiveAsFlow().filter { request ->
            state.value.requests[request.id] is RequestState.Pending
        }

    override suspend fun record(groupId: String, event: PaymentTransfer): Result<Unit> {
        val request = PaymentRequest(IdGenerator.generate(prefix = "payment-request"), groupId, event)
        state.update { current ->
            val initial = if (current.contexts[groupId] == null) {
                RequestState.Completed(request, Result.failure(IllegalStateException("Payment context unavailable")))
            } else {
                RequestState.Pending(request)
            }
            current.copy(requests = current.requests + (request.id to initial))
        }
        try {
            if (state.value.requests[request.id] is RequestState.Pending) {
                channel(groupId).send(request)
            }
            return state.mapNotNull {
                (it.requests[request.id] as? RequestState.Completed)?.result
            }.first()
        } finally {
            state.update { it.copy(requests = it.requests - request.id) }
        }
    }

    override fun complete(request: PaymentRequest, result: Result<Unit>) {
        state.update { current ->
            val existing = current.requests[request.id]
            if (existing is RequestState.Pending && existing.request == request) {
                current.copy(requests = current.requests + (request.id to RequestState.Completed(request, result)))
            } else {
                current
            }
        }
    }

    private fun updateContext(groupId: String, context: PaymentContext?) {
        // Context invalidation and pending request failures are published atomically.
        state.update { current ->
            current.copy(
                contexts = current.contexts + (groupId to context),
                requests = if (context != null) {
                    current.requests
                } else {
                    current.requests.mapValues { (_, status) ->
                        if (status is RequestState.Pending && status.request.groupId == groupId) {
                            RequestState.Completed(status.request, Result.failure(IllegalStateException("Payment conversation closed")))
                        } else {
                            status
                        }
                    }
                }
            )
        }
    }

    private fun channel(groupId: String): Channel<PaymentRequest> =
        channels.updateAndGet {
            if (groupId in it) it else it + (groupId to Channel<PaymentRequest>(Channel.UNLIMITED))
        }.getValue(groupId)

    private data class ComposerState(
        val contexts: Map<String, PaymentContext?> = emptyMap(),
        val requests: Map<String, RequestState> = emptyMap()
    )

    private sealed interface RequestState {
        val request: PaymentRequest

        data class Pending(
            override val request: PaymentRequest
        ) : RequestState

        data class Completed(
            override val request: PaymentRequest,
            val result: Result<Unit>
        ) : RequestState
    }
}
