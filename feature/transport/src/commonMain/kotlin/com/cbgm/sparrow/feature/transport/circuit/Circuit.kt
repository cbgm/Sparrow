package com.cbgm.sparrow.feature.transport.circuit

import com.cbgm.sparrow.core.crypto.circuit.CircuitCipher
import com.cbgm.sparrow.core.crypto.circuit.CircuitHopKeys
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.transport.relay.RelayHop
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

/** A hop of the circuit could not be built or stopped passing cells on; [nodeId] is left out for a while. */
internal class CircuitHopFailedException(
    val nodeId: String,
    message: String
) : IllegalStateException(message)

/**
 * A circuit over the link [session] to its first relay (`/v1/circuit`). Cells for the current last hop are sealed for
 * it and get one ChaCha20 layer for every hop before it; answers come back the same way. For now one request is on
 * the way at a time, so an answer always belongs to the last request.
 */
internal class Circuit(
    private val session: WebSocketSession,
    private val cipher: CircuitCipher,
    private val answerTimeoutMilliseconds: Long = DEFAULT_ANSWER_TIMEOUT_MILLISECONDS,
    private val now: () -> Long = SystemClock::nowEpochMilliseconds
) {
    private val hops = mutableListOf<Hop>()
    private val exchanging = Mutex()

    val nodeIds: List<String> get() = hops.map { hop -> hop.nodeId }

    suspend fun create(relay: RelayHop) {
        val ephemeral = cipher.newEphemeralKey()
        session.send(Frame.Binary(true, CircuitCell(CIRCUIT_ID, CircuitCell.CREATE, ephemeral.publicKey).encode()))
        val created = receiveCell(relay.nodeId)
        val keys =
            created
                .takeIf { cell -> cell.command == CircuitCell.CREATED }
                ?.let { cell -> cipher.completeHandshake(ephemeral, relay.relayKey, cell.body.copyOf(REPLY_BYTES)) }
                ?: throw CircuitHopFailedException(relay.nodeId, "circuit not created")
        hops += Hop(relay.nodeId, keys)
    }

    suspend fun extend(relay: RelayHop) {
        val ephemeral = cipher.newEphemeralKey()
        val answer = exchange(RelayPayload(RelayCommand.EXTEND, 0, extendRequest(relay.circuitUrl, ephemeral.publicKey)))
        val keys =
            answer
                .takeIf { payload -> payload.command == RelayCommand.EXTENDED }
                ?.let { payload -> cipher.completeHandshake(ephemeral, relay.relayKey, payload.data) }
                ?: throw CircuitHopFailedException(relay.nodeId, "not reachable as the next hop")
        hops += Hop(relay.nodeId, keys)
    }

    /** The round trip to the last hop and back, in milliseconds. */
    suspend fun ping(): Long {
        val sentAt = now()
        val answer = exchange(RelayPayload(RelayCommand.PING, 0, sentAt.toString().encodeToByteArray()))
        check(answer.command == RelayCommand.PONG) { "The circuit answered a ping with ${answer.command}" }
        return now() - sentAt
    }

    suspend fun close() {
        runCatching { session.send(Frame.Binary(true, CircuitCell(CIRCUIT_ID, CircuitCell.DESTROY, ByteArray(0)).encode())) }
        runCatching { session.close() }
    }

    private suspend fun exchange(request: RelayPayload): RelayPayload =
        exchanging.withLock {
            val last = hops.last()
            var body = cipher.seal(last.keys.forward, last.forwardCount++, request.encode())
            hops.dropLast(1).asReversed().forEach { hop -> body = cipher.stream(hop.keys.forward, hop.forwardCount++, body) }
            session.send(Frame.Binary(true, CircuitCell(CIRCUIT_ID, CircuitCell.RELAY, body).encode()))

            val cell = receiveCell(last.nodeId)
            if (cell.command != CircuitCell.RELAY) throw CircuitHopFailedException(last.nodeId, "circuit closed")
            var answer = cell.body
            hops.dropLast(1).forEach { hop -> answer = cipher.stream(hop.keys.backward, hop.backwardCount++, answer) }
            cipher
                .open(last.keys.backward, last.backwardCount++, answer)
                ?.let(RelayPayload::decode)
                ?: throw CircuitHopFailedException(last.nodeId, "answer did not open")
        }

    /** The next cell other than padding; a silent hop is blamed on [nodeId], the hop the cell was for. */
    private suspend fun receiveCell(nodeId: String): CircuitCell =
        try {
            withTimeout(answerTimeoutMilliseconds.milliseconds) {
                var cell: CircuitCell? = null
                while (cell == null) {
                    cell =
                        (session.incoming.receive() as? Frame.Binary)
                            ?.let { frame -> CircuitCell.decode(frame.readBytes()) }
                            ?.takeIf { decoded -> decoded.command != CircuitCell.PADDING }
                }
                cell
            }
        } catch (_: TimeoutCancellationException) {
            throw CircuitHopFailedException(nodeId, "no answer within ${answerTimeoutMilliseconds / 1_000} s")
        }

    private class Hop(
        val nodeId: String,
        val keys: CircuitHopKeys
    ) {
        var forwardCount = 0L
        var backwardCount = 0L
    }

    private companion object {
        const val CIRCUIT_ID = 1
        const val REPLY_BYTES = 64
        const val DEFAULT_ANSWER_TIMEOUT_MILLISECONDS = 15_000L
    }
}
