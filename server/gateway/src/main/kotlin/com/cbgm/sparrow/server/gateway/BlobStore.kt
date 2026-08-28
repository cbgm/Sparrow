package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.protocol.BlobMetadata
import com.cbgm.sparrow.server.protocol.BlobUploadTicketClaims
import com.cbgm.sparrow.server.protocol.serverJson
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
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
                        if (metadata.expiresAtEpochMilliseconds <= nowEpochMilliseconds) {
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

    private fun calculateStoredBytes(): Long {
        if (!root.exists()) return 0L
        return Files.walk(root).use { stream ->
            stream
                .filter { path -> path.fileName.toString().endsWith(DATA_SUFFIX) }
                .mapToLong { path -> runCatching { Files.size(path) }.getOrDefault(0L) }
                .sum()
        }
    }

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
        if (!updateUsage) {
            Files.deleteIfExists(paths.data)
            Files.deleteIfExists(paths.metadata)
            return
        }
        synchronized(capacityLock) {
            val removedBytes =
                if (paths.data.exists()) {
                    runCatching { Files.size(paths.data) }.getOrDefault(0L)
                } else {
                    0L
                }
            val dataDeleted = Files.deleteIfExists(paths.data)
            Files.deleteIfExists(paths.metadata)
            if (dataDeleted && removedBytes > 0L) {
                storedBytes = (storedBytes - removedBytes).coerceAtLeast(0L)
            }
        }
    }

    private fun paths(blobId: String): BlobPaths {
        requireBlobId(blobId)
        val shard = blobId.take(SHARD_LENGTH)
        val directory = root.resolve(shard)
        return BlobPaths(
            directory = directory,
            data = directory.resolve("$blobId$DATA_SUFFIX"),
            metadata = directory.resolve("$blobId$METADATA_SUFFIX")
        )
    }

    private fun requireBlobId(blobId: String) {
        require(BLOB_ID.matches(blobId)) { "Invalid blob ID" }
    }

    private fun sha256Hex(value: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }

    private fun constantTimeEquals(left: String, right: String): Boolean =
        MessageDigest.isEqual(
            left.toByteArray(StandardCharsets.US_ASCII),
            right.toByteArray(StandardCharsets.US_ASCII)
        )

    private data class BlobPaths(
        val directory: Path,
        val data: Path,
        val metadata: Path
    )

    private companion object {
        val BLOB_ID = Regex("[A-Za-z0-9_-]{16,128}")
        const val SHARD_LENGTH = 2
        const val BUFFER_BYTES = 64 * 1024
        const val DATA_SUFFIX = ".blob"
        const val METADATA_SUFFIX = ".meta.json"
    }
}

class BlobTooLargeException : IllegalArgumentException("Blob exceeds its allowed byte size")

class BlobAlreadyExistsException : IllegalStateException("Blob already exists")

class BlobStorageCapacityExceededException : IllegalStateException("Blob storage capacity exceeded")
