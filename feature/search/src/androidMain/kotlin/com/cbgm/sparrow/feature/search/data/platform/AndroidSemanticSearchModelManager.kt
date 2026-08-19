package com.cbgm.sparrow.feature.search.data.platform

import android.content.Context
import com.cbgm.sparrow.feature.search.data.model.SemanticSearchModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class AndroidSemanticSearchModelManager(
    context: Context
) : SemanticSearchModelManager {
    private val modelDirectory = File(context.filesDir, "semantic-search")
    private val modelFile = File(modelDirectory, SemanticSearchModel.MODEL_FILE_NAME)
    private val hashFile = File(modelDirectory, "${SemanticSearchModel.MODEL_FILE_NAME}.sha256")

    override suspend fun isModelReady(): Boolean =
        withContext(Dispatchers.IO) {
            modelFile.isFile && hashFile.isFile && verifyStoredHash()
        }

    override suspend fun downloadAndVerify(onProgress: (Float?) -> Unit) {
        withContext(Dispatchers.IO) {
            modelDirectory.mkdirs()
            val temporaryFile = File(modelDirectory, "${SemanticSearchModel.MODEL_FILE_NAME}.download")
            temporaryFile.delete()

            val connection =
                (URL(SemanticSearchModel.MODEL_URL).openConnection() as HttpURLConnection).apply {
                    connectTimeout = CONNECT_TIMEOUT_MILLIS
                    readTimeout = READ_TIMEOUT_MILLIS
                    instanceFollowRedirects = true
                }
            try {
                connection.connect()
                check(connection.responseCode in 200..299) {
                    "Model download failed with HTTP ${connection.responseCode}"
                }
                val expectedBytes = connection.contentLengthLong.takeIf { it > 0L }
                val digest = MessageDigest.getInstance("SHA-256")
                var downloaded = 0L

                connection.inputStream.buffered().use { input ->
                    temporaryFile.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            digest.update(buffer, 0, count)
                            downloaded += count
                            onProgress(expectedBytes?.let { downloaded.toFloat() / it.toFloat() })
                        }
                    }
                }
                check(temporaryFile.length() > 0L) { "Downloaded semantic search model is empty" }
                val sha256 = digest.digest().toHex()

                // The first HTTPS download pins the exact bytes locally. Every later load verifies
                // those bytes before MediaPipe sees them. A release-pinned signed manifest can
                // replace this TOFU pin without changing the model/index architecture.
                modelFile.delete()
                temporaryFile.copyTo(modelFile, overwrite = true)
                temporaryFile.delete()
                hashFile.writeText(sha256)
                check(verifyStoredHash()) { "Semantic search model integrity verification failed" }
                onProgress(1f)
            } finally {
                connection.disconnect()
                temporaryFile.delete()
            }
        }
    }

    override suspend fun deleteModel() {
        withContext(Dispatchers.IO) {
            modelFile.delete()
            hashFile.delete()
            modelDirectory.delete()
        }
    }

    fun requireVerifiedModelFile(): File {
        check(modelFile.isFile && hashFile.isFile && verifyStoredHash()) {
            "Semantic search model is missing or failed integrity verification"
        }
        return modelFile
    }

    private fun verifyStoredHash(): Boolean {
        val expected = hashFile.takeIf { it.isFile }?.readText()?.trim().orEmpty()
        if (expected.length != SHA256_HEX_LENGTH || !modelFile.isFile) return false
        val digest = MessageDigest.getInstance("SHA-256")
        modelFile.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return MessageDigest.isEqual(expected.encodeToByteArray(), digest.digest().toHex().encodeToByteArray())
    }

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 120_000
        const val SHA256_HEX_LENGTH = 64
    }
}
