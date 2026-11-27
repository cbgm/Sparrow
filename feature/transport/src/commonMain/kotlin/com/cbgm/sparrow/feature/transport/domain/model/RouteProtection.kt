package com.cbgm.sparrow.feature.transport.domain.model

/** How well the route to the mailboxes protects the user right now. */
enum class RouteProtection {
    /** Every circuit is up through the chosen hops, or the first ones are still being built. */
    PROTECTED,

    /** Fewer hops than chosen, or no circuit while messages wait for one. */
    REDUCED,

    /** No circuit, and requests go out directly: the mailbox node sees the device's address. */
    DIRECT
}
