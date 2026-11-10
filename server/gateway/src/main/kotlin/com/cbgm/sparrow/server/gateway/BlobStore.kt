package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.protocol.BlobMetadata
import com.cbgm.sparrow.server.protocol.BlobUploadTicketClaims
import com.cbgm.sparrow.server.protocol.OpenBlobRequest
import com.cbgm.sparrow.server.protocol.serverJson
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText

class BlobStore(
    private val root: Path,
    private val maximumBlobBytes: Long,
    private val maximumStorageBytes: Long
) {
    private val capacityLock = Any()
    private var storedBytes = 0L
    private var reservedBytes = 0L

    init {
        require(maximumBlobBytes > 0L)
        require(maximumStorageBytes >= maximumBlobBytes) {
            "Maximum blob storage must be at least one maximum-size blob"
        }
        root.createDirectories()
        removeOrphanedDataFiles()
        purgeExpiredInternal(System.currentTimeMillis(), updateUsage = false)
        storedBytes = calculateStoredBytes()
        check(storedBytes <= maximumStorageBytes) {
            "Existing blob storage exceeds the configured maximum"
        }
    }

    suspend fun store(
        claims: BlobUploadTicketClaims,
        channel: ByteReadChannel
    ): BlobMetadata {
        requireBlobId(claims.blobId)
        val allowedBytes = minOf(claims.maximumBytes, maximumBlobBytes)
        reserveCapacity(allowedBytes)

        val paths = paths(claims.blobId)
        withContext(Dispatchers.IO) {
            paths.directory.createDirectories()
        }
        var reservationCommitted = false
        try {
            val written = writeBlobData(paths.data, channel, allowedBytes)
            try {
                validateBlobSize(written, claims.maximumBytes)
                val metadata = claims.toBlobMetadata(written)
                writeMetadataAndCommit(
                    metadataPath = paths.metadata,
                    metadata = metadata,
                    reservedBytes = allowedBytes,
                    storedBytes = written
                )
                reservationCommitted = true
                return metadata
            } catch (error: Exception) {
                withContext(Dispatchers.IO) {
                    Files.deleteIfExists(paths.data)
                }
                throw error
            }
        } finally {
            if (!reservationCommitted) {
                releaseCapacity(allowedBytes)
            }
        }
    }

    private suspend fun writeBlobData(
        dataPath: Path,
        channel: ByteReadChannel,
        allowedBytes: Long
    ): Long =
        withContext(Dispatchers.IO) {
            var created = false
            try {
                Files.newOutputStream(
                    dataPath,
                    java.nio.file.StandardOpenOption.CREATE_NEW,
                    java.nio.file.StandardOpenOption.WRITE
                ).also { created = true }.use { output ->
                    copyBlobData(channel, output, allowedBytes)
                }
            } catch (_: java.nio.file.FileAlreadyExistsException) {
                throw BlobAlreadyExistsException()
            } catch (error: Exception) {
                if (created) {
                    Files.deleteIfExists(dataPath)
                }
                throw error
            }
        }

    private suspend fun copyBlobData(
        channel: ByteReadChannel,
        output: java.io.OutputStream,
        allowedBytes: Long
    ): Long =
        withContext(Dispatchers.IO) {
            val buffer = ByteArray(BUFFER_BYTES)
            var written = 0L
            var read = channel.readAvailable(buffer)
            while (read >= 0) {
                if (read > 0) {
                    written += read
                    if (written > allowedBytes) {
                        throw BlobTooLargeException()
                    }
                    output.write(buffer, 0, read)
                }
                read = channel.readAvailable(buffer)
            }
            written
        }

    private fun validateBlobSize(
        written: Long,
        expectedBytes: Long
    ) {
        require(written > 0L) { "Blob must not be empty" }
        require(written == expectedBytes) {
            "Blob byte size does not match the upload ticket"
        }
    }

    private fun BlobUploadTicketClaims.toBlobMetadata(written: Long): BlobMetadata =
        BlobMetadata(
            blobId = blobId,
            byteSize = written,
            readCapabilitySha256 = readCapabilitySha256,
            deleteCapabilitySha256 = deleteCapabilitySha256,
            expiresAtEpochMilliseconds = blobExpiresAtEpochMilliseconds
        )

    private suspend fun writeMetadataAndCommit(
        metadataPath: Path,
        metadata: BlobMetadata,
        reservedBytes: Long,
        storedBytes: Long
    ) =
        withContext(Dispatchers.IO) {
            synchronized(capacityLock) {
                writeMetadata(metadataPath, metadata)
                commitCapacity(reservedBytes = reservedBytes, storedBytes = storedBytes)
            }
        }

    private fun writeMetadata(
        metadataPath: Path,
        metadata: BlobMetadata
    ) {
        try {
            Files.writeString(
                metadataPath,
                serverJson.encodeToString(metadata),
                java.nio.file.StandardOpenOption.CREATE_NEW,
                java.nio.file.StandardOpenOption.WRITE
            )
        } catch (_: java.nio.file.FileAlreadyExistsException) {
            throw BlobAlreadyExistsException()
        }
    }

    /**
     * Opens a blob that arrives in chunks of [CHUNK_BYTES]: its whole size is held from now on, so a started upload
     * never runs out of room. Chunks are accepted until [uploadExpiresAtEpochMilliseconds].
     */
    fun open(
        request: OpenBlobRequest,
        uploadExpiresAtEpochMilliseconds: Long
    ): BlobMetadata {
        requireBlobId(request.blobId)
        if (request.byteSize !in 1..maximumBlobBytes) throw BlobTooLargeException()
        val metadata =
            BlobMetadata(
                blobId = request.blobId,
                byteSize = request.byteSize,
                readCapabilitySha256 = request.readCapabilitySha256,
                deleteCapabilitySha256 = request.deleteCapabilitySha256,
                expiresAtEpochMilliseconds = request.expiresAtEpochMilliseconds,
                chunkCount = ((request.byteSize + CHUNK_BYTES - 1) / CHUNK_BYTES).toInt(),
                uploadCapabilitySha256 = request.uploadCapabilitySha256,
                uploadExpiresAtEpochMilliseconds = uploadExpiresAtEpochMilliseconds,
                complete = false
            )
        val paths = paths(request.blobId)
        synchronized(capacityLock) {
            if (paths.metadata.exists() || paths.data.exists()) throw BlobAlreadyExistsException()
            if (request.byteSize > maximumStorageBytes - storedBytes - reservedBytes) throw BlobStorageCapacityExceededException()
            paths.chunks.createDirectories()
            writeMetadata(paths.metadata, metadata)
            storedBytes += request.byteSize
        }
        return metadata
    }

    /**
     * Stores chunk [index] of an open blob and answers its SHA-256; null when the blob is not open for this
     * capability. A chunk may be sent again (its answer was lost); the blob is complete once every chunk is there.
     */
    fun storeChunk(
        blobId: String,
        uploadCapability: String,
        index: Int,
        bytes: ByteArray,
        nowEpochMilliseconds: Long
    ): String? {
        val metadata = uploading(blobId, uploadCapability, nowEpochMilliseconds) ?: return null
        val chunkCount = metadata.chunkCount ?: return null
        require(index in 0 until chunkCount) { "Invalid chunk index" }
        require(bytes.size.toLong() == metadata.chunkSize(index)) { "Chunk size does not match the blob" }
        val paths = paths(blobId)
        val part = paths.chunks.resolve("$index$CHUNK_SUFFIX.${System.nanoTime()}$PART_SUFFIX")
        Files.write(part, bytes, java.nio.file.StandardOpenOption.CREATE_NEW, java.nio.file.StandardOpenOption.WRITE)
        Files.move(part, paths.chunk(index), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        if ((0 until chunkCount).all { chunk -> paths.chunk(chunk).exists() }) markComplete(paths.metadata, metadata)
        return sha256Hex(bytes)
    }

    /** The chunks an open blob already holds; null when it is not open for this capability. */
    fun storedChunks(
        blobId: String,
        uploadCapability: String,
        nowEpochMilliseconds: Long
    ): List<Int>? {
        val metadata = uploading(blobId, uploadCapability, nowEpochMilliseconds) ?: metadata(blobId)?.takeIf { it.complete && it.opensUpload(uploadCapability) }
        val chunkCount = metadata?.chunkCount ?: return null
        val paths = paths(blobId)
        return (0 until chunkCount).filter { chunk -> paths.chunk(chunk).exists() }
    }

    /** Chunk [index] of a complete blob, for whoever holds its read capability. */
    fun readableChunk(
        blobId: String,
        readCapability: String,
        index: Int,
        nowEpochMilliseconds: Long
    ): Path? {
        val metadata = metadata(blobId) ?: return null
        if (metadata.expiresAtEpochMilliseconds <= nowEpochMilliseconds) {
            deleteFiles(blobId)
            return null
        }
        val chunkCount = metadata.chunkCount ?: return null
        val readable = metadata.complete && index in 0 until chunkCount && constantTimeEquals(metadata.readCapabilitySha256, sha256Hex(readCapability))
        return paths(blobId).chunk(index).takeIf { readable && it.exists() }
    }

    private fun uploading(
        blobId: String,
        uploadCapability: String,
        nowEpochMilliseconds: Long
    ): BlobMetadata? =
        metadata(blobId)?.takeIf { metadata ->
            !metadata.complete &&
                (metadata.uploadExpiresAtEpochMilliseconds ?: 0L) > nowEpochMilliseconds &&
                metadata.opensUpload(uploadCapability)
        }

    private fun BlobMetadata.opensUpload(uploadCapability: String): Boolean =
        uploadCapabilitySha256?.let { hash -> constantTimeEquals(hash, sha256Hex(uploadCapability)) } == true

    private fun BlobMetadata.chunkSize(index: Int): Long = minOf(CHUNK_BYTES.toLong(), byteSize - index.toLong() * CHUNK_BYTES)

    private fun markComplete(
        metadataPath: Path,
        metadata: BlobMetadata
    ) {
        val part = metadataPath.resolveSibling("${metadataPath.fileName}.${System.nanoTime()}$PART_SUFFIX")
        Files.writeString(part, serverJson.encodeToString(metadata.copy(complete = true)))
        Files.move(part, metadataPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    fun readableBlob(
        blobId: String,
        readCapability: String,
        nowEpochMilliseconds: Long
    ): Path? {
        val metadata = metadata(blobId) ?: return null
        if (metadata.expiresAtEpochMilliseconds <= nowEpochMilliseconds) {
            deleteFiles(blobId)
            return null
        }
        if (!constantTimeEquals(metadata.readCapabilitySha256, sha256Hex(readCapability))) {
            return null
        }
        return paths(blobId).data.takeIf(Path::exists)
    }

    fun delete(
        blobId: String,
        deleteCapability: String,
        nowEpochMilliseconds: Long
    ): Boolean {
        val metadata = metadata(blobId) ?: return false
        if (metadata.expiresAtEpochMilliseconds <= nowEpochMilliseconds) {
            deleteFiles(blobId)
            return false
        }
        if (!constantTimeEquals(metadata.deleteCapabilitySha256, sha256Hex(deleteCapability))) {
            return false
        }
        deleteFiles(blobId)
        return true
    }

    fun purgeExpired(nowEpochMilliseconds: Long) {
        purgeExpiredInternal(nowEpochMilliseconds, updateUsage = true)
    }

    private fun purgeExpiredInternal(
        nowEpochMilliseconds: Long,
        updateUsage: Boolean
    ) {
        if (!root.exists()) return
        Files.walk(root).use { stream ->
            stream
                .filter { path -> path.fileName.toString().endsWith(METADATA_SUFFIX) }
                .forEach { metadataPath ->
                    runCatching {
                        val metadata = serverJson.decodeFromString<BlobMetadata>(metadataPath.readText())
                        val uploadAbandoned = !metadata.complete && (metadata.uploadExpiresAtEpochMilliseconds ?: 0L) <= nowEpochMilliseconds
                        if (metadata.expiresAtEpochMilliseconds <= nowEpochMilliseconds || uploadAbandoned) {
                            deleteFiles(metadata.blobId, updateUsage)
                        }
                    }
                }
        }
    }

    private fun reserveCapacity(bytes: Long) {
        synchronized(capacityLock) {
            val available = maximumStorageBytes - storedBytes - reservedBytes
            if (bytes > available) {
                throw BlobStorageCapacityExceededException()
            }
            reservedBytes += bytes
        }
    }

    private fun commitCapacity(
        reservedBytes: Long,
        storedBytes: Long
    ) {
        synchronized(capacityLock) {
            this.reservedBytes -= reservedBytes
            this.storedBytes += storedBytes
        }
    }

    private fun releaseCapacity(bytes: Long) {
        synchronized(capacityLock) {
            reservedBytes -= bytes
            check(reservedBytes >= 0L) { "Blob storage reservation underflow" }
        }
    }

    /** Whole blobs by their file, chunked ones by the size held for them when they were opened. */
    private fun calculateStoredBytes(): Long {
        if (!root.exists()) return 0L
        return Files.walk(root).use { stream ->
            stream
                .mapToLong { path ->
                    val name = path.fileName.toString()
                    when {
                        name.endsWith(DATA_SUFFIX) -> runCatching { Files.size(path) }.getOrDefault(0L)
                        name.endsWith(METADATA_SUFFIX) -> chunkedBytes(path)
                        else -> 0L
                    }
                }.sum()
        }
    }

    private fun chunkedBytes(metadataPath: Path): Long =
        runCatching { serverJson.decodeFromString<BlobMetadata>(metadataPath.readText()) }
            .getOrNull()
            ?.takeIf { metadata -> metadata.chunkCount != null }
            ?.byteSize
            ?: 0L

    private fun removeOrphanedDataFiles() {
        if (!root.exists()) return
        Files.walk(root).use { stream ->
            stream
                .filter { path -> path.fileName.toString().endsWith(DATA_SUFFIX) }
                .forEach { dataPath ->
                    runCatching {
                        val blobId = dataPath.fileName.toString().removeSuffix(DATA_SUFFIX)
                        if (!paths(blobId).metadata.exists()) {
                            Files.deleteIfExists(dataPath)
                        }
                    }
                }
        }
    }

    private fun metadata(blobId: String): BlobMetadata? =
        runCatching {
            requireBlobId(blobId)
            val path = paths(blobId).metadata
            if (!path.exists()) return null
            serverJson.decodeFromString<BlobMetadata>(path.readText())
        }.getOrNull()

    private fun deleteFiles(
        blobId: String,
        updateUsage: Boolean = true
    ) {
        val paths = paths(blobId)
        synchronized(capacityLock) {
            val dataBytes = if (paths.data.exists()) runCatching { Files.size(paths.data) }.getOrDefault(0L) else 0L
            val chunkedBytes = if (paths.metadata.exists()) chunkedBytes(paths.metadata) else 0L
            val dataDeleted = Files.deleteIfExists(paths.data)
            deleteChunks(paths.chunks)
            val metadataDeleted = Files.deleteIfExists(paths.metadata)
            val removedBytes = (if (dataDeleted) dataBytes else 0L) + (if (metadataDeleted) chunkedBytes else 0L)
            if (updateUsage && removedBytes > 0L) {
                storedBytes = (storedBytes - removedBytes).coerceAtLeast(0L)
            }
        }
    }

    private fun deleteChunks(chunks: Path) {
        if (!chunks.exists()) return
        Files.list(chunks).use { files -> files.forEach { file -> Files.deleteIfExists(file) } }
        Files.deleteIfExists(chunks)
    }

    private fun paths(blobId: String): BlobPaths {
        requireBlobId(blobId)
        val shard = blobId.take(SHARD_LENGTH)
        val directory = root.resolve(shard)
        return BlobPaths(
            directory = directory,
            data = directory.resolve("$blobId$DATA_SUFFIX"),
            metadata = directory.resolve("$blobId$METADATA_SUFFIX"),
            chunks = directory.resolve(blobId)
        )
    }

    private fun requireBlobId(blobId: String) {
        require(BLOB_ID.matches(blobId)) { "Invalid blob ID" }
    }

    private fun sha256Hex(value: String): String = sha256Hex(value.toByteArray(StandardCharsets.UTF_8))

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(bytes)
            .joinToString(separator = "") { byte -> "%02x".format(byte) }

    private fun constantTimeEquals(left: String, right: String): Boolean =
        MessageDigest.isEqual(
            left.toByteArray(StandardCharsets.US_ASCII),
            right.toByteArray(StandardCharsets.US_ASCII)
        )

    private data class BlobPaths(
        val directory: Path,
        val data: Path,
        val metadata: Path,
        val chunks: Path
    ) {
        fun chunk(index: Int): Path = chunks.resolve("$index$CHUNK_SUFFIX")
    }

    companion object {
        /** Every chunk but a blob's last is this large; one fits into a single request through a circuit. */
        const val CHUNK_BYTES = 1_048_576
        private const val CHUNK_SUFFIX = ".chunk"
        private const val PART_SUFFIX = ".part"
        private val BLOB_ID = Regex("[A-Za-z0-9_-]{16,128}")
        private const val SHARD_LENGTH = 2
        private const val BUFFER_BYTES = 64 * 1024
        private const val DATA_SUFFIX = ".blob"
        private const val METADATA_SUFFIX = ".meta.json"
    }
}

class BlobTooLargeException : IllegalArgumentException("Blob exceeds its allowed byte size")

class BlobAlreadyExistsException : IllegalStateException("Blob already exists")

class BlobStorageCapacityExceededException : IllegalStateException("Blob storage capacity exceeded")
