package com.cbgm.sparrow.feature.conversationorchestration.runtime.outbox

/**
 * The packet was prepared securely but the transport could not confirm relay
 * acceptance. Its outbox row may be retried with its ORIGINAL destination/key;
 * this is not permission to change identities or downgrade encryption.
 *
 * The stable prefix is persisted in protocol_outbox.lastError so the generic
 * outbox can distinguish a failed wire send from a permanent preparation error.
 */
internal class RetryableWireDeliveryException(
    cause: Throwable
) : IllegalStateException("TRANSIENT_WIRE: relay acceptance was not confirmed", cause)
