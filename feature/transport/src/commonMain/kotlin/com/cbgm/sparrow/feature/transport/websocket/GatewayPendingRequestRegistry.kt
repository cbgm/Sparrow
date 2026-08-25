package com.cbgm.sparrow.feature.transport.websocket

import com.cbgm.sparrow.feature.transport.gateway.model.GatewayBlobUploadTicket
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayEnvelopeAcceptance
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
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
                withTimeout(timeoutMilliseconds.milliseconds) {
                    deferred.await()
                }
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
                withTimeout(timeoutMilliseconds.milliseconds) {
                    deferred.await()
                }
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
