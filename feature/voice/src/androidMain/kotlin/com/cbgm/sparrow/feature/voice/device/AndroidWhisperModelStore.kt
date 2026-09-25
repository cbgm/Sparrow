package com.cbgm.sparrow.feature.voice.device

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

internal class AndroidWhisperModelStore(
    private val context: Context
) {
    fun requireModel(): File {
        val directory = File(context.filesDir, MODEL_DIRECTORY).apply { mkdirs() }
        val modelFile = File(directory, MODEL_FILE_NAME)

        if (modelFile.isFile && modelFile.sha256() == MODEL_SHA256) {
            return modelFile
        }

        cleanupLegacyModel(directory)
        modelFile.delete()
        val partialFile = File(directory, "$MODEL_FILE_NAME.part")
        partialFile.delete()

        try {
            download(partialFile)
            check(partialFile.sha256() == MODEL_SHA256) {
                "Downloaded voice transcription model failed integrity verification"
            }
            check(partialFile.renameTo(modelFile)) {
                "Could not install the voice transcription model"
            }
        } catch (error: Throwable) {
            partialFile.delete()
            throw error
        }

        return modelFile
    }

    private fun cleanupLegacyModel(directory: File) {
        File(directory, LEGACY_MODEL_FILE_NAME)
            .takeIf(File::isFile)
            ?.delete()
    }

    private fun download(destination: File) {
        val startedAtNanos = System.nanoTime()
        val connection =
            (URL(MODEL_DOWNLOAD_URL).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = CONNECT_TIMEOUT_MILLISECONDS
                readTimeout = READ_TIMEOUT_MILLISECONDS
                requestMethod = "GET"
            }

        try {
            connection.connect()
            check(connection.responseCode in 200..299) {
                "Could not download the voice transcription model (HTTP ${connection.responseCode})"
            }

            connection.inputStream.buffered().use { input ->
                destination.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                    while (true) {
                        check(
                            System.nanoTime() - startedAtNanos <=
                                TimeUnit.MILLISECONDS.toNanos(DOWNLOAD_TIMEOUT_MILLISECONDS)
                        ) {
                            "Voice transcription model download timed out"
                        }

                        val read = input.read(buffer)
                        if (read < 0) break
                        if (read > 0) output.write(buffer, 0, read)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().buffered().use { input ->
            val buffer = ByteArray(HASH_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString(separator = "") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
    }

    private companion object {
        const val MODEL_DIRECTORY = "voice-transcription"
        const val MODEL_FILE_NAME = "ggml-base-q5_1.bin"
        const val LEGACY_MODEL_FILE_NAME = "ggml-base.bin"
        const val MODEL_DOWNLOAD_URL =
            "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin"
        const val MODEL_SHA256 = "422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898"
        const val CONNECT_TIMEOUT_MILLISECONDS = 20_000
        const val READ_TIMEOUT_MILLISECONDS = 30_000
        const val DOWNLOAD_TIMEOUT_MILLISECONDS = 180_000L
        const val DOWNLOAD_BUFFER_SIZE = 64 * 1024
        const val HASH_BUFFER_SIZE = 64 * 1024
    }
}
