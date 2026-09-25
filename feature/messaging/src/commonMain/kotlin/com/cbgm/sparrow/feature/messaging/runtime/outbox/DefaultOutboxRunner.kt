package com.cbgm.sparrow.feature.messaging.runtime.outbox

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.outbox.OutboxProcessor
import com.cbgm.sparrow.core.protocol.outbox.OutboxRunner
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.time.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

class DefaultOutboxRunner(
    private val protocolOutbox: ProtocolOutbox,
    private val outboxProcessor: OutboxProcessor,
    private val retryIntervalMilliseconds: Long = 15_000L
) : OutboxRunner {
    private val logger = SparrowLog.withTag("DefaultOutboxRunner")

    private val runnerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val processingMutex = Mutex()

    init {
        require(retryIntervalMilliseconds > 0L)
    }

    private var recoveryJob: Job? = null

    // Requeue PROCESSING rows only when starting a new runner lifecycle. Calling
    // start() again for a new transport connection must not requeue a packet
    // currently being sent by the existing observation/recovery coroutine.
    // On a real process restart this flag is initialized to true again.
    @Volatile private var needsInterruptedRecovery = true

    @Volatile private var lifecycleEpoch = 0L

    // The pending-flow collector must not race first-start interrupted recovery.
    // Otherwise it could move a packet to PROCESSING just as the recovery task
    // requeues every PROCESSING row, permitting a second concurrent send.
    private var initialRecoveryComplete = CompletableDeferred<Unit>()

    private var transientRetryJob: Job? = null

    private var observationJob: Job? = null

    private var expiryJob: Job? = null

    override fun start() {
        if (observationJob?.isActive != true) {
            observationJob =
                runnerScope.launch {
                    protocolOutbox
                        .observePending()
                        .collect { pendingItems ->
                            initialRecoveryComplete.await()
                            if (pendingItems.isNotEmpty()) {
                                processAvailableItems()
                            }
                        }
                }
        }

        if (expiryJob?.isActive != true) {
            expiryJob =
                runnerScope.launch {
                    protocolOutbox
                        .observeNextSentExpiry()
                        .collectLatest { expiresAtEpochMilliseconds ->
                            if (expiresAtEpochMilliseconds == null) {
                                return@collectLatest
                            }

                            val remainingMilliseconds =
                                (expiresAtEpochMilliseconds - SystemClock.nowEpochMilliseconds())
                                    .coerceAtLeast(0L)
                            if (remainingMilliseconds > 0L) {
                                delay(remainingMilliseconds.milliseconds)
                            }

                            outboxProcessor
                                .expireAccepted()
                                .onFailure { error ->
                                    if (error is CancellationException) {
                                        throw error
                                    }
                                    logger.error(error) { "Accepted-envelope expiry processing failed" }
                                }
                        }
                }
        }

        if (transientRetryJob?.isActive != true) {
            transientRetryJob = runnerScope.launch {
                while (isActive) {
                    delay(retryIntervalMilliseconds.milliseconds)
                    protocolOutbox.retryTransientFailed(SystemClock.nowEpochMilliseconds())
                        .onFailure { error ->
                            if (error is CancellationException) throw error
                            logger.error(error) { "Retrying due transport failures failed" }
                        }
                }
            }
        }

        /*
         * start() also signals a successful transport reconnection. Do not cancel
         * an in-flight recovery/send just because another connection event arrived:
         * requeueing PROCESSING here can send the same packet concurrently twice.
         * A new connection may, however, retry TRANSIENT_WIRE failures immediately.
         */
        if (recoveryJob?.isActive == true) return
        val epochAtStart = lifecycleEpoch
        val readyForThisLifecycle = initialRecoveryComplete
        recoveryJob = runnerScope.launch {
            try {
                processingMutex.withLock {
                    if (needsInterruptedRecovery) {
                        protocolOutbox.requeueInterrupted().getOrThrow()
                        if (lifecycleEpoch == epochAtStart) {
                            needsInterruptedRecovery = false
                        }
                    }
                    protocolOutbox.retryFailed().getOrThrow()
                    processAvailableItemsLocked()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                logger.error(error) { "Outbox recovery failed" }
            } finally {
                readyForThisLifecycle.complete(Unit)
            }
        }
    }

    override fun stop() {
        lifecycleEpoch += 1L
        needsInterruptedRecovery = true
        initialRecoveryComplete = CompletableDeferred()
        transientRetryJob?.cancel()
        transientRetryJob = null
        recoveryJob?.cancel()
        recoveryJob = null
        observationJob?.cancel()
        observationJob = null
        expiryJob?.cancel()
        expiryJob = null
    }

    private suspend fun processAvailableItems() {
        processingMutex.withLock { processAvailableItemsLocked() }
    }

    private suspend fun processAvailableItemsLocked() {
        while (true) {
            val result = outboxProcessor.processPending(limit = PROCESSING_BATCH_SIZE)

            if (result.isFailure) {
                val error = result.exceptionOrNull()
                if (error is CancellationException) throw error
                return
            }

            val processingResult = result.getOrThrow()
            if (processingResult.processedCount == 0 ||
                processingResult.processedCount < PROCESSING_BATCH_SIZE
            ) {
                return
            }
        }
    }

    private companion object {
        const val PROCESSING_BATCH_SIZE = 20
    }
}
