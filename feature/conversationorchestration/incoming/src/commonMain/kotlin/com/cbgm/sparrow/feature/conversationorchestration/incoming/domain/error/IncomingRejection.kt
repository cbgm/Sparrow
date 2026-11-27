package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.error

import com.cbgm.sparrow.protocol.handler.IncomingMessageRejectedException

/*
 * A packet that is not allowed or invalid can never succeed. It is rejected, so its envelope is acknowledged and
 * dropped instead of being redelivered until it expires and blocking the mailbox behind it. Failures that can
 * succeed later (database, network, a message that has not arrived yet) must keep throwing.
 */

/** Rejects the packet unless [value] holds. */
internal inline fun rejectUnless(value: Boolean, lazyMessage: () -> String) {
    if (!value) throw IncomingMessageRejectedException(lazyMessage())
}

/** Rejects the packet when [value] is missing. */
internal inline fun <T : Any> rejectIfNull(value: T?, lazyMessage: () -> String): T =
    value ?: throw IncomingMessageRejectedException(lazyMessage())

/** Rejects the packet when a requirement of [validation] fails. */
internal inline fun <T> rejectInvalid(validation: () -> T): T =
    try {
        validation()
    } catch (error: IllegalArgumentException) {
        throw IncomingMessageRejectedException(error.message ?: "Invalid incoming packet", error)
    }
