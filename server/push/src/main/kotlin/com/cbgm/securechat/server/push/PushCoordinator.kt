package com.cbgm.securechat.server.push

import com.cbgm.securechat.server.protocol.RelayEnvelope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DEFAULT_FALLBACK_DELAY_MILLISECONDS = 5_000L

class PushCoordinator(
    private val pendingEnvelopes: PendingEnvelopeStore,
    private val sender: FirebasePushSender,
    private val scope: CoroutineScope,
    private val fallbackDelayMilliseconds: Long = DEFAULT_FALLBACK_DELAY_MILLISECONDS
) {
    fun resumePendingNotifications() {
        scope.launch {
            delay(fallbackDelayMilliseconds)

            pendingEnvelopes
                .pendingRecipientIds()
                .forEach { recipientId ->
                    sender.notifyMessagesAvailable(recipientId)
                }
        }
    }

    suspend fun accept(envelope: RelayEnvelope): Boolean {
        val accepted = pendingEnvelopes.enqueue(envelope)
        if (!accepted) {
            return pendingEnvelopes.contains(envelope.envelopeId)
        }

        scope.launch {
            delay(fallbackDelayMilliseconds)
            if (pendingEnvelopes.contains(envelope.envelopeId)) {
                sender.notifyMessagesAvailable(envelope.recipientId)
            }
        }
        return true
    }

    suspend fun replicate(envelope: RelayEnvelope): Boolean {
        val accepted = pendingEnvelopes.enqueue(envelope)
        return accepted || pendingEnvelopes.contains(envelope.envelopeId)
    }

    suspend fun notifyRecipient(recipientId: String) {
        sender.notifyMessagesAvailable(recipientId)
    }
}
