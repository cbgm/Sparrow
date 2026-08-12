package com.cbgm.securechat.server.gateway

import com.cbgm.securechat.server.protocol.GatewayServerMessage
import com.cbgm.securechat.server.protocol.RelayEnvelope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal class GatewayPushDispatcher(
    private val pushClient: LegacyPushClient,
    private val markFederationStored: suspend (String) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob())
    private val slots = Semaphore(MAX_CONCURRENT_PUSH_OPERATIONS)

    fun scheduleFallback(
        envelope: RelayEnvelope,
        federationEnvelopeId: String
    ) {
        launchPushOperation {
            val stored = pushClient.store(envelope)
            if (stored) {
                markFederationStored(federationEnvelopeId)
            }
        }
    }

    fun deliverPending(connection: GatewayConnection) {
        launchPushOperation {
            pushClient
                .pending(recipientId = connection.routingId)
                .forEach { envelope ->
                    connection.send(
                        GatewayServerMessage.IncomingEnvelope(
                            envelope = envelope
                        )
                    )
                }
        }
    }

    fun acknowledge(
        recipientId: String,
        envelopeId: String
    ) {
        launchPushOperation {
            pushClient.acknowledge(
                recipientId = recipientId,
                envelopeId = envelopeId
            )
        }
    }

    fun close() {
        scope.cancel()
    }

    private fun launchPushOperation(operation: suspend () -> Unit) {
        scope.launch {
            slots.withPermit {
                runCatching {
                    operation()
                }
            }
        }
    }

    private companion object {
        const val MAX_CONCURRENT_PUSH_OPERATIONS = 8
    }
}
