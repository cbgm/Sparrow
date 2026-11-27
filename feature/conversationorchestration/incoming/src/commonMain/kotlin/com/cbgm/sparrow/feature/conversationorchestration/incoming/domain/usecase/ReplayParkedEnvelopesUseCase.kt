package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.IncomingEnvelopeHandling
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.ParkedEnvelope
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.repository.ParkedEnvelopeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Processes the parked envelopes whose wait is met ([CheckDeferredPacketReadyUseCase]) again, in the order
 * they arrived, through [process] — the same path a new envelope takes. An envelope that was handled,
 * rejected or whose sender is unknown is removed. One that has to wait again was parked again under its
 * new wait by [process]. One that failed is kept for the next replay, like a mailbox envelope that was not
 * acknowledged. Replays run one at a time so two envelopes arriving together never replay the same one twice.
 */
class ReplayParkedEnvelopesUseCase(
    private val parkedEnvelopes: ParkedEnvelopeRepository,
    private val checkDeferredPacketReady: CheckDeferredPacketReadyUseCase
) {
    private val logger = SparrowLog.withTag("ReplayParkedEnvelopesUseCase")
    private val replayMutex = Mutex()

    /** Returns how many parked envelopes were settled and removed. */
    suspend operator fun invoke(process: suspend (ParkedEnvelope) -> IncomingEnvelopeHandling): Result<Int> =
        safeSuspendCall { replayMutex.withLock { replayReady(process) } }

    private suspend fun replayReady(process: suspend (ParkedEnvelope) -> IncomingEnvelopeHandling): Int {
        val ready =
            parkedEnvelopes.findWaits().getOrThrow().filter { wait -> checkDeferredPacketReady(wait).getOrThrow() }
        if (ready.isEmpty()) return 0
        return parkedEnvelopes.findWaitingFor(ready).getOrThrow().count { envelope -> replay(envelope, process) }
    }

    private suspend fun replay(
        envelope: ParkedEnvelope,
        process: suspend (ParkedEnvelope) -> IncomingEnvelopeHandling
    ): Boolean {
        val handling =
            try {
                process(envelope)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                logger.warn(error) { "Parked envelope ${envelope.envelopeId} failed and stays parked" }
                return false
            }
        if (handling == IncomingEnvelopeHandling.DEFERRED) return false
        parkedEnvelopes.remove(envelope.envelopeId).getOrThrow()
        return true
    }
}
