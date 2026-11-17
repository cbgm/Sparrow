package com.cbgm.sparrow.feature.transport.circuit

import com.cbgm.sparrow.core.util.logging.SparrowLog
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException

/** One request of a file transfer; [url] is the blob node's address. */
internal class FileTransferRequest(
    val method: String,
    val url: String,
    val bearerToken: String? = null,
    val contentType: String? = null,
    val body: ByteArray = ByteArray(0)
)

internal class FileTransferResponse(
    val status: Int,
    val body: ByteArray
) {
    val isSuccess: Boolean get() = status in HTTP_SUCCESS
}

/** How one transfer's requests go: through its circuit, made by [sessionNodeId], or directly when that is null. */
internal interface FileTransferRoute {
    val sessionNodeId: String?

    /** The HTTP address of [sessionNodeId], where a new blob is stored. */
    val sessionNodeBaseUrl: String?

    suspend fun request(request: FileTransferRequest): FileTransferResponse
}

/** Runs one file transfer's requests on a route of their own. */
internal interface FileTransfers {
    suspend fun <T> transfer(block: suspend (FileTransferRoute) -> T): T
}

/**
 * Sends and fetches files through a circuit of their own ([CircuitRunner.withOwnCircuit]), one per transfer, so the
 * blob node does not see the device's address and a long transfer does not hold back messages. The switch applies as
 * for messages: without a circuit through at least two hops a transfer goes directly only when the user allows it
 * ([CircuitFallbackStore]); otherwise it waits up to [waitMilliseconds] for the app's circuits to reach two hops and
 * then fails with [ProtectedRouteMissingException].
 */
internal class FileTransferCircuits(
    private val circuits: CircuitRunner,
    private val fallback: CircuitFallbackStore,
    private val directClient: HttpClient,
    private val waitMilliseconds: Long = DEFAULT_WAIT_MILLISECONDS
) : FileTransfers {
    private val logger = SparrowLog.withTag("FileTransferCircuits")

    override suspend fun <T> transfer(block: suspend (FileTransferRoute) -> T): T {
        val sendsDirectly = fallback.sendsDirectly()
        // Without the user's permission to go directly, a transfer starts only once enough relaying nodes are up.
        if (!sendsDirectly) circuits.awaitCircuit(waitMilliseconds, CircuitHopCount.PROTECTING) ?: throw ProtectedRouteMissingException()
        var started = false
        return try {
            circuits.withOwnCircuit(minimumHopCount = if (sendsDirectly) 1 else CircuitHopCount.PROTECTING) { circuit ->
                started = true
                logger.debug { "File transfer through its own circuit (${circuit.nodeIds.size} hop(s))" }
                block(ThroughCircuit(circuit))
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            // Only a circuit that could not be built falls back; a transfer that failed on its circuit is tried again later.
            if (started || !sendsDirectly) throw failure
            logger.warn { "File transfer sent directly (no circuit: ${failure.message}): the blob node sees this device's address" }
            block(Direct())
        }
    }

    private class ThroughCircuit(
        private val circuit: Circuit
    ) : FileTransferRoute {
        override val sessionNodeId: String? = circuit.nodeIds.lastOrNull()
        override val sessionNodeBaseUrl: String? = circuit.sessionNodeCircuitUrl?.toHttpBase()

        override suspend fun request(request: FileTransferRequest): FileTransferResponse {
            val answer =
                circuit.request(
                    CircuitHttpRequest(
                        method = request.method,
                        url = request.url,
                        bearerToken = request.bearerToken,
                        contentType = request.contentType.takeIf { request.body.isNotEmpty() },
                        body = request.body
                    )
                )
            return FileTransferResponse(answer.status, answer.body)
        }
    }

    private companion object {
        const val DEFAULT_WAIT_MILLISECONDS = 30_000L
    }

    private inner class Direct : FileTransferRoute {
        override val sessionNodeId: String? = null
        override val sessionNodeBaseUrl: String? = null

        override suspend fun request(request: FileTransferRequest): FileTransferResponse {
            val response =
                directClient.request(request.url) {
                    method = HttpMethod.parse(request.method)
                    request.bearerToken?.let { token -> header(HttpHeaders.Authorization, "Bearer $token") }
                    request.body.takeIf { it.isNotEmpty() }?.let { bytes ->
                        contentType(request.contentType?.let(ContentType::parse) ?: ContentType.Application.OctetStream)
                        setBody(bytes)
                    }
                }
            return FileTransferResponse(response.status.value, response.readRawBytes())
        }
    }
}

/** `wss://node/v1/circuit` → `https://node`. */
private fun String.toHttpBase(): String? =
    when {
        startsWith("wss://") -> "https://${removePrefix("wss://")}"
        startsWith("ws://") -> "http://${removePrefix("ws://")}"
        else -> null
    }?.substringBefore("/v1/circuit")?.trimEnd('/')

private val HTTP_SUCCESS = 200..299
