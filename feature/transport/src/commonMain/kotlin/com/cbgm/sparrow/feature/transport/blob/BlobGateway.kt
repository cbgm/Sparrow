package com.cbgm.sparrow.feature.transport.blob

/**
 * An encrypted blob to send: the app chooses its ID and capabilities, the node keeps only their hashes.
 * [uploadCapability] lets this device send its chunks and is never shared.
 */
class BlobUpload(
    val blobId: String,
    val ciphertext: ByteArray,
    val readCapabilitySha256: String,
    val deleteCapabilitySha256: String,
    val uploadCapability: String,
    val uploadCapabilitySha256: String,
    val expiresAtEpochMilliseconds: Long
)

/** Where a sent blob is kept and until when. */
class StoredBlob(
    val nodeId: String,
    val expiresAtEpochMilliseconds: Long
)

/** A file could not be sent or fetched: the blob node refused it, or every attempt through a new circuit failed. */
class FileTransferFailedException(
    cause: Throwable
) : IllegalStateException("File transfer failed: ${cause.message}", cause)

/** True when this failure, or one behind it, is a [FileTransferFailedException]. */
fun Throwable.isFileTransferFailure(): Boolean = generateSequence(this) { failure -> failure.cause }.take(MAXIMUM_CAUSES).any { it is FileTransferFailedException }

private const val MAXIMUM_CAUSES = 8

/** Sends, fetches and deletes encrypted blobs at their nodes, in chunks, each transfer through a circuit of its own. */
interface BlobGateway {
    suspend fun upload(blob: BlobUpload): StoredBlob

    /** The [ciphertextByteSize] bytes of blob [blobId] at [nodeId]. */
    suspend fun download(
        nodeId: String,
        blobId: String,
        readCapability: String,
        ciphertextByteSize: Long
    ): ByteArray

    suspend fun delete(
        nodeId: String,
        blobId: String,
        deleteCapability: String
    )
}
