package com.cbgm.securechat.feature.messaging.application.outbox

import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.core.protocol.outbox.OutboxProcessor
import com.cbgm.securechat.core.protocol.outbox.OutboxRunner
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultOutboxRunner(
    private val protocolOutbox: ProtocolOutbox,
    private val outboxProcessor: OutboxProcessor
) : OutboxRunner {
    private val logger = SecureChatLog.withTag("DefaultOutboxRunner")

    private val runnerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val processingMutex = Mutex()

    private var observationJob: Job? = null

    override fun start() {
        if (observationJob?.isActive != true) {
            observationJob =
                runnerScope.launch {
                    protocolOutbox
                        .observePending()
                        .collect { pendingItems ->
                            if (pendingItems.isNotEmpty()) {
                                processAvailableItems()
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
                processAvailableItems()
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
    }

    private suspend fun processAvailableItems() {
        processingMutex.withLock {
            while (true) {
                val result = outboxProcessor.processPending(limit = PROCESSING_BATCH_SIZE)

                if (result.isFailure) {
                    val error = result.exceptionOrNull()

                    if (error is CancellationException) {
                        throw error
                    }

                    return
                }

                val processingResult = result.getOrThrow()

                if (processingResult.processedCount == 0) {
                    return
                }

                if (processingResult.processedCount < PROCESSING_BATCH_SIZE) {
                    return
                }
            }
        }
    }

    private companion object {
        const val PROCESSING_BATCH_SIZE = 20
    }
}
