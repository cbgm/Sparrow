package com.cbgm.sparrow.feature.transport.data.blob

import com.cbgm.sparrow.core.crypto.hash.Sha256CryptoHash
import com.cbgm.sparrow.feature.transport.data.directory.NodeEndpointRemoteDataSource
import com.cbgm.sparrow.feature.transport.data.mailbox.MailboxCreationWorkSolver
import com.cbgm.sparrow.feature.transport.data.route.circuit.CircuitRequestRefusedException
import com.cbgm.sparrow.feature.transport.data.route.circuit.FileTransferRequest
import com.cbgm.sparrow.feature.transport.data.route.circuit.FileTransferResponse
import com.cbgm.sparrow.feature.transport.data.route.circuit.FileTransferRoute
import com.cbgm.sparrow.feature.transport.data.route.circuit.FileTransfers
import com.cbgm.sparrow.feature.transport.data.routing.RoutingIdLocalDataSource
import com.cbgm.sparrow.feature.transport.domain.model.BlobUpload
import com.cbgm.sparrow.feature.transport.domain.model.FileTransferFailedException
import com.cbgm.sparrow.feature.transport.domain.model.NodeEndpoint
import com.cbgm.sparrow.feature.transport.domain.rule.isFileTransferFailure
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BlobRepositoryImplTest {
    private val hash = Sha256CryptoHash()
    private val node = FakeBlobNode()
    private val ciphertext = Random(7).nextBytes(BlobRepositoryImpl.CHUNK_BYTES * 2 + 5_000)

    @Test
    fun aBlobGoesInChunksToTheSessionNodeAndComesBackWhole() =
        runTest {
            val gateway = gateway()

            val stored = gateway.upload(upload())
            val downloaded = gateway.download(stored.nodeId, BLOB_ID, READ, ciphertext.size.toLong())

            assertEquals("node-a", stored.nodeId)
            assertEquals(3, node.chunks.size)
            assertContentEquals(ciphertext, downloaded)
        }

    @Test
    fun theLastChunkIsPaddedSoTheNodeSeesOnlyASizeClass() =
        runTest {
            gateway().upload(upload())

            assertEquals(2L * BlobRepositoryImpl.CHUNK_BYTES + 16L * 1_024L, node.openedByteSize)
            assertEquals(16 * 1_024, node.chunks.getValue(2).size)
        }

    @Test
    fun anInterruptedUploadGoesOnWithTheMissingChunksThroughANewCircuit() =
        runTest {
            node.failPutAt = 1
            val transfers = CountingTransfers(node)

            gateway(transfers).upload(upload())

            assertEquals(2, transfers.circuits)
            assertEquals(listOf(0, 1, 1, 2), node.putOrder)
        }

    @Test
    fun aRefusingSessionNodeIsNotAskedAgainAndTheFailureSaysAFileTransferFailed() =
        runTest {
            node.refuses = true
            val transfers = CountingTransfers(node)

            val failure = assertFailsWith<FileTransferFailedException> { gateway(transfers).upload(upload()) }

            assertEquals(1, transfers.circuits)
            assertTrue(failure.isFileTransferFailure())
        }

    @Test
    fun paddingStaysWithinAChunk() {
        assertEquals(1_024L, BlobRepositoryImpl.paddedSize(10))
        assertEquals(BlobRepositoryImpl.CHUNK_BYTES.toLong(), BlobRepositoryImpl.paddedSize(BlobRepositoryImpl.CHUNK_BYTES.toLong()))
        assertEquals(BlobRepositoryImpl.CHUNK_BYTES + 1_024L, BlobRepositoryImpl.paddedSize(BlobRepositoryImpl.CHUNK_BYTES + 1L))
        assertTrue(BlobRepositoryImpl.paddedSize(300_000) % (64 * 1_024) == 0L)
    }

    private fun gateway(transfers: FileTransfers = CountingTransfers(node)) =
        BlobRepositoryImpl(
            transfers = transfers,
            json = Json,
            creationWork = MailboxCreationWorkSolver(hash, bits = 0),
            cryptoHash = hash,
            nodeEndpointDataSource =
                object : NodeEndpointRemoteDataSource {
                    override suspend fun resolve(
                        localRoutingId: String,
                        forceRefresh: Boolean
                    ) = Result.success(listOf(NodeEndpoint("node-a", "wss://node-a.test/v1/gateway")))
                },
            routingIdDataSource =
                object : RoutingIdLocalDataSource {
                    override suspend fun getLocalRoutingId() = Result.success("scrouting1_me")

                    override suspend fun getLocalBootstrapRoutingId(): Result<String> = error("not used")
                },
            randomBytes = { size -> ByteArray(size) { 7 } },
            retryDelayMilliseconds = 0L
        )

    private fun upload() =
        BlobUpload(
            blobId = BLOB_ID,
            ciphertext = ciphertext,
            readCapabilitySha256 = "r",
            deleteCapabilitySha256 = "d",
            uploadCapability = UPLOAD,
            uploadCapabilitySha256 = "u",
            expiresAtEpochMilliseconds = 1_000_000L
        )

    private class CountingTransfers(
        private val node: FakeBlobNode
    ) : FileTransfers {
        var circuits = 0

        override suspend fun <T> transfer(block: suspend (FileTransferRoute) -> T): T {
            circuits++
            return block(node)
        }
    }

    /** One blob node behind a circuit that ends at it. */
    private inner class FakeBlobNode : FileTransferRoute {
        override val sessionNodeId = "node-a"
        override val sessionNodeBaseUrl = "https://node-a.test"
        val chunks = HashMap<Int, ByteArray>()
        val putOrder = mutableListOf<Int>()
        var openedByteSize = 0L
        var failPutAt: Int? = null
        var refuses = false

        override suspend fun request(request: FileTransferRequest): FileTransferResponse {
            if (refuses) throw CircuitRequestRefusedException()
            val path = request.url.removePrefix("https://node-a.test")
            val chunkIndex = path.substringAfter("/chunks/", "").toIntOrNull()
            return when {
                request.method == "POST" && path == "/v1/blobs" -> {
                    openedByteSize = Json.parseToJsonElement(request.body.decodeToString()).jsonObject.getValue("byteSize").jsonPrimitive.long
                    respond("""{"nodeId":"node-a","chunkBytes":${BlobRepositoryImpl.CHUNK_BYTES},"expiresAtEpochMilliseconds":1000000}""")
                }
                request.method == "GET" && path.endsWith("/chunks") -> respond("""{"stored":[${chunks.keys.sorted().joinToString(",")}]}""")
                request.method == "PUT" && chunkIndex != null -> put(chunkIndex, request.body)
                request.method == "GET" && chunkIndex != null -> FileTransferResponse(200, chunks.getValue(chunkIndex))
                else -> FileTransferResponse(400, ByteArray(0))
            }
        }

        private fun put(
            index: Int,
            body: ByteArray
        ): FileTransferResponse {
            putOrder += index
            if (failPutAt == index) {
                failPutAt = null
                error("circuit closed")
            }
            chunks[index] = body
            val sha256 = hash.sha256(body).joinToString("") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }
            return respond("""{"index":$index,"sha256":"$sha256"}""")
        }

        private fun respond(json: String) = FileTransferResponse(200, json.encodeToByteArray())
    }

    private companion object {
        const val BLOB_ID = "blob-0123456789abcdef"
        const val READ = "read"
        const val UPLOAD = "upload"
    }
}
