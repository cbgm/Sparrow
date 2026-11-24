package com.cbgm.sparrow.protocol.attachment

/** Sends, fetches and deletes encrypted blobs at their nodes, in chunks, each transfer through a circuit of its own. */
interface BlobTransferSource {
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
