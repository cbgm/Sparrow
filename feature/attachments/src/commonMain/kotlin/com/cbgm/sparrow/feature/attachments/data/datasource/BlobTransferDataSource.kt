package com.cbgm.sparrow.feature.attachments.data.datasource

import com.cbgm.sparrow.core.base.blob.data.model.EncryptedBlobReferenceDto
import com.cbgm.sparrow.core.crypto.blob.BlobCipher
import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.core.crypto.random.SecureRandomGenerator
import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.transport.blob.BlobGateway
import com.cbgm.sparrow.feature.transport.blob.BlobUpload
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal class BlobTransferDataSource(
    private val blobGateway: BlobGateway,
    private val blobCipher: BlobCipher,
    private val cryptoHash: CryptoHash,
    private val secureRandomGenerator: SecureRandomGenerator
) {
    suspend fun upload(
        plaintext: ByteArray,
        retentionMilliseconds: Long
    ): Pair<EncryptedBlobReferenceDto, String> {
        require(plaintext.isNotEmpty()) { "Attachment blob must not be empty" }
        require(retentionMilliseconds > 0L) { "Blob retention must be positive" }

        val blobId = IdGenerator.generate(prefix = "blob")
        val readCapability = capability()
        val deleteCapability = capability()
        val uploadCapability = capability()
        val associatedData = associatedData(blobId)
        val encrypted = blobCipher.encrypt(plaintext, associatedData).getOrThrow()
        check(encrypted.ciphertext.size.toLong() <= MAX_BLOB_CIPHERTEXT_BYTES) {
            "Encrypted blob exceeds the supported client size"
        }
        val now = SystemClock.nowEpochMilliseconds()
        val effectiveRetentionMilliseconds =
            if (retentionMilliseconds >= CLOCK_SKEW_MARGIN_THRESHOLD_MILLISECONDS) {
                retentionMilliseconds - CLOCK_SKEW_MARGIN_MILLISECONDS
            } else {
                retentionMilliseconds
            }
        check(effectiveRetentionMilliseconds <= Long.MAX_VALUE - now) {
            "Blob retention overflows its expiry"
        }
        val expiresAt = now + effectiveRetentionMilliseconds
        val stored =
            blobGateway.upload(
                BlobUpload(
                    blobId = blobId,
                    ciphertext = encrypted.ciphertext,
                    readCapabilitySha256 = cryptoHash.sha256(readCapability.encodeToByteArray()).hex(),
                    deleteCapabilitySha256 = cryptoHash.sha256(deleteCapability.encodeToByteArray()).hex(),
                    uploadCapability = uploadCapability,
                    uploadCapabilitySha256 = cryptoHash.sha256(uploadCapability.encodeToByteArray()).hex(),
                    expiresAtEpochMilliseconds = expiresAt
                )
            )

        return EncryptedBlobReferenceDto(
            nodeId = stored.nodeId,
            blobId = blobId,
            readCapability = readCapability,
            ciphertextByteSize = encrypted.ciphertext.size.toLong(),
            expiresAtEpochMilliseconds = stored.expiresAtEpochMilliseconds,
            encryptionKey = encrypted.key,
            nonce = encrypted.nonce,
            ciphertextSha256 = cryptoHash.sha256(encrypted.ciphertext)
        ) to deleteCapability
    }

    suspend fun download(reference: EncryptedBlobReferenceDto): ByteArray {
        val ciphertext = blobGateway.download(reference.nodeId, reference.blobId, reference.readCapability, reference.ciphertextByteSize)
        check(cryptoHash.sha256(ciphertext).contentEquals(reference.ciphertextSha256)) {
            "Blob ciphertext hash mismatch"
        }
        return blobCipher
            .decrypt(
                ciphertext = ciphertext,
                key = reference.encryptionKey,
                nonce = reference.nonce,
                associatedData = associatedData(reference.blobId)
            ).getOrThrow()
    }

    suspend fun delete(
        reference: EncryptedBlobReferenceDto,
        deleteCapability: String
    ) {
        blobGateway.delete(reference.nodeId, reference.blobId, deleteCapability)
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
        const val CLOCK_SKEW_MARGIN_MILLISECONDS = 5L * 60L * 1_000L
        const val CLOCK_SKEW_MARGIN_THRESHOLD_MILLISECONDS = 24L * 60L * 60L * 1_000L
        const val CAPABILITY_BYTES = 32
        const val MAX_BLOB_CIPHERTEXT_BYTES = 128L * 1024L * 1024L
        const val HEX = "0123456789abcdef"
    }
}
