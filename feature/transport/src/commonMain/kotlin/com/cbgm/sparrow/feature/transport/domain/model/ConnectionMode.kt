package com.cbgm.sparrow.feature.transport.domain.model

/** How messages reach this device while Sparrow is closed; Sparrow uses no push service. */
enum class ConnectionMode {
    /** A foreground service keeps the circuits open; messages arrive at once. */
    ALWAYS_CONNECTED,

    /** Messages wait in the mailboxes until the app is opened. */
    WHILE_OPEN,

    /** Android checks the mailboxes now and then (every 15 minutes at most). */
    CHECK_PERIODICALLY
}
