package com.cbgm.sparrow.feature.transport.domain.model

object CircuitCount {
    const val MINIMUM = 1
    const val MAXIMUM = 3
    const val DEFAULT = 2
}

object CircuitHopCount {
    const val MINIMUM = 1
    const val MAXIMUM = 5
    const val DEFAULT = 2

    /** The fewest hops that hide the device's address from the node that makes a request for it. */
    const val PROTECTING = 2
}

object DecoyCount {
    const val MINIMUM = 0
    const val MAXIMUM = 5
    const val DEFAULT = 0
}
