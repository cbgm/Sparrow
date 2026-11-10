package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.security.CircuitHandshake
import com.cbgm.sparrow.server.security.CircuitHopKeys
import com.cbgm.sparrow.server.security.CircuitLayer
import com.cbgm.sparrow.server.security.X25519KeyPair
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.utils.io.readBuffer
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
import kotlinx.io.readByteArray
import java.io.ByteArrayOutputStream
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
    private val exitClient: HttpClient = createRelayHttpClient(),
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
            RelayCommand.STREAM_DATA -> hop.collect(request).takeIf { complete -> !complete }?.let { hop.refuse(request.stream) }
            RelayCommand.STREAM_END -> hop.finish(request)?.let { body -> launch { exit(hop, request.stream, body) } }
            else -> Unit
        }
    }

    /** As the last hop, the session node makes an HTTP request for the app: only to node mailboxes and blobs. */
    private suspend fun exit(
        hop: CircuitHop,
        stream: Int,
        encodedRequest: ByteArray
    ) {
        val request = CircuitHttpRequest.decode(encodedRequest)
        val target = request?.let { destinations.sessionRequest(it.method, it.url) }
        when {
            request == null || target == null -> hop.answer(stream, RelayCommand.STREAM_REFUSED, ByteArray(0))
            else -> {
                val response =
                    try {
                        call(request, target.toString())
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        null
                    }
                when (response) {
                    null -> hop.answer(stream, RelayCommand.STREAM_FAILED, ByteArray(0))
                    else -> hop.answer(stream, RelayCommand.STREAM_END, response.encode())
                }
            }
        }
    }

    private suspend fun call(
        request: CircuitHttpRequest,
        url: String
    ): CircuitHttpResponse? {
        val response =
            exitClient.request(url) {
                method = HttpMethod.parse(request.method)
                request.bearerToken?.let { token -> header(HttpHeaders.Authorization, "Bearer $token") }
                request.body.takeIf { it.isNotEmpty() }?.let { body ->
                    contentType(request.contentType?.let(ContentType::parse) ?: ContentType.Application.Json)
                    setBody(body)
                }
            }
        val body =
            response
                .bodyAsChannel()
                .readBuffer(MAXIMUM_RESPONSE_BYTES + 1L)
                .readByteArray()
                .takeIf { it.size <= MAXIMUM_RESPONSE_BYTES }
                ?: return null
        return CircuitHttpResponse(response.status.value, response.headers[HttpHeaders.ContentType], body)
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
        exitClient.close()
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

        private val streams = HashMap<Int, ByteArrayOutputStream>()

        /** Sealed and sent under one lock: several answers may be on their way, and their counts must arrive in order. */
        suspend fun reply(payload: RelayPayload) {
            counters.withLock {
                val sealed = CircuitLayer.seal(keys.backward, backwardCount++, payload.encode())
                inbound.send(CircuitCell(circuitId, CircuitCell.RELAY, sealed))
            }
        }

        /** Adds a request part; false when the request grew too large or too many are open. */
        fun collect(part: RelayPayload): Boolean {
            val buffer =
                streams[part.stream]
                    ?: ByteArrayOutputStream().takeIf { streams.size < MAXIMUM_OPEN_STREAMS }?.also { streams[part.stream] = it }
                    ?: return false
            buffer.write(part.data)
            return buffer.size() <= MAXIMUM_REQUEST_BYTES
        }

        /** The whole request once its last part arrived, or null when it was refused. */
        suspend fun finish(last: RelayPayload): ByteArray? {
            val complete = collect(last)
            val request = streams.remove(last.stream)?.toByteArray()
            if (!complete) refuse(last.stream)
            return request?.takeIf { complete }
        }

        suspend fun refuse(stream: Int) {
            streams.remove(stream)
            reply(RelayPayload(RelayCommand.STREAM_REFUSED, stream, ByteArray(0)))
        }

        /** [data] in as many parts as it needs; the last one carries [command]. */
        suspend fun answer(
            stream: Int,
            command: Int,
            data: ByteArray
        ) {
            val parts =
                (data.indices step RelayPayload.MAXIMUM_DATA_BYTES)
                    .map { start -> data.copyOfRange(start, minOf(start + RelayPayload.MAXIMUM_DATA_BYTES, data.size)) }
                    .ifEmpty { listOf(ByteArray(0)) }
            parts.dropLast(1).forEach { part -> reply(RelayPayload(RelayCommand.STREAM_DATA, stream, part)) }
            reply(RelayPayload(command, stream, parts.last()))
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
                                CircuitCell.RELAY ->
                                    counters.withLock {
                                        val layered = CircuitLayer.stream(keys.backward, backwardCount++, cell.body)
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
        const val MAXIMUM_OPEN_STREAMS = 8

        /** Room for the largest envelope a mailbox takes (1 MiB of payload) and its JSON around it. */
        const val MAXIMUM_REQUEST_BYTES = 3 * 512 * 1024

        /** Room for one page of pending envelopes: a mailbox answers at most about 1 MiB plus one envelope. */
        const val MAXIMUM_RESPONSE_BYTES = 3 * 1024 * 1024

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

    /** A part of a request (towards the session node) or an answer (back), more parts follow. */
    const val STREAM_DATA = 6

    /** The last part of a request or an answer. */
    const val STREAM_END = 7

    /** The session node will not make this request: not a mailbox, too large or too many at once. */
    const val STREAM_REFUSED = 8

    /** The request could not be made: the mailbox node did not answer, or the answer was too large. */
    const val STREAM_FAILED = 9
}

/**
 * An HTTP request the session node makes for the app, in a stream:
 * `method length (1) || method || url length (2) || url || token length (2) || bearer token || content type length (1) ||
 * content type || body`. An empty token or content type means none.
 */
internal class CircuitHttpRequest(
    val method: String,
    val url: String,
    val bearerToken: String?,
    val contentType: String?,
    val body: ByteArray
) {
    fun encode(): ByteArray {
        val output = ByteArrayOutputStream()
        output.writeField(method.encodeToByteArray(), 1)
        output.writeField(url.encodeToByteArray(), 2)
        output.writeField(bearerToken.orEmpty().encodeToByteArray(), 2)
        output.writeField(contentType.orEmpty().encodeToByteArray(), 1)
        output.write(body)
        return output.toByteArray()
    }

    companion object {
        fun decode(bytes: ByteArray): CircuitHttpRequest? =
            runCatching {
                val buffer = ByteBuffer.wrap(bytes)
                val method = buffer.readField(1)
                val url = buffer.readField(2)
                val token = buffer.readField(2)
                val contentType = buffer.readField(1)
                CircuitHttpRequest(
                    method = method,
                    url = url,
                    bearerToken = token.ifEmpty { null },
                    contentType = contentType.ifEmpty { null },
                    body = ByteArray(buffer.remaining()).also(buffer::get)
                )
            }.getOrNull()
    }
}

/** The answer: `status (2) || content type length (1) || content type || body`. */
internal class CircuitHttpResponse(
    val status: Int,
    val contentType: String?,
    val body: ByteArray
) {
    fun encode(): ByteArray {
        val output = ByteArrayOutputStream()
        output.write(status ushr 8)
        output.write(status and 0xFF)
        output.writeField(contentType.orEmpty().encodeToByteArray(), 1)
        output.write(body)
        return output.toByteArray()
    }
}

private fun ByteArrayOutputStream.writeField(
    value: ByteArray,
    lengthBytes: Int
) {
    require(value.size < (1 shl (8 * lengthBytes))) { "Field too long" }
    (lengthBytes - 1 downTo 0).forEach { index -> write((value.size ushr (8 * index)) and 0xFF) }
    write(value)
}

private fun ByteBuffer.readField(lengthBytes: Int): String {
    val length = (0 until lengthBytes).fold(0) { value, _ -> (value shl 8) or (get().toInt() and 0xFF) }
    return ByteArray(length).also(::get).decodeToString()
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
