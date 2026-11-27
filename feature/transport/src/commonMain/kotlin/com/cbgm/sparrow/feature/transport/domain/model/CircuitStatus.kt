package com.cbgm.sparrow.feature.transport.domain.model

/** Where the app's circuit stands; [Connected.latencyMilliseconds] is the last measured round trip. */
sealed interface CircuitStatus {
    data object Building : CircuitStatus

    /**
     * [circuitsUp] of the [requestedCircuitCount] circuits are open and end at [sessionOperators] different operators;
     * the hops and round trip are the best one's.
     */
    data class Connected(
        val hopCount: Int,
        val requestedHopCount: Int,
        val latencyMilliseconds: Long,
        val circuitsUp: Int = 1,
        val requestedCircuitCount: Int = 1,
        val sessionOperators: Int = 1
    ) : CircuitStatus

    data class Unavailable(
        val requestedHopCount: Int
    ) : CircuitStatus
}
