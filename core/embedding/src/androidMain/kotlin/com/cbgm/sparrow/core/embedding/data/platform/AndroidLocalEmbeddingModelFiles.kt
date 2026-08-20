package com.cbgm.sparrow.core.embedding.data.platform

import android.content.Context
import com.cbgm.sparrow.core.embedding.data.model.LocalEmbeddingModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class AndroidLocalEmbeddingModelFiles(
    context: Context
) {
    // Reuse the existing semantic-search directory so current installs keep their downloaded model.
    private val modelDirectory = File(context.filesDir, "semantic-search")
    val modelFile = File(modelDirectory, LocalEmbeddingModel.MODEL_FILE_NAME)
    val partialFile = File(modelDirectory, "${LocalEmbeddingModel.MODEL_FILE_NAME}.download")
    private val hashFile = File(modelDirectory, "${LocalEmbeddingModel.MODEL_FILE_NAME}.sha256")

    suspend fun isModelReady(): Boolean =
        withContext(Dispatchers.IO) {
            modelFile.isFile && hashFile.isFile && verifyStoredHash()
        }

    suspend fun deleteAll() {
        withContext(Dispatchers.IO) {
            modelFile.delete()
            partialFile.delete()
            hashFile.delete()
            modelDirectory.delete()
        }
    }

    fun ensureDirectory() {
        check(modelDirectory.exists() || modelDirectory.mkdirs()) {
            "Could not create local embedding model directory"
        }
    }

    fun promoteVerifiedDownload(sha256: String) {
        ensureDirectory()
        check(partialFile.isFile && partialFile.length() > 0L) {
            "Downloaded local embedding model is empty"
        }

        modelFile.delete()
        check(
            partialFile.renameTo(modelFile) || run {
                partialFile.copyTo(modelFile, overwrite = true)
                partialFile.delete()
                true
            }
        ) {
            "Could not install local embedding model"
        }
        hashFile.writeText(sha256)
        check(verifyStoredHash()) {
            "Local embedding model integrity verification failed"
        }
    }

    fun requireVerifiedModelFile(): File {
        check(modelFile.isFile && hashFile.isFile && verifyStoredHash()) {
            "Local embedding model is missing or failed integrity verification"
        }
        return modelFile
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().toHex()
    }

    private fun verifyStoredHash(): Boolean {
        val expected = hashFile.takeIf { it.isFile }?.readText()?.trim().orEmpty()
        if (expected.length != SHA256_HEX_LENGTH || !modelFile.isFile) return false
        val actual = sha256(modelFile)
        return MessageDigest.isEqual(expected.encodeToByteArray(), actual.encodeToByteArray())
    }

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }

    private companion object {
        const val SHA256_HEX_LENGTH = 64
    }
}
