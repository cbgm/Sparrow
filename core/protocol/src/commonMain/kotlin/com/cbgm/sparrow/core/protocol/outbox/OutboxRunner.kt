package com.cbgm.sparrow.core.protocol.outbox

/**
 * Continuously watches the persistent outbox and triggers processing
 * whenever pending packets exist.
 */
interface OutboxRunner {
    /**
     * Starts observing the outbox.
     *
     * Calling start more than once must have no effect.
     */
    fun start()

    /**
     * Stops observing and processing the outbox.
     */
    fun stop()
}
