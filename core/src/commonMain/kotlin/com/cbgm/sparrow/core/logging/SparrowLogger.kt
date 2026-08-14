package com.cbgm.sparrow.core.logging

import co.touchlab.kermit.Logger

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
    fun withTag(tag: String): SparrowLogger {
        require(tag.isNotBlank()) {
            "Logger tag must not be blank"
        }

        return KermitSparrowLogger(
            delegate = Logger.withTag(tag)
        )
    }
}

private class KermitSparrowLogger(
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
        delegate.e(
            throwable = throwable,
            message = message
        )
    }
}
