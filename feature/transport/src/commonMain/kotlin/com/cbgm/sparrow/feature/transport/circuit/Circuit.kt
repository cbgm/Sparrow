package com.cbgm.sparrow.feature.transport.circuit

import com.cbgm.sparrow.core.crypto.circuit.CircuitCipher
import com.cbgm.sparrow.core.crypto.circuit.CircuitHopKeys
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.transport.relay.RelayHop
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.milliseconds

/** The session node would not make a request (not a mailbox, too large); the circuit itself is fine. */
internal class CircuitRequestRefusedException : IllegalStateException("The session node refused the request")

/** A hop of the circuit could not be built or stopped passing cells on; [nodeId] is left out for a while. */
internal class CircuitHopFailedException(
    val nodeId: String,
    message: String
) : IllegalStateException(message)

/**
 * A circuit over the link [session] to its first relay (`/v1/circuit`). Cells for the current last hop are sealed for
 * it and get one ChaCha20 layer for every hop before it; answers come back the same way. While it is built, each
 * step waits for its own answer; once built, [readAnswers] hands every answer to the stream it belongs to, so many
 * requests can be on the way at once (a mailbox wait does not hold back sending or a ping).
 */
internal class Circuit(
    private val session: WebSocketSession,
    private val cipher: CircuitCipher,
    private val answerTimeoutMilliseconds: Long = DEFAULT_ANSWER_TIMEOUT_MILLISECONDS,
    private val now: () -> Long = SystemClock::nowEpochMilliseconds
) {
    private val hops = mutableListOf<Hop>()
    private val sending = Mutex()
    private val streams = Mutex()
    private val openStreams = mutableMapOf<Int, Channel<RelayPayload>>()
    private var nextStream = 1

    @Volatile var isClosed = false
        private set

    val nodeIds: List<String> get() = hops.map { hop -> hop.nodeId }

    /** The `/v1/circuit` address of the last hop, the session node that makes the requests. */
    val sessionNodeCircuitUrl: String? get() = hops.lastOrNull()?.circuitUrl

    /** Who runs the session node; it sees every mailbox requested through this circuit. */
    val sessionOperator: String? get() = hops.lastOrNull()?.operator

    suspend fun create(relay: RelayHop) {
        val ephemeral = cipher.newEphemeralKey()
        session.send(Frame.Binary(true, CircuitCell(CIRCUIT_ID, CircuitCell.CREATE, ephemeral.publicKey).encode()))
        val created = receiveCell(relay.nodeId)
        val keys =
            created
                .takeIf { cell -> cell.command == CircuitCell.CREATED }
                ?.let { cell -> cipher.completeHandshake(ephemeral, relay.relayKey, cell.body.copyOf(REPLY_BYTES)) }
                ?: throw CircuitHopFailedException(relay.nodeId, "circuit not created")
        hops += Hop(relay.nodeId, relay.circuitUrl, relay.operator, keys)
    }

    suspend fun extend(relay: RelayHop) {
        val ephemeral = cipher.newEphemeralKey()
        val answer = exchange(RelayPayload(RelayCommand.EXTEND, 0, extendRequest(relay.circuitUrl, ephemeral.publicKey)))
        val keys =
            answer
                .takeIf { payload -> payload.command == RelayCommand.EXTENDED }
                ?.let { payload -> cipher.completeHandshake(ephemeral, relay.relayKey, payload.data) }
                ?: throw CircuitHopFailedException(relay.nodeId, "not reachable as the next hop")
        hops += Hop(relay.nodeId, relay.circuitUrl, relay.operator, keys)
    }

    /** The round trip to the last hop and back, in milliseconds. Needs [readAnswers] running. */
    suspend fun ping(): Long {
        val sentAt = now()
        val answer =
            onStream { stream, answers ->
                sending.withLock { sendToLastHop(RelayPayload(RelayCommand.PING, stream, sentAt.toString().encodeToByteArray())) }
                answers.receive()
            }
        check(answer.command == RelayCommand.PONG) { "The circuit answered a ping with ${answer.command}" }
        return now() - sentAt
    }

    /**
     * The session node makes [request] for the app; its answer, or a failure when the mailbox node did not answer.
     * The request and the answer may span many cells. Needs [readAnswers] running.
     */
    suspend fun request(request: CircuitHttpRequest): CircuitHttpResponse =
        onStream { stream, answers ->
            val parts = streamParts(request.encode())
            sending.withLock {
                parts.dropLast(1).forEach { part -> sendToLastHop(RelayPayload(RelayCommand.STREAM_DATA, stream, part)) }
                sendToLastHop(RelayPayload(RelayCommand.STREAM_END, stream, parts.last()))
            }
            collectAnswer(answers)
        }

    /**
     * Reads the built circuit's answers and hands each to its stream, until the circuit fails or is cancelled; then
     * every request still waiting fails too.
     */
    suspend fun readAnswers() {
        var failure: Exception = IllegalStateException("The circuit is closed")
        try {
            while (true) {
                val answer = openAnswer(nextCell())
                streams.withLock { openStreams[answer.stream] }?.send(answer)
            }
        } catch (readFailure: Exception) {
            failure = readFailure
            throw readFailure
        } finally {
            isClosed = true
            withContext(NonCancellable) { streams.withLock { openStreams.values.forEach { waiting -> waiting.close(failure) } } }
        }
    }

    private suspend fun collectAnswer(answers: ReceiveChannel<RelayPayload>): CircuitHttpResponse {
        val parts = mutableListOf<ByteArray>()
        var part = answers.receive()
        while (part.command == RelayCommand.STREAM_DATA) {
            parts += part.data
            part = answers.receive()
        }
        parts += part.data
        val answer = join(parts)
        return when (part.command) {
            RelayCommand.STREAM_END -> CircuitHttpResponse.decode(answer) ?: error("The session node sent an unreadable answer")
            RelayCommand.STREAM_REFUSED -> throw CircuitRequestRefusedException()
            else -> error("The mailbox node did not answer the session node")
        }
    }

    suspend fun close() {
        isClosed = true
        runCatching { session.send(Frame.Binary(true, CircuitCell(CIRCUIT_ID, CircuitCell.DESTROY, ByteArray(0)).encode())) }
        runCatching { session.close() }
    }

    /** Opens a stream for [block] to send on and read its answers from, closed again afterwards; a silent circuit is blamed on its last hop. */
    private suspend fun <T> onStream(block: suspend (stream: Int, answers: ReceiveChannel<RelayPayload>) -> T): T {
        val (stream, answers) = openStream()
        return try {
            withTimeout(answerTimeoutMilliseconds.milliseconds) { block(stream, answers) }
        } catch (_: TimeoutCancellationException) {
            throw CircuitHopFailedException(hops.last().nodeId, "no answer within ${answerTimeoutMilliseconds / 1_000} s")
        } finally {
            withContext(NonCancellable) { streams.withLock { openStreams.remove(stream) } }
        }
    }

    private suspend fun openStream(): Pair<Int, Channel<RelayPayload>> =
        streams.withLock {
            check(!isClosed) { "The circuit is closed" }
            var stream = nextStream
            while (stream in openStreams) stream = stream % MAXIMUM_STREAM_ID + 1
            nextStream = stream % MAXIMUM_STREAM_ID + 1
            stream to Channel<RelayPayload>(Channel.UNLIMITED).also { channel -> openStreams[stream] = channel }
        }

    /** While the circuit is built: one step, one answer. */
    private suspend fun exchange(request: RelayPayload): RelayPayload {
        sending.withLock { sendToLastHop(request) }
        return openAnswer(receiveCell(hops.last().nodeId))
    }

    private suspend fun sendToLastHop(request: RelayPayload) {
        val last = hops.last()
        var body = cipher.seal(last.keys.forward, last.forwardCount++, request.encode())
        hops.dropLast(1).asReversed().forEach { hop -> body = cipher.stream(hop.keys.forward, hop.forwardCount++, body) }
        session.send(Frame.Binary(true, CircuitCell(CIRCUIT_ID, CircuitCell.RELAY, body).encode()))
    }

    private suspend fun openAnswer(cell: CircuitCell): RelayPayload {
        val last = hops.last()
        if (cell.command != CircuitCell.RELAY) throw CircuitHopFailedException(last.nodeId, "circuit closed")
        var answer = cell.body
        hops.dropLast(1).forEach { hop -> answer = cipher.stream(hop.keys.backward, hop.backwardCount++, answer) }
        return cipher
            .open(last.keys.backward, last.backwardCount++, answer)
            ?.let(RelayPayload::decode)
            ?: throw CircuitHopFailedException(last.nodeId, "answer did not open")
    }

    /** The next cell other than padding; a silent hop is blamed on [nodeId], the hop the cell was for. */
    private suspend fun receiveCell(nodeId: String): CircuitCell =
        try {
            withTimeout(answerTimeoutMilliseconds.milliseconds) { nextCell() }
        } catch (_: TimeoutCancellationException) {
            throw CircuitHopFailedException(nodeId, "no answer within ${answerTimeoutMilliseconds / 1_000} s")
        }

    private suspend fun nextCell(): CircuitCell {
        var cell: CircuitCell? = null
        while (cell == null) {
            cell =
                (session.incoming.receive() as? Frame.Binary)
                    ?.let { frame -> CircuitCell.decode(frame.readBytes()) }
                    ?.takeIf { decoded -> decoded.command != CircuitCell.PADDING }
        }
        return cell
    }

    private fun join(parts: List<ByteArray>): ByteArray {
        val joined = ByteArray(parts.sumOf(ByteArray::size))
        var offset = 0
        parts.forEach { part ->
            part.copyInto(joined, offset)
            offset += part.size
        }
        return joined
    }

    private class Hop(
        val nodeId: String,
        val circuitUrl: String,
        val operator: String,
        val keys: CircuitHopKeys
    ) {
        var forwardCount = 0L
        var backwardCount = 0L
    }

    private companion object {
        const val CIRCUIT_ID = 1
        const val REPLY_BYTES = 64
        const val DEFAULT_ANSWER_TIMEOUT_MILLISECONDS = 15_000L
        const val MAXIMUM_STREAM_ID = 0xFFFF
    }
}
