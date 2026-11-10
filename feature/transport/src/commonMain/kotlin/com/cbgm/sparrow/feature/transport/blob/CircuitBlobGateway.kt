package com.cbgm.sparrow.feature.transport.blob

import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.transport.circuit.CircuitRequestRefusedException
import com.cbgm.sparrow.feature.transport.circuit.FileTransferRequest
import com.cbgm.sparrow.feature.transport.circuit.FileTransferResponse
import com.cbgm.sparrow.feature.transport.circuit.FileTransferRoute
import com.cbgm.sparrow.feature.transport.circuit.FileTransfers
import com.cbgm.sparrow.feature.transport.circuit.isWaitingForRoute
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpointResolver
import com.cbgm.sparrow.feature.transport.mailbox.MailboxCreationWork
import com.cbgm.sparrow.feature.transport.mailbox.MailboxCreationWorkSolver
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds

/**
 * Blobs in chunks of [CHUNK_BYTES], each transfer through a circuit of its own ([FileTransfers]). A new blob is
 * opened, paid with a proof of work, at the session node at the end of the circuit; nothing there names the sender.
 * A transfer that breaks off goes on with the chunks still missing, through a new circuit, up to [MAXIMUM_ATTEMPTS]
 * times. The last chunk is padded with random bytes to a size class, so a node sees only roughly how large a file is.
 */
