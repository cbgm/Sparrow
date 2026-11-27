package com.cbgm.sparrow.protocol.handler

import com.cbgm.sparrow.core.util.result.ExpectedFailure

/**
 * Signals that an incoming transport message is permanently invalid and must not be retried.
 *
 * Transient failures such as database, network, or runtime errors must not use this exception.
 * A rejection is an expected outcome: its envelope is dropped, and it is not reported as an error.
 */
class IncomingMessageRejectedException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause),
    ExpectedFailure
