package com.cbgm.sparrow.core.protocol.handler

/**
 * Signals that an incoming transport message is permanently invalid and must not be retried.
 *
 * Transient failures such as database, network, or runtime errors must not use this exception.
 */
class IncomingMessageRejectedException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
