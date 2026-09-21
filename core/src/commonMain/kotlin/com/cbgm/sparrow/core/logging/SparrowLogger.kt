package com.cbgm.sparrow.core.logging

import co.touchlab.kermit.Logger
import com.cbgm.sparrow.core.time.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

interface SparrowLogger {
    fun debug(message: () -> String)

    fun info(message: () -> String)

    fun warn(throwable: Throwable? = null, message: () -> String)

    fun error(throwable: Throwable? = null, message: () -> String)
}

/** The single reporting entry point, independent of ViewModels and UI lifecycle. */
object SparrowLog {
    @Volatile
    private var errorSink: SparrowErrorSink? = null

    // Two independent one-consumer queues: no replay=0 loss when navigation is stopped,
    // and repeated events are not conflated. AppNavigation is the only collector.
    private val errorQueue = Channel<String>(Channel.UNLIMITED)
    private val hintQueue = Channel<String>(Channel.UNLIMITED)
    val errors: Flow<String> = errorQueue.receiveAsFlow()
    val hints: Flow<String> = hintQueue.receiveAsFlow()

    // An error logged before Koin installs the persistent sink must still reach it.
    // A failed sink call is retried rather than discarding this record.
    private data class PendingError(
        val tag: String,
        val timestamp: Long,
        val message: String,
        val throwable: Throwable?
    )

    private val pendingErrors = Channel<PendingError>(Channel.UNLIMITED)
    private val sinkInstalled = Channel<Unit>(Channel.CONFLATED)
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        persistenceScope.launch {
            for (entry in pendingErrors) {
                while (true) {
                    val sink = errorSink
                    if (sink == null) {
                        sinkInstalled.receive()
                        continue
                    }
                    try {
                        sink.record(
                            tag = entry.tag,
                            timestampEpochMilliseconds = entry.timestamp,
                            message = entry.message,
                            throwable = entry.throwable
                        )
                        break
                    } catch (failure: Exception) {
                        // Do NOT log through SparrowLog here: that would recurse.
                        logErrorToPlatform("SparrowLog", "Error-log sink rejected a record; retrying", failure)
                        delay(PERSISTENCE_RETRY_MILLIS.milliseconds)
                    }
                }
            }
        }
    }

    /** A reported failure appears in the global snackbar AND the developer error log. */
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        withTag(tag).error(throwable) { message }
    }

    /** Informational feedback is not an error and does not pollute the developer error log. */
    fun hint(message: String) {
        if (message.isNotBlank()) hintQueue.trySend(message)
    }

    fun withTag(tag: String): SparrowLogger {
        require(tag.isNotBlank()) { "Logger tag must not be blank" }
        return KermitSparrowLogger(tag = tag, delegate = Logger.withTag(tag))
    }

    /** An error in error-log storage itself cannot be written through the failing sink. */
    fun reportErrorStorageFailure(failure: Throwable) {
        logErrorToPlatform("SparrowLog", "Developer error log storage failed", failure)
        errorQueue.trySend("SparrowLog: Developer error log storage failed")
    }

    fun installErrorSink(sink: SparrowErrorSink) {
        errorSink = sink
        sinkInstalled.trySend(Unit)
    }

    internal fun recordError(tag: String, message: String, throwable: Throwable?) {
        val resolved = message.ifBlank { throwable?.message?.takeIf(String::isNotBlank) ?: "Unexpected error" }
        val event = PendingError(tag, SystemClock.nowEpochMilliseconds(), resolved, throwable)
        // Logcat uses the full message and the original throwable (including its stack trace).
        // It is independent of the snackbar's truncated display text and the database sink.
        // Even a broken platform logger must not prevent UI or persistence delivery.
        try {
            logErrorToPlatform(tag, resolved, throwable)
        } finally {
            pendingErrors.trySend(event)
            errorQueue.trySend("$tag: ${resolved.take(MAX_SNACKBAR_LENGTH)}")
        }
    }

    private const val MAX_SNACKBAR_LENGTH = 300
    private const val PERSISTENCE_RETRY_MILLIS = 2_000L
}

private class KermitSparrowLogger(
    private val tag: String,
    private val delegate: Logger
) : SparrowLogger {
    override fun debug(message: () -> String) = delegate.d(message = message)

    override fun info(message: () -> String) = delegate.i(message = message)

    override fun warn(throwable: Throwable?, message: () -> String) {
        val resolvedMessage = message()
        try {
            delegate.w(throwable = throwable, message = { resolvedMessage })
        } finally {
            // A warning with a real exception is still a failure, even when the
            // caller recovered. Do not lose its cause from the developer log.
            if (throwable != null) {
                SparrowLog.recordError(tag = tag, message = resolvedMessage, throwable = throwable)
            }
        }
    }

    override fun error(throwable: Throwable?, message: () -> String) {
        val resolvedMessage = message()
        // recordError writes directly to the platform error logger and independently
        // forwards the same error to the developer log and the global snackbar.
        SparrowLog.recordError(tag = tag, message = resolvedMessage, throwable = throwable)
    }
}
