package com.cbgm.sparrow.feature.conversationorchestration.runtime

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.conversationorchestration.runtime.outbox.InvitationTransportFailureHandler
import com.cbgm.sparrow.feature.messaging.domain.usecase.AcknowledgeMessagingFailureUseCase
import com.cbgm.sparrow.feature.messaging.domain.usecase.ObserveMessagingFailureEventsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.milliseconds

/** Application-specific recovery of durable, attempt-scoped Messaging failures. */
class MessagingTransportResultObserver internal constructor(
    private val observeFailures: ObserveMessagingFailureEventsUseCase,
    private val acknowledgeFailure: AcknowledgeMessagingFailureUseCase,
    private val invitationFailureHandler: InvitationTransportFailureHandler
) {
    private val logger = SparrowLog.withTag("MessagingTransportResultObserver")

    suspend fun run() {
        while (true) {
            // Suspends when the journal is empty, resumes for every persisted attempt.
            val failures = observeFailures().first { events -> events.isNotEmpty() }
            var encounteredFailure = false
            failures.forEach { event ->
                try {
                    invitationFailureHandler.onFailed(event.encodedPacket).getOrThrow()
                    // Acknowledge only after the application-side action succeeded.
                    acknowledgeFailure(event.eventId).getOrThrow()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    encounteredFailure = true
                    logger.error(error) {
                        "Failed to reconcile outgoing transport failure: packetId=${event.packetId}, attempt=${event.attemptCount}"
                    }
                }
            }
            // Avoid spinning if a handler/DB error leaves a journal entry unacknowledged.
            if (encounteredFailure) delay(RETRY_DELAY_MILLISECONDS.milliseconds)
        }
    }

    private companion object {
        const val RETRY_DELAY_MILLISECONDS = 5_000L
    }
}
