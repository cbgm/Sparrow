package com.cbgm.sparrow.feature.messaging.application.outbox

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.outbox.OutboxProcessor
import com.cbgm.sparrow.core.protocol.outbox.OutboxRunner
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DefaultOutboxRunner(
    private val protocolOutbox: ProtocolOutbox,
    private val outboxProcessor: OutboxProcessor
) : OutboxRunner {
    private val logger = SparrowLog.withTag("DefaultOutboxRunner")

    private val runnerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var observationJob: Job? = null
    private val drainJobs = mutableListOf<Job>()

    override fun start() {
        if (observationJob?.isActive != true) {
            observationJob =
                runnerScope.launch {
                    protocolOutbox
                        .observePending()
                        .collect { pendingItems ->
                            if (pendingItems.isNotEmpty()) {
                                ensureDrainWorkers()
                            }
                        }
                }
        }

        /*
         * start() is also the connection-available signal. It is called for
         * every successful transport connection, not only on process startup.
         *
         * Recover packets left in PROCESSING by a cancelled send/process
         * death and retry packets that failed while the transport was offline.
         */
        runnerScope.launch {
            runCatching {
                protocolOutbox.requeueInterrupted().getOrThrow()
                protocolOutbox.retryFailed().getOrThrow()
            }.onFailure { error ->
                if (error is CancellationException) {
                    throw error
                }

                logger.error(error) { "Outbox recovery failed" }
            }
        }
    }

    override fun stop() {
        observationJob?.cancel()
        observationJob = null
        drainJobs.forEach { job -> job.cancel() }
        drainJobs.clear()
    }

    private fun ensureDrainWorkers() {
        drainJobs.removeAll { job -> !job.isActive }
        repeat(MAX_CONCURRENT_DRAIN_WORKERS - drainJobs.size) {
            drainJobs +=
                runnerScope.launch {
                    processAvailableItems()
                }
        }
    }

    private suspend fun processAvailableItems() {
        while (true) {
            val result = outboxProcessor.processPending(limit = 1)

            if (result.isFailure) {
                val error = result.exceptionOrNull()

                if (error is CancellationException) {
                    throw error
                }

                return
            }

            if (result.getOrThrow().processedCount == 0) {
                return
            }
        }
    }

    private companion object {
        const val MAX_CONCURRENT_DRAIN_WORKERS = 8
    }
}
