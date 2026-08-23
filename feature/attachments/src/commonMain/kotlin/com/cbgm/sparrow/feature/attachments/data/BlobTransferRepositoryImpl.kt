package com.cbgm.sparrow.feature.attachments.data

import com.cbgm.sparrow.core.crypto.blob.BlobCipher
import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.core.crypto.random.SecureRandomGenerator
import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.attachment.EncryptedBlobReference
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.attachments.domain.model.UploadedBlob
import com.cbgm.sparrow.feature.attachments.domain.repository.BlobTransferRepository
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpointResolver
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayBlobUploadTicketRequest
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import com.cbgm.sparrow.feature.transport.websocket.WebSocketTransportClient
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class BlobTransferRepositoryImpl(
    private val httpClient: HttpClient,
    private val webSocketTransportClient: WebSocketTransportClient,
    private val nodeEndpointResolver: NodeEndpointResolver,
    private val localRoutingIdProvider: LocalRoutingIdProvider,
    private val blobCipher: BlobCipher,
    private val cryptoHash: CryptoHash,
    private val secureRandomGenerator: SecureRandomGenerator
) : BlobTransferRepository {
    override suspend fun upload(
        plaintext: ByteArray,
        retentionMilliseconds: Long
    ): Result<UploadedBlob> =
        runCatching {
            require(plaintext.isNotEmpty()) { "Attachment blob must not be empty" }
            require(retentionMilliseconds > 0L) { "Blob retention must be positive" }

            val blobId = IdGenerator.generate(prefix = "blob")
            val readCapability = capability()
            val deleteCapability = capability()
            val associatedData = associatedData(blobId)
            val encrypted = blobCipher.encrypt(plaintext, associatedData).getOrThrow()
            check(encrypted.ciphertext.size.toLong() <= EncryptedBlobReference.MAX_BLOB_CIPHERTEXT_BYTES) {
                "Encrypted blob exceeds the supported client size"
            }
            val now = SystemClock.nowEpochMilliseconds()
            check(retentionMilliseconds <= Long.MAX_VALUE - now) { "Blob retention overflows its expiry" }
            val expiresAt = now + retentionMilliseconds
            val ticket =
                webSocketTransportClient
                    .requestBlobUploadTicket(
                        request =
                            GatewayBlobUploadTicketRequest(
                                requestId = IdGenerator.generate(prefix = "blob-ticket"),
                                blobId = blobId,
                                maximumBytes = encrypted.ciphertext.size.toLong(),
                                readCapabilitySha256 = cryptoHash.sha256(readCapability.encodeToByteArray()).hex(),
                                deleteCapabilitySha256 = cryptoHash.sha256(deleteCapability.encodeToByteArray()).hex(),
                                blobExpiresAtEpochMilliseconds = expiresAt
                            ),
                        timeoutMilliseconds = TICKET_TIMEOUT_MILLISECONDS
                    ).getOrThrow()

            val endpoint = resolveBlobEndpoint(ticket.nodeId)
            val response =
                httpClient.put("${endpoint.trimEnd('/')}/v1/blobs/$blobId") {
                    bearerAuth(ticket.uploadToken)
                    contentType(ContentType.Application.OctetStream)
                    setBody(encrypted.ciphertext)
                }
            check(response.status.isSuccess()) {
                "Blob upload failed with HTTP ${response.status.value}"
            }

            UploadedBlob(
                reference =
                    EncryptedBlobReference(
                        nodeId = ticket.nodeId,
                        blobId = blobId,
                        readCapability = readCapability,
                        ciphertextByteSize = encrypted.ciphertext.size.toLong(),
                        expiresAtEpochMilliseconds = ticket.blobExpiresAtEpochMilliseconds,
                        encryptionKey = encrypted.key,
                        nonce = encrypted.nonce,
                        ciphertextSha256 = cryptoHash.sha256(encrypted.ciphertext)
                    ),
                deleteCapability = deleteCapability
            )
        }

    override suspend fun download(reference: EncryptedBlobReference): Result<ByteArray> =
        runCatching {
            val endpoint = resolveBlobEndpoint(reference.nodeId)
            val response =
                httpClient.get("${endpoint.trimEnd('/')}/v1/blobs/${reference.blobId}") {
                    bearerAuth(reference.readCapability)
                }
            check(response.status.isSuccess()) {
                "Blob download failed with HTTP ${response.status.value}"
            }
            val ciphertext = response.readExactly(reference.ciphertextByteSize)
            check(cryptoHash.sha256(ciphertext).contentEquals(reference.ciphertextSha256)) {
                "Blob ciphertext hash mismatch"
            }
            blobCipher
                .decrypt(
                    ciphertext = ciphertext,
                    key = reference.encryptionKey,
                    nonce = reference.nonce,
                    associatedData = associatedData(reference.blobId)
                ).getOrThrow()
        }

    override suspend fun delete(uploadedBlob: UploadedBlob): Result<Unit> =
        runCatching {
            val reference = uploadedBlob.reference
            val endpoint = resolveBlobEndpoint(reference.nodeId)
            val response =
                httpClient.delete("${endpoint.trimEnd('/')}/v1/blobs/${reference.blobId}") {
                    bearerAuth(uploadedBlob.deleteCapability)
                }
            check(response.status.isSuccess() || response.status.value == HTTP_NOT_FOUND) {
                "Blob delete failed with HTTP ${response.status.value}"
            }
        }

    private suspend fun io.ktor.client.statement.HttpResponse.readExactly(expectedBytes: Long): ByteArray {
        require(expectedBytes in 1..EncryptedBlobReference.MAX_BLOB_CIPHERTEXT_BYTES) {
            "Invalid blob ciphertext size"
        }
        val expectedSize = expectedBytes.toInt()
        val result = ByteArray(expectedSize)
        val channel = bodyAsChannel()
        var offset = 0
        while (offset < expectedSize) {
            val read = channel.readAvailable(result, offset, expectedSize - offset)
            check(read >= 0) { "Blob download ended before the declared ciphertext size" }
            if (read > 0) offset += read
        }
        return result
    }

    private suspend fun resolveBlobEndpoint(nodeId: String): String {
        val localRoutingId = localRoutingIdProvider.getLocalRoutingId().getOrThrow()

        fun List<com.cbgm.sparrow.feature.transport.discovery.NodeEndpoint>.findBlobBase(): String? =
            firstOrNull { endpoint -> endpoint.nodeId == nodeId }?.websocketUrl?.toBlobHttpBase()

        return nodeEndpointResolver.resolve(localRoutingId, forceRefresh = false).getOrThrow().findBlobBase()
            ?: nodeEndpointResolver.resolve(localRoutingId, forceRefresh = true).getOrThrow().findBlobBase()
            ?: error("Blob node $nodeId is not available")
    }

    private fun String.toBlobHttpBase(): String {
        val httpUrl =
            when {
                startsWith("wss://") -> "https://${removePrefix("wss://")}"
                startsWith("ws://") -> "http://${removePrefix("ws://")}"
                else -> error("Unsupported gateway WebSocket URL: $this")
            }
        return httpUrl.substringBefore("/v1/gateway").trimEnd('/')
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun capability(): String =
        Base64.encode(
            secureRandomGenerator.generateBytes(CAPABILITY_BYTES).getOrThrow()
        )

    private fun associatedData(blobId: String): ByteArray =
        "sparrow.blob.v1:$blobId".encodeToByteArray()

    private fun ByteArray.hex(): String =
        joinToString(separator = "") { byte ->
            val value = byte.toInt() and 0xff
            HEX[value ushr 4].toString() + HEX[value and 0x0f]
        }

    private companion object {
        const val CAPABILITY_BYTES = 32
        const val TICKET_TIMEOUT_MILLISECONDS = 10_000L
        const val HTTP_NOT_FOUND = 404
        const val HEX = "0123456789abcdef"
    }
}
