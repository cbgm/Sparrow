package com.cbgm.sparrow.feature.transport.domain.repository

import com.cbgm.sparrow.feature.transport.domain.model.BlobUpload
import com.cbgm.sparrow.feature.transport.domain.model.StoredBlob

/** Sends, fetches and deletes encrypted blobs at their nodes, in chunks, each transfer through a circuit of its own. */
interface BlobRepository {
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