internal class CircuitBlobGateway(
    private val transfers: FileTransfers,
    private val json: Json,
    private val creationWork: MailboxCreationWorkSolver,
    private val cryptoHash: CryptoHash,
    private val nodeEndpointResolver: NodeEndpointResolver,
    private val localRoutingIdProvider: LocalRoutingIdProvider,
    private val randomBytes: suspend (Int) -> ByteArray,
    private val retryDelayMilliseconds: Long = DEFAULT_RETRY_DELAY_MILLISECONDS
) : BlobGateway {
    private val logger = SparrowLog.withTag("CircuitBlobGateway")

    override suspend fun upload(blob: BlobUpload): StoredBlob {
        val padded = blob.ciphertext + randomBytes(paddedSize(blob.ciphertext.size.toLong()).toInt() - blob.ciphertext.size)
        val chunks = padded.chunked()
        var opened: OpenedBlob? = null
        val stored = HashSet<Int>()
        return retrying("upload") {
            transfers.transfer { route ->
                val target = opened ?: open(route, blob, padded.size.toLong()).also { opened = it }
                if (stored.isEmpty()) stored += storedChunks(route, target.baseUrl, blob)
                chunks.indices.filterNot(stored::contains).forEach { index ->
                    putChunk(route, target.baseUrl, blob, index, chunks[index])
                    stored += index
                }
                StoredBlob(target.nodeId, target.expiresAtEpochMilliseconds)
            }
        }
    }

    override suspend fun download(
        nodeId: String,
        blobId: String,
        readCapability: String,
        ciphertextByteSize: Long
    ): ByteArray {
        require(ciphertextByteSize in 1..MAXIMUM_CIPHERTEXT_BYTES) { "Invalid blob ciphertext size" }
        val baseUrl = baseUrl(nodeId)
        val chunkCount = ((paddedSize(ciphertextByteSize) + CHUNK_BYTES - 1) / CHUNK_BYTES).toInt()
        val chunks = arrayOfNulls<ByteArray>(chunkCount)
        return retrying("download") {
            transfers.transfer { route ->
                for (index in 0 until chunkCount) {
                    if (chunks[index] != null) continue
                    val response = route.request(FileTransferRequest("GET", "$baseUrl/v1/blobs/$blobId/chunks/$index", readCapability))
                    if (index == 0 && response.status == HTTP_NOT_FOUND) return@transfer wholeBlob(route, baseUrl, blobId, readCapability, ciphertextByteSize)
                    response.requireSuccess("Blob chunk $index download")
                    chunks[index] = response.body
                }
                joined(chunks.filterNotNull(), ciphertextByteSize.toInt())
            }
        }
    }

    override suspend fun delete(
        nodeId: String,
        blobId: String,
        deleteCapability: String
    ) {
        val baseUrl = baseUrl(nodeId)
        val response = transfers.transfer { route -> route.request(FileTransferRequest("DELETE", "$baseUrl/v1/blobs/$blobId", deleteCapability)) }
        check(response.isSuccess || response.status == HTTP_NOT_FOUND) { "Blob delete failed with HTTP ${response.status}" }
    }

    private suspend fun open(
        route: FileTransferRoute,
        blob: BlobUpload,
        byteSize: Long
    ): OpenedBlob {
        val (nodeId, baseUrl) = route.sessionNodeId?.let { id -> route.sessionNodeBaseUrl?.let { url -> id to url } } ?: directNode()
        val request =
            OpenBlobRequest(
                blobId = blob.blobId,
                byteSize = byteSize,
                readCapabilitySha256 = blob.readCapabilitySha256,
                deleteCapabilitySha256 = blob.deleteCapabilitySha256,
                uploadCapabilitySha256 = blob.uploadCapabilitySha256,
                expiresAtEpochMilliseconds = blob.expiresAtEpochMilliseconds,
                creationWork = creationWork.solve()
            )
        val response =
            route.request(FileTransferRequest("POST", "$baseUrl/v1/blobs", contentType = JSON, body = json.encodeToString(request).encodeToByteArray()))
        response.requireSuccess("Opening the blob")
        val opened = json.decodeFromString<OpenBlobResponse>(response.body.decodeToString())
        check(opened.chunkBytes == CHUNK_BYTES) { "The blob node uses chunks of ${opened.chunkBytes} bytes" }
        return OpenedBlob(opened.nodeId, baseUrl, opened.expiresAtEpochMilliseconds)
    }

    private suspend fun storedChunks(
        route: FileTransferRoute,
        baseUrl: String,
        blob: BlobUpload
    ): List<Int> {
        val response = route.request(FileTransferRequest("GET", "$baseUrl/v1/blobs/${blob.blobId}/chunks", blob.uploadCapability))
        response.requireSuccess("Asking for the stored chunks")
        return json.decodeFromString<BlobChunksResponse>(response.body.decodeToString()).stored
    }

    /** Sends a chunk; the node answers the hash of what it stored, and a chunk that arrived changed is sent once more. */
    private suspend fun putChunk(
        route: FileTransferRoute,
        baseUrl: String,
        blob: BlobUpload,
        index: Int,
        chunk: ByteArray
    ) {
        val expected = cryptoHash.sha256(chunk).hex()
        repeat(CHUNK_SEND_ATTEMPTS) {
            val response =
                route.request(FileTransferRequest("PUT", "$baseUrl/v1/blobs/${blob.blobId}/chunks/$index", blob.uploadCapability, OCTET_STREAM, chunk))
            response.requireSuccess("Blob chunk $index upload")
            if (json.decodeFromString<BlobChunkStored>(response.body.decodeToString()).sha256 == expected) return
            logger.warn { "Blob chunk $index arrived changed; sending it again" }
        }
        error("Blob chunk $index did not arrive intact")
    }

    /** The first [size] bytes of [chunks]; the rest is the last chunk's padding. */
    private fun joined(
        chunks: List<ByteArray>,
        size: Int
    ): ByteArray {
        check(chunks.sumOf { chunk -> chunk.size.toLong() } >= size) { "Blob download ended before the declared ciphertext size" }
        val result = ByteArray(size)
        var offset = 0
        for (chunk in chunks) {
            if (offset >= size) break
            val length = minOf(chunk.size, size - offset)
            chunk.copyInto(result, offset, 0, length)
            offset += length
        }
        return result
    }

    /** A blob sent before chunks existed, in one piece; only small ones fit through a circuit. */
    private suspend fun wholeBlob(
        route: FileTransferRoute,
        baseUrl: String,
        blobId: String,
        readCapability: String,
        ciphertextByteSize: Long
    ): ByteArray {
        if (ciphertextByteSize > MAXIMUM_WHOLE_BLOB_BYTES) throw BlobRejectedException("A blob sent in one piece is too large for a circuit")
        val response = route.request(FileTransferRequest("GET", "$baseUrl/v1/blobs/$blobId", readCapability))
        response.requireSuccess("Blob download")
        check(response.body.size.toLong() == ciphertextByteSize) { "Blob download ended before the declared ciphertext size" }
        return response.body
    }

    /**
     * Runs [attempt] again after a failure, through a new circuit. Not retried: the user's choice to wait for a route
     * (passed on as it is), and a node that refused the request or the blob (that answer does not change).
     */
    private suspend fun <T> retrying(
        what: String,
        attempt: suspend () -> T
    ): T {
        var failures = 0
        while (true) {
            try {
                return attempt()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                failures++
                if (failure.isWaitingForRoute()) throw failure
                if (failure is BlobRejectedException || failure is CircuitRequestRefusedException || failures >= MAXIMUM_ATTEMPTS) {
                    throw FileTransferFailedException(failure)
                }
                logger.info { "Blob $what interrupted (${failure.message}); going on through a new circuit ($failures of $MAXIMUM_ATTEMPTS)" }
                delay((retryDelayMilliseconds * failures).milliseconds)
            }
        }
    }

    private suspend fun directNode(): Pair<String, String> {
        val endpoint =
            nodeEndpointResolver
                .resolve(localRoutingIdProvider.getLocalRoutingId().getOrThrow(), forceRefresh = false)
                .getOrThrow()
                .firstOrNull()
                ?: error("No blob node is available")
        return endpoint.nodeId to endpoint.websocketUrl.toHttpBase()
    }

    private suspend fun baseUrl(nodeId: String): String {
        val localRoutingId = localRoutingIdProvider.getLocalRoutingId().getOrThrow()

        suspend fun find(forceRefresh: Boolean): String? =
            nodeEndpointResolver.resolve(localRoutingId, forceRefresh).getOrThrow().firstOrNull { it.nodeId == nodeId }?.websocketUrl?.toHttpBase()
        return find(forceRefresh = false) ?: find(forceRefresh = true) ?: error("Blob node $nodeId is not available")
    }

    private fun String.toHttpBase(): String {
        val httpUrl =
            when {
                startsWith("wss://") -> "https://${removePrefix("wss://")}"
                startsWith("ws://") -> "http://${removePrefix("ws://")}"
                else -> error("Unsupported gateway WebSocket URL: $this")
            }
        return httpUrl.substringBefore("/v1/gateway").trimEnd('/')
    }

    private fun FileTransferResponse.requireSuccess(what: String) {
        if (status in CLIENT_ERRORS) throw BlobRejectedException("$what failed with HTTP $status")
        check(isSuccess) { "$what failed with HTTP $status" }
    }

    private fun ByteArray.chunked(): List<ByteArray> = (indices step CHUNK_BYTES).map { start -> copyOfRange(start, minOf(size, start + CHUNK_BYTES)) }

    private fun ByteArray.hex(): String =
        joinToString(separator = "") { byte ->
            val value = byte.toInt() and 0xff
            HEX[value ushr 4].toString() + HEX[value and 0x0f]
        }

    private class OpenedBlob(
        val nodeId: String,
        val baseUrl: String,
        val expiresAtEpochMilliseconds: Long
    )

    companion object {
        /** As the blob node expects ([BlobStore.CHUNK_BYTES] there); fits into one request through a circuit. */
        const val CHUNK_BYTES = 1_048_576
        private const val MAXIMUM_CIPHERTEXT_BYTES = 128L * 1024L * 1024L
        private const val MAXIMUM_WHOLE_BLOB_BYTES = 3L * 1024L * 1024L - 1_024L
        private const val MAXIMUM_ATTEMPTS = 5
        private const val CHUNK_SEND_ATTEMPTS = 2
        private const val DEFAULT_RETRY_DELAY_MILLISECONDS = 3_000L
        private const val HTTP_NOT_FOUND = 404
        private val CLIENT_ERRORS = 400..499
        private const val JSON = "application/json"
        private const val OCTET_STREAM = "application/octet-stream"
        private const val HEX = "0123456789abcdef"
        private val SIZE_CLASSES = listOf(1, 4, 16, 64, 256).map { kibibytes -> kibibytes * 1_024L }
        private const val LARGE_STEP = 64L * 1_024L

        /** Whole chunks, then the last one grown to 1, 4, 16, 64 or 256 KiB, beyond that to the next 64 KiB. */
        fun paddedSize(size: Long): Long {
            val whole = (size - 1) / CHUNK_BYTES * CHUNK_BYTES
            val last = size - whole
            val paddedLast = SIZE_CLASSES.firstOrNull { sizeClass -> last <= sizeClass } ?: ((last + LARGE_STEP - 1) / LARGE_STEP * LARGE_STEP)
            return whole + minOf(paddedLast, CHUNK_BYTES.toLong())
        }
    }
}

/** The blob node refused a request (expired, not found, too large); trying again through another circuit does not help. */
internal class BlobRejectedException(
    message: String
) : IllegalStateException(message)

@Serializable
private data class OpenBlobRequest(
    val blobId: String,
    val byteSize: Long,
    val readCapabilitySha256: String,
    val deleteCapabilitySha256: String,
    val uploadCapabilitySha256: String,
    val expiresAtEpochMilliseconds: Long,
    val creationWork: MailboxCreationWork
)

@Serializable
private data class OpenBlobResponse(
    val nodeId: String,
    val chunkBytes: Int,
    val expiresAtEpochMilliseconds: Long
)

@Serializable
private data class BlobChunksResponse(
    val stored: List<Int>
)

@Serializable
private data class BlobChunkStored(
    val index: Int,
    val sha256: String
)
