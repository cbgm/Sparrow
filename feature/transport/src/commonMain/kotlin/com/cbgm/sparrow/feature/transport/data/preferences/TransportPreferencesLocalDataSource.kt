package com.cbgm.sparrow.feature.transport.data.preferences

import com.cbgm.sparrow.feature.transport.domain.model.CircuitCount
import com.cbgm.sparrow.feature.transport.domain.model.CircuitHopCount
import com.cbgm.sparrow.feature.transport.domain.model.ConnectionMode
import com.cbgm.sparrow.feature.transport.domain.model.DecoyCount
import kotlinx.coroutines.flow.Flow

/** The user's choices for how the app reaches the network, kept on the device; numbers stay within their bounds. */
internal interface TransportPreferencesLocalDataSource {
    /**
     * How many circuits the app keeps open at once, [CircuitCount.MINIMUM] to [CircuitCount.MAXIMUM]. Contacts are
     * spread across them, so one session node does not see all of the device's mailboxes together.
     */
    fun observeCircuitCount(): Flow<Int>

    suspend fun setCircuitCount(circuitCount: Int)

    /** How many hops the circuits have, [CircuitHopCount.MINIMUM] to [CircuitHopCount.MAXIMUM]. */
    fun observeHopCount(): Flow<Int>

    suspend fun setHopCount(hopCount: Int)

    /**
     * How many decoy envelopes go out with each send, [DecoyCount.MINIMUM] (off, the default) to [DecoyCount.MAXIMUM]:
     * they go into other contacts' mailboxes at the same moment, so a mailbox node cannot tell from timing which
     * mailboxes a send was meant for.
     */
    fun observeDecoyCount(): Flow<Int>

    suspend fun setDecoyCount(decoyCount: Int)

    /**
     * Whether a request that should go through the circuit is sent directly while there is none (the default: messages
     * keep working, but that server sees the device's address), or waits for one.
     */
    fun observeSendsDirectly(): Flow<Boolean>

    suspend fun setSendsDirectly(sendsDirectly: Boolean)

    /** The one gateway node the user prefers; null means automatic selection. */
    fun observePreferredNode(): Flow<String?>

    suspend fun setPreferredNode(nodeId: String?)

    /** How messages reach this device while Sparrow is closed; until the user chooses, only while it is open. */
    fun observeConnectionMode(): Flow<ConnectionMode>

    suspend fun setConnectionMode(mode: ConnectionMode)
}
