package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model

/** What happens to a direct message or operation of a contact. */
enum class DirectPeerAccess {
    /** The identity exchange is established: the packet is handled. */
    AUTHORIZED,

    /** The exchange is still in flight: the packet waits until it completes or fails. */
    PENDING,

    /** Unknown, blocked or revoked: the packet is dropped. */
    DROPPED
}
