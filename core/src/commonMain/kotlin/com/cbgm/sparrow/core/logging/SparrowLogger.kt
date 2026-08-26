package com.cbgm.sparrow.core.logging

import co.touchlab.kermit.Logger
import com.cbgm.sparrow.core.time.SystemClock

interface SparrowLogger {
    fun debug(message: () -> String)

    fun info(message: () -> String)

    fun warn(
        throwable: Throwable? = null,
        message: () -> String
    )

    fun error(
        throwable: Throwable? = null,
        message: () -> String
    )
}

object SparrowLog {
    @Volatile
    private var errorSink: SparrowErrorSink? = null

    fun withTag(tag: String): SparrowLogger {
        require(tag.isNotBlank()) {
            "Logger tag must not be blank"
        }

        return KermitSparrowLogger(
            tag = tag,
            delegate = Logger.withTag(tag)
        )
    }

    fun installErrorSink(sink: SparrowErrorSink) {
        errorSink = sink
    }

    internal fun recordError(
        tag: String,
        message: String,
        throwable: Throwable?
    ) {
        runCatching {
            errorSink?.record(
                tag = tag,
                timestampEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                message = message,
                throwable = throwable
            )
        }
    }
}

private class KermitSparrowLogger(
    private val tag: String,
    private val delegate: Logger
) : SparrowLogger {
    override fun debug(message: () -> String) {
        delegate.d(message = message)
    }

    override fun info(message: () -> String) {
        delegate.i(message = message)
    }

    override fun warn(
        throwable: Throwable?,
        message: () -> String
    ) {
        delegate.w(
            throwable = throwable,
            message = message
        )
    }

    override fun error(
        throwable: Throwable?,
        message: () -> String
    ) {
        val resolvedMessage = message()

        delegate.e(
            throwable = throwable,
            message = { resolvedMessage }
        )

        SparrowLog.recordError(
            tag = tag,
            message = resolvedMessage,
            throwable = throwable
        )
    }
}
