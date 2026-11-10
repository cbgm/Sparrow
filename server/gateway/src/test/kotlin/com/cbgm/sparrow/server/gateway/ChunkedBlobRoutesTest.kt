package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.protocol.BlobChunkStored
import com.cbgm.sparrow.server.protocol.BlobChunksResponse
import com.cbgm.sparrow.server.protocol.MailboxCreationWork
import com.cbgm.sparrow.server.protocol.OpenBlobRequest
import com.cbgm.sparrow.server.protocol.OpenBlobResponse
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.security.CreationWorkCheck
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.nio.file.Files
import java.security.MessageDigest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ChunkedBlobRoutesTest {
    private val blobId = "blob-${UUID.randomUUID()}"
    private val content = ByteArray(BlobStore.CHUNK_BYTES * 2 + 1_000) { index -> (index % 251).toByte() }

    @Test
    fun aBlobArrivesInChunksAndIsReadChunkByChunk() =
        blobs {
            assertEquals(HttpStatusCode.Created, open().status)
            val chunks = content.chunks()
            chunks.forEachIndexed { index, chunk ->
                val stored = serverJson.decodeFromString<BlobChunkStored>(putChunk(index, chunk).bodyAsText())
                assertEquals(sha256(chunk), stored.sha256)
            }

            val read = chunks.indices.map { index -> client.get("/v1/blobs/$blobId/chunks/$index") { bearerAuth(READ) }.bodyAsBytes() }
            assertContentEquals(content, read.reduce(ByteArray::plus))
        }

    @Test
    fun anInterruptedUploadLearnsWhichChunksAreThereAndNothingIsReadableBefore() =
        blobs {
            open()
            putChunk(1, content.chunks()[1])

            val stored = client.get("/v1/blobs/$blobId/chunks") { bearerAuth(UPLOAD) }
            assertEquals(listOf(1), serverJson.decodeFromString<BlobChunksResponse>(stored.bodyAsText()).stored)
            assertEquals(HttpStatusCode.NotFound, client.get("/v1/blobs/$blobId/chunks/1") { bearerAuth(READ) }.status)
        }

    @Test
    fun aChunkOfTheWrongSizeOrWithoutTheUploadCapabilityIsRefused() =
        blobs {
            open()

            assertEquals(HttpStatusCode.BadRequest, putChunk(0, ByteArray(10)).status)
            assertEquals(
                HttpStatusCode.NotFound,
                client.put("/v1/blobs/$blobId/chunks/0") {
                    bearerAuth(READ)
                    setBody(content.chunks()[0])
                }.status
            )
        }

    @Test
    fun aBlobIsOpenedOnlyWithProofOfWork() =
        blobs(workBits = 32) {
            assertEquals(HttpStatusCode.Forbidden, open().status)
        }

    @Test
    fun deletingFreesTheBlob() =
        blobs {
            open()
            content.chunks().forEachIndexed { index, chunk -> putChunk(index, chunk) }

            assertEquals(HttpStatusCode.NoContent, client.delete("/v1/blobs/$blobId") { bearerAuth(DELETE) }.status)
            assertEquals(HttpStatusCode.NotFound, client.get("/v1/blobs/$blobId/chunks/0") { bearerAuth(READ) }.status)
        }

    private fun blobs(
        workBits: Int = 0,
        block: suspend ApplicationTestBuilder.() -> Unit
    ) = testApplication {
        val store = BlobStore(Files.createTempDirectory("blobs"), maximumBlobBytes = 8L * BlobStore.CHUNK_BYTES, maximumStorageBytes = 16L * BlobStore.CHUNK_BYTES)
        application {
            install(ContentNegotiation) { json(serverJson) }
            routing {
                installBlobRoutes(store, BlobUploadPermitStore())
                installChunkedBlobRoutes(store, "node-a", CreationWorkCheck(workBits), maximumRetentionMilliseconds = 24L * 60L * 60L * 1_000L)
            }
        }
        block()
    }

    private suspend fun ApplicationTestBuilder.open() =
        client.post("/v1/blobs") {
            contentType(ContentType.Application.Json)
            setBody(
                serverJson.encodeToString(
                    OpenBlobRequest(
                        blobId = blobId,
                        byteSize = content.size.toLong(),
                        readCapabilitySha256 = sha256(READ.encodeToByteArray()),
                        deleteCapabilitySha256 = sha256(DELETE.encodeToByteArray()),
                        uploadCapabilitySha256 = sha256(UPLOAD.encodeToByteArray()),
                        expiresAtEpochMilliseconds = System.currentTimeMillis() + 60L * 60L * 1_000L,
                        creationWork = MailboxCreationWork("stamp-${UUID.randomUUID()}", System.currentTimeMillis(), 0L)
                    )
                )
            )
        }.also { response ->
            if (response.status == HttpStatusCode.Created) {
                assertEquals("node-a", serverJson.decodeFromString<OpenBlobResponse>(response.bodyAsText()).nodeId)
            }
        }

    private suspend fun ApplicationTestBuilder.putChunk(
        index: Int,
        chunk: ByteArray
    ) = client.put("/v1/blobs/$blobId/chunks/$index") {
        bearerAuth(UPLOAD)
        contentType(ContentType.Application.OctetStream)
        setBody(chunk)
    }

    private fun ByteArray.chunks(): List<ByteArray> =
        indices.step(BlobStore.CHUNK_BYTES).map { start -> copyOfRange(start, minOf(size, start + BlobStore.CHUNK_BYTES)) }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(separator = "") { byte -> "%02x".format(byte) }

    private companion object {
        const val READ = "read-capability"
        const val DELETE = "delete-capability"
        const val UPLOAD = "upload-capability"
    }
}
