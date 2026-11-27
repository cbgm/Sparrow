package com.cbgm.sparrow.feature.transport.data.route.circuit

import com.cbgm.sparrow.core.crypto.circuit.CircuitHopKeys
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketExtension
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/** Plays the relays of a circuit the way the gateway's CircuitRelay does, with the readable test cipher. */
internal class SimulatedRelays(
    scope: CoroutineScope,
    private val cipher: ReadableCircuitCipher,
    private val unreachable: Set<String> = emptySet()
) {
    private val toApp = Channel<Frame>(Channel.UNLIMITED)
    private val fromApp = Channel<Frame>(Channel.UNLIMITED)
    private val hops = mutableListOf<Hop>()
    val extendedTo = mutableListOf<String>()
    var pingsAtLastHop = 0
    var lastRequest: String? = null
    private val streams = mutableMapOf<Int, ByteArray>()
    val session: WebSocketSession = ChannelSession(toApp, fromApp)
    private val job: Job =
        scope.launch {
            for (frame in fromApp) {
                // The app closes the link with a Close frame; nothing to relay then.
                val cell = (frame as? Frame.Binary)?.let { binary -> CircuitCell.decode(binary.readBytes()) } ?: continue
                when (cell.command) {
                    CircuitCell.CREATE -> created(cell.body.copyOf(32))
                    CircuitCell.RELAY -> relay(cell.body)
                    else -> Unit
                }
            }
        }

    private var heldWait: Int? = null

    fun stop() {
        job.cancel()
    }

    suspend fun destroy() {
        toApp.send(Frame.Binary(true, CircuitCell(1, CircuitCell.DESTROY, ByteArray(0)).encode()))
    }

    private suspend fun created(ephemeral: ByteArray) {
        val hop = Hop(ephemeral.first().toInt())
        hops += hop
        toApp.send(Frame.Binary(true, CircuitCell(1, CircuitCell.CREATED, hop.reply()).encode()))
    }

    private suspend fun relay(body: ByteArray) {
        val inner = hops.dropLast(1)
        val last = hops.last()
        var layered = body
        inner.forEach { hop -> layered = cipher.stream(hop.keys.forward, hop.forwardCount++, layered) }
        val request = RelayPayload.decode(cipher.open(last.keys.forward, last.forwardCount++, layered)!!)!!
        val answers =
            when (request.command) {
                RelayCommand.EXTEND -> listOf(extend(request.data))
                RelayCommand.PING -> listOf(RelayPayload(RelayCommand.PONG, request.stream, request.data)).also { pingsAtLastHop++ } + releaseWait()
                RelayCommand.STREAM_DATA -> emptyList<RelayPayload>().also { streams[request.stream] = (streams[request.stream] ?: ByteArray(0)) + request.data }
                RelayCommand.STREAM_END -> exit(request.stream, (streams.remove(request.stream) ?: ByteArray(0)) + request.data)
                else -> error("Unexpected command ${request.command}")
            }
        answers.forEach { answer ->
            var back = cipher.seal(last.keys.backward, last.backwardCount++, answer.encode())
            inner.asReversed().forEach { hop -> back = cipher.stream(hop.keys.backward, hop.backwardCount++, back) }
            toApp.send(Frame.Binary(true, CircuitCell(1, CircuitCell.RELAY, back).encode()))
        }
    }

    /** Reads the request as the gateway does and answers mailbox paths with a large body. */
    private fun exit(
        stream: Int,
        bytes: ByteArray
    ): List<RelayPayload> {
        var offset = 0

        fun field(lengthBytes: Int): String {
            val length = (0 until lengthBytes).fold(0) { value, index -> (value shl 8) or (bytes[offset + index].toInt() and 0xFF) }
            offset += lengthBytes
            return bytes.copyOfRange(offset, offset + length).decodeToString().also { offset += length }
        }
        val method = field(1)
        val url = field(2)
        val token = field(2)
        field(1)
        lastRequest = "$method $url $token ${bytes.size - offset}"
        if (url.endsWith("/v1/mailboxes/wait")) {
            heldWait = stream
            return emptyList()
        }
        if ("/v1/mailboxes/" !in url) return listOf(RelayPayload(RelayCommand.STREAM_REFUSED, stream, ByteArray(0)))
        val type = "text/plain".encodeToByteArray()
        val answer = byteArrayOf(0, 201.toByte(), type.size.toByte()) + type + ByteArray(3_000) { (it % 251).toByte() }
        val parts = streamParts(answer)
        return parts.dropLast(1).map { part -> RelayPayload(RelayCommand.STREAM_DATA, stream, part) } +
            RelayPayload(RelayCommand.STREAM_END, stream, parts.last())
    }

    private fun releaseWait(): List<RelayPayload> =
        listOfNotNull(heldWait?.let { stream -> RelayPayload(RelayCommand.STREAM_END, stream, byteArrayOf(0, 200.toByte(), 0)) }).also { heldWait = null }

    private fun extend(data: ByteArray): RelayPayload {
        val urlLength = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
        val url = data.copyOfRange(2, 2 + urlLength).decodeToString()
        if (url in unreachable) return RelayPayload(RelayCommand.EXTEND_FAILED, 0, ByteArray(0))
        extendedTo += url
        // The reply of the new hop is created before the last hop changes, as the gateway attaches it.
        val next = Hop(data[2 + urlLength].toInt())
        val reply = next.reply()
        return RelayPayload(RelayCommand.EXTENDED, 0, reply).also { hops += next }
    }

    private class Hop(
        seed: Int
    ) {
        val keys = CircuitHopKeys(ByteArray(32) { seed.toByte() }, ByteArray(32) { (seed + 100).toByte() })
        var forwardCount = 0L
        var backwardCount = 0L

        fun reply(): ByteArray = keys.forward + keys.backward
    }
}

internal class ChannelSession(
    override val incoming: ReceiveChannel<Frame>,
    override val outgoing: SendChannel<Frame>
) : WebSocketSession {
    override val coroutineContext: CoroutineContext = Job()
    override var masking: Boolean = false
    override var maxFrameSize: Long = Long.MAX_VALUE
    override val extensions: List<WebSocketExtension<*>> = emptyList()

    override suspend fun flush() = Unit

    @Deprecated("Use cancel() instead.", level = DeprecationLevel.ERROR)
    override fun terminate() = Unit
}
