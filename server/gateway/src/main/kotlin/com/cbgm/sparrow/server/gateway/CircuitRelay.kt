package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.security.CircuitHandshake
import com.cbgm.sparrow.server.security.CircuitHopKeys
import com.cbgm.sparrow.server.security.CircuitLayer
import com.cbgm.sparrow.server.security.X25519KeyPair
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

/**
 * One node's part in the app's circuits (`/v1/circuit`). Cells come in over the link from the app or the hop before;
 * a cell for an inner hop gets this hop's ChaCha20 layer removed and goes on to the next hop, a cell for this hop (it
 * is the circuit's current last hop) is opened and answered here. As the last hop it extends the circuit to the next
 * relay ([RelayCommand.EXTEND]) and answers pings; later stages add the session node's mailbox commands. It keeps and
 * logs nothing about the circuits.
 */
internal class CircuitRelay(
    private val keys: X25519KeyPair,
    private val httpClient: HttpClient,
    private val destinations: RelayDestinationPolicy,
    private val extendTimeoutMilliseconds: Long = EXTEND_TIMEOUT_MILLISECONDS
) {
    /** Serves one link until it closes; every circuit on it ends with it, and so do their onward links. */
    suspend fun handle(link: DefaultWebSocketSession) {
        val circuits = ConcurrentHashMap<Int, CircuitHop>()
        val inbound = Link(link)
        try {
            coroutineScope {
                for (frame in link.incoming) {
                    val cell = (frame as? Frame.Binary)?.let { binary -> CircuitCell.decode(binary.readBytes()) } ?: continue
                    when (cell.command) {
                        CircuitCell.CREATE -> create(cell, circuits, inbound)
                        CircuitCell.RELAY -> circuits[cell.circuitId]?.let { hop -> forward(hop, cell) }
                        CircuitCell.DESTROY -> circuits.remove(cell.circuitId)?.close()
                        else -> Unit
                    }
                }
            }
        } finally {
            circuits.values.forEach { hop -> hop.close() }
        }
    }

    private suspend fun create(
        cell: CircuitCell,
        circuits: ConcurrentHashMap<Int, CircuitHop>,
        inbound: Link
    ) {
        val reply =
            CircuitHandshake
                .respond(keys, cell.body.copyOfRange(0, CircuitHandshake.KEY_BYTES))
                ?.takeIf { circuits.size < MAXIMUM_CIRCUITS_PER_LINK && !circuits.containsKey(cell.circuitId) }
        when (reply) {
            null -> inbound.send(CircuitCell(cell.circuitId, CircuitCell.DESTROY, ByteArray(0)))
            else -> {
                circuits[cell.circuitId] = CircuitHop(cell.circuitId, reply.keys, inbound)
                inbound.send(CircuitCell(cell.circuitId, CircuitCell.CREATED, reply.reply))
            }
        }
    }

    private suspend fun CoroutineScope.forward(
        hop: CircuitHop,
        cell: CircuitCell
    ) {
        when (val next = hop.next) {
            null -> answer(hop, cell)
            else -> next.link.send(CircuitCell(next.circuitId, CircuitCell.RELAY, hop.peelForward(cell.body)))
        }
    }

    /** This hop is the circuit's last: the cell is for it. Opened in arrival order, since its count is the nonce. */
    private suspend fun CoroutineScope.answer(
        hop: CircuitHop,
        cell: CircuitCell
    ) {
        val request = hop.openForward(cell.body)?.let(RelayPayload::decode)
        when (request?.command) {
            null -> hop.destroy()
            RelayCommand.EXTEND -> launch { extend(hop, request) }
            RelayCommand.PING -> hop.reply(RelayPayload(RelayCommand.PONG, request.stream, request.data))
            else -> Unit
        }
    }

    private suspend fun extend(
        hop: CircuitHop,
        request: RelayPayload
    ) {
        val extension = ExtendRequest.decode(request.data)
        val target = extension?.let { destinations.nextCircuitHop(it.url) }
        val created =
            try {
                target?.let { url -> connect(url.toString(), extension.clientEphemeralKey) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            }
        when (created) {
            null -> hop.reply(RelayPayload(RelayCommand.EXTEND_FAILED, request.stream, ByteArray(0)))
            else -> {
                // Attached first: the app sends through the new hop as soon as it reads EXTENDED.
                hop.attach(created.first)
                hop.reply(RelayPayload(RelayCommand.EXTENDED, request.stream, created.second))
            }
        }
    }

    /** Opens the onward link and creates the circuit there: the link and the next relay's handshake reply. */
    private suspend fun connect(
        url: String,
        clientEphemeralKey: ByteArray
    ): Pair<Link, ByteArray>? {
        val session = httpClient.webSocketSession(url)
        val link = Link(session)
        link.send(CircuitCell(ONWARD_CIRCUIT_ID, CircuitCell.CREATE, clientEphemeralKey))
        val created =
            withTimeoutOrNull(extendTimeoutMilliseconds.milliseconds) {
                var answer: CircuitCell? = null
                while (answer == null) {
                    answer = (session.incoming.receive() as? Frame.Binary)?.let { frame -> CircuitCell.decode(frame.readBytes()) }
                }
                answer
            }
        return created
            ?.takeIf { cell -> cell.command == CircuitCell.CREATED && cell.circuitId == ONWARD_CIRCUIT_ID }
            ?.let { cell -> link to cell.body.copyOfRange(0, CircuitHandshake.REPLY_BYTES) }
            ?: run {
                session.close()
                null
            }
    }

    fun close() {
        httpClient.close()
    }

    private class CircuitHop(
        private val circuitId: Int,
        private val keys: CircuitHopKeys,
        private val inbound: Link
    ) {
        private val counters = Mutex()
        private var forwardCount = 0L
        private var backwardCount = 0L

        @Volatile var next: NextHop? = null
            private set

        suspend fun peelForward(body: ByteArray): ByteArray =
            counters.withLock { CircuitLayer.stream(keys.forward, forwardCount++, body) }

        suspend fun openForward(body: ByteArray): ByteArray? =
            counters.withLock { CircuitLayer.open(keys.forward, forwardCount++, body) }

        suspend fun reply(payload: RelayPayload) {
            val sealed = counters.withLock { CircuitLayer.seal(keys.backward, backwardCount++, payload.encode()) }
            inbound.send(CircuitCell(circuitId, CircuitCell.RELAY, sealed))
        }

        suspend fun destroy() {
            inbound.send(CircuitCell(circuitId, CircuitCell.DESTROY, ByteArray(0)))
            close()
        }

        /** Cells from the next hop get this hop's layer and go back towards the app. */
        fun attach(link: Link) {
            val reader =
                link.scope.launch {
                    try {
                        for (frame in link.session.incoming) {
                            val cell = (frame as? Frame.Binary)?.let { binary -> CircuitCell.decode(binary.readBytes()) } ?: continue
                            when (cell.command) {
                                CircuitCell.RELAY -> {
                                    val layered = counters.withLock { CircuitLayer.stream(keys.backward, backwardCount++, cell.body) }
                                    inbound.send(CircuitCell(circuitId, CircuitCell.RELAY, layered))
                                }
                                CircuitCell.DESTROY -> break
                                else -> Unit
                            }
                        }
                    } finally {
                        runCatching { inbound.send(CircuitCell(circuitId, CircuitCell.DESTROY, ByteArray(0))) }
                    }
                }
            next = NextHop(link, ONWARD_CIRCUIT_ID, reader)
        }

        suspend fun close() {
            next?.let { onward ->
                onward.reader.cancel()
                runCatching { onward.link.session.close() }
            }
            next = null
        }
    }

    private class NextHop(
        val link: Link,
        val circuitId: Int,
        val reader: Job
    )

    private class Link(
        val session: WebSocketSession
    ) {
        private val sending = Mutex()
        val scope: CoroutineScope get() = session

        suspend fun send(cell: CircuitCell) {
            sending.withLock { session.send(Frame.Binary(true, cell.encode())) }
        }
    }

    private companion object {
        const val EXTEND_TIMEOUT_MILLISECONDS = 10_000L
        const val MAXIMUM_CIRCUITS_PER_LINK = 16

        /** An onward link carries exactly one circuit for now. */
        const val ONWARD_CIRCUIT_ID = 1
    }
}

/** A fixed-size circuit cell: `circuit ID (4) || command (1) || body`, always [SIZE] bytes. */
internal class CircuitCell(
    val circuitId: Int,
    val command: Int,
    val body: ByteArray
) {
    fun encode(): ByteArray =
        ByteBuffer
            .allocate(SIZE)
            .putInt(circuitId)
            .put(command.toByte())
            .put(body.copyOf(BODY_BYTES))
            .array()

    companion object {
        const val SIZE = 1024
        const val BODY_BYTES = SIZE - 5
        const val CREATE = 1
        const val CREATED = 2
        const val RELAY = 3
        const val DESTROY = 4

        fun decode(frame: ByteArray): CircuitCell? =
            frame.takeIf { it.size == SIZE }?.let { bytes ->
                val buffer = ByteBuffer.wrap(bytes)
                CircuitCell(buffer.getInt(), buffer.get().toInt(), bytes.copyOfRange(5, SIZE))
            }
    }
}

/** What the last hop reads once it removed its layer: `command (1) || stream (2) || length (2) || data || zeros`. */
internal class RelayPayload(
    val command: Int,
    val stream: Int,
    val data: ByteArray
) {
    fun encode(): ByteArray {
        require(data.size <= MAXIMUM_DATA_BYTES) { "Relay data must fit one cell" }
        return ByteBuffer
            .allocate(PLAINTEXT_BYTES)
            .put(command.toByte())
            .putShort(stream.toShort())
            .putShort(data.size.toShort())
            .put(data)
            .array()
    }

    companion object {
        /** The body less the authentication tag of the last hop's seal. */
        const val PLAINTEXT_BYTES = CircuitCell.BODY_BYTES - CircuitLayer.TAG_BYTES
        const val MAXIMUM_DATA_BYTES = PLAINTEXT_BYTES - 5

        fun decode(plaintext: ByteArray): RelayPayload? {
            if (plaintext.size != PLAINTEXT_BYTES) return null
            val buffer = ByteBuffer.wrap(plaintext)
            val command = buffer.get().toInt()
            val stream = buffer.getShort().toInt() and 0xFFFF
            val length = buffer.getShort().toInt() and 0xFFFF
            return plaintext
                .takeIf { length <= MAXIMUM_DATA_BYTES }
                ?.let { RelayPayload(command, stream, it.copyOfRange(5, 5 + length)) }
        }
    }
}

internal object RelayCommand {
    const val EXTEND = 1
    const val EXTENDED = 2
    const val EXTEND_FAILED = 3
    const val PING = 4
    const val PONG = 5
}

/** EXTEND data: `url length (2) || url (UTF-8) || the app's ephemeral key for the next hop (32)`. */
internal class ExtendRequest(
    val url: String,
    val clientEphemeralKey: ByteArray
) {
    fun encode(): ByteArray {
        val urlBytes = url.encodeToByteArray()
        return ByteBuffer
            .allocate(2 + urlBytes.size + CircuitHandshake.KEY_BYTES)
            .putShort(urlBytes.size.toShort())
            .put(urlBytes)
            .put(clientEphemeralKey)
            .array()
    }

    companion object {
        fun decode(data: ByteArray): ExtendRequest? {
            if (data.size < 2) return null
            val length = ByteBuffer.wrap(data).getShort().toInt() and 0xFFFF
            return data
                .takeIf { it.size == 2 + length + CircuitHandshake.KEY_BYTES }
                ?.let { ExtendRequest(it.copyOfRange(2, 2 + length).decodeToString(), it.copyOfRange(2 + length, it.size)) }
        }
    }
}

/** `GET /v1/circuit` (WebSocket): one link from the app or another relay. */
internal fun Route.installCircuitRoute(relay: CircuitRelay) {
    webSocket("/v1/circuit") { relay.handle(this) }
}

internal fun createCircuitHttpClient(): HttpClient =
    HttpClient(CIO) {
        followRedirects = false
        install(WebSockets)
    }
