package com.cbgm.sparrow.feature.transport.websocket

import com.cbgm.sparrow.feature.transport.gateway.model.GatewayBlobUploadTicket
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayEnvelopeAcceptance
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

internal class GatewayPendingRequestRegistry {
    private val envelopeAcceptanceMutex = Mutex()
    private val blobTicketMutex = Mutex()

    private val pendingEnvelopeAcceptances =
        mutableMapOf<String, CompletableDeferred<GatewayEnvelopeAcceptance>>()

    private val pendingBlobTickets =
        mutableMapOf<String, CompletableDeferred<GatewayBlobUploadTicket>>()

    suspend fun awaitEnvelopeAcceptance(
        envelopeId: String,
        timeoutMilliseconds: Long,
        send: suspend () -> Unit
    ): Result<GatewayEnvelopeAcceptance> =
        runCatching {
            require(timeoutMilliseconds > 0L) {
                "Acknowledgement timeout must be positive"
            }

            val deferred = CompletableDeferred<GatewayEnvelopeAcceptance>()
            envelopeAcceptanceMutex.withLock {
                check(pendingEnvelopeAcceptances.put(envelopeId, deferred) == null) {
                    "Envelope is already awaiting acknowledgement"
                }
            }

            try {
                send()
                // A missing acknowledgement is an ordinary send failure. A TimeoutCancellationException would
                // look like a cancellation to every caller and stop the outbox instead of failing one packet.
                withTimeoutOrNull(timeoutMilliseconds.milliseconds) { deferred.await() }
                    ?: throw GatewayAcknowledgementTimeoutException("Gateway did not acknowledge the envelope")
            } finally {
                envelopeAcceptanceMutex.withLock {
                    pendingEnvelopeAcceptances.remove(envelopeId)
                }
            }
        }

    suspend fun awaitBlobUploadTicket(
        requestId: String,
        timeoutMilliseconds: Long,
        send: suspend () -> Unit
    ): Result<GatewayBlobUploadTicket> =
        runCatching {
            require(timeoutMilliseconds > 0L) {
                "Blob upload ticket timeout must be positive"
            }

            val deferred = CompletableDeferred<GatewayBlobUploadTicket>()
            blobTicketMutex.withLock {
                check(pendingBlobTickets.put(requestId, deferred) == null) {
                    "Blob upload ticket request is already pending"
                }
            }

            try {
                send()
                withTimeoutOrNull(timeoutMilliseconds.milliseconds) { deferred.await() }
                    ?: throw GatewayAcknowledgementTimeoutException("Gateway did not issue a blob upload ticket")
            } finally {
                blobTicketMutex.withLock {
                    pendingBlobTickets.remove(requestId)
                }
            }
        }

    suspend fun completeEnvelopeAcceptance(acceptance: GatewayEnvelopeAcceptance) {
        envelopeAcceptanceMutex.withLock {
            pendingEnvelopeAcceptances[acceptance.envelopeId]
        }?.complete(acceptance)
    }

    /** The gateway refused this envelope; its sender learns it at once instead of after the timeout. */
    suspend fun rejectEnvelopeAcceptance(
        envelopeId: String,
        error: Throwable
    ) {
        envelopeAcceptanceMutex.withLock {
            pendingEnvelopeAcceptances[envelopeId]
        }?.completeExceptionally(error)
    }

    suspend fun completeBlobUploadTicket(ticket: GatewayBlobUploadTicket) {
        blobTicketMutex.withLock {
            pendingBlobTickets[ticket.requestId]
        }?.complete(ticket)
    }

    suspend fun rejectBlobUploadTicket(
        requestId: String,
        error: Throwable
    ) {
        blobTicketMutex.withLock {
            pendingBlobTickets[requestId]
        }?.completeExceptionally(error)
    }

    suspend fun failAll(error: Throwable) {
        val envelopeAcceptances =
            envelopeAcceptanceMutex.withLock {
                pendingEnvelopeAcceptances.values.toList().also {
                    pendingEnvelopeAcceptances.clear()
                }
            }

        val blobTickets =
            blobTicketMutex.withLock {
                pendingBlobTickets.values.toList().also {
                    pendingBlobTickets.clear()
                }
            }

        envelopeAcceptances.forEach { deferred ->
            deferred.completeExceptionally(error)
        }
        blobTickets.forEach { deferred ->
            deferred.completeExceptionally(error)
        }
    }
}

/** The gateway did not answer a request in time; an ordinary failure, never a cancellation. */
internal class GatewayAcknowledgementTimeoutException(
    message: String
) : IllegalStateException(message)
