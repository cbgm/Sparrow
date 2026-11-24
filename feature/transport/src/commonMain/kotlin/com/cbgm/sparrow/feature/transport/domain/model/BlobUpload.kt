package com.cbgm.sparrow.feature.transport.domain.model

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
