package com.cbgm.sparrow.feature.search.data.platform

import com.cbgm.sparrow.feature.search.data.model.SemanticSearchModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AndroidSemanticSearchModelDownloader(
    private val modelFiles: AndroidSemanticSearchModelFiles
) {
    suspend fun downloadAndVerify(onProgress: suspend (Float?) -> Unit) {
        withContext(Dispatchers.IO) {
            modelFiles.ensureDirectory()
            downloadResumable(onProgress)
            coroutineContext.ensureActive()

            val sha256 = modelFiles.sha256(modelFiles.partialFile)
            modelFiles.promoteVerifiedDownload(sha256)
            onProgress(1f)
        }
    }

    private suspend fun downloadResumable(onProgress: suspend (Float?) -> Unit) {
        var existingBytes = modelFiles.partialFile.takeIf { it.isFile }?.length() ?: 0L
        var connection = openConnection(existingBytes)
        val cancellationHandle =
            currentCoroutineContext().job.invokeOnCompletion { cause ->
                if (cause is CancellationException) {
                    connection.disconnect()
                }
            }

        try {
            withContext(Dispatchers.IO) {
                connection.connect()
            }

            if (existingBytes > 0L && connection.responseCode == HTTP_RANGE_NOT_SATISFIABLE) {
                val remoteSize = parseUnsatisfiedRangeSize(connection.getHeaderField("Content-Range"))
                if (remoteSize == existingBytes) {
                    onProgress(1f)
                    return
                }

                connection.disconnect()
                modelFiles.partialFile.delete()
                existingBytes = 0L
                connection = openConnection(existingBytes)
                withContext(Dispatchers.IO) {
                    connection.connect()
                }
            }

            val responseCode = connection.responseCode
            check(responseCode in 200..299) {
                "Model download failed with HTTP $responseCode"
            }

            val append = existingBytes > 0L && responseCode == HttpURLConnection.HTTP_PARTIAL
            if (existingBytes > 0L && !append) {
                existingBytes = 0L
            }

            val responseBytes = connection.contentLengthLong.takeIf { it > 0L }
            val expectedBytes = responseBytes?.let { it + existingBytes }
            var downloadedBytes = existingBytes

            onProgress(expectedBytes?.let { downloadedBytes.toFloat() / it.toFloat() })

            connection.inputStream.buffered().use { input ->
                FileOutputStream(modelFiles.partialFile, append).buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var lastReportedPercent = progressPercent(downloadedBytes, expectedBytes)

                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloadedBytes += count

                        val percent = progressPercent(downloadedBytes, expectedBytes)
                        if (percent == null || percent != lastReportedPercent) {
                            lastReportedPercent = percent
                            onProgress(expectedBytes?.let { downloadedBytes.toFloat() / it.toFloat() })
                        }
                    }
                }
            }

            check(modelFiles.partialFile.length() > 0L) {
                "Downloaded semantic search model is empty"
            }
            if (expectedBytes != null) {
                check(modelFiles.partialFile.length() == expectedBytes) {
                    "Semantic search model download ended before all bytes were received"
                }
            }
        } finally {
            cancellationHandle.dispose()
            connection.disconnect()
        }
    }

    private fun openConnection(existingBytes: Long): HttpURLConnection =
        (URL(SemanticSearchModel.MODEL_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            instanceFollowRedirects = true
            setRequestProperty("Accept-Encoding", "identity")
            if (existingBytes > 0L) {
                setRequestProperty("Range", "bytes=$existingBytes-")
            }
        }

    private fun parseUnsatisfiedRangeSize(contentRange: String?): Long? =
        contentRange
            ?.substringAfter("*/", missingDelimiterValue = "")
            ?.toLongOrNull()

    private fun progressPercent(
        downloadedBytes: Long,
        expectedBytes: Long?
    ): Int? =
        expectedBytes
            ?.takeIf { it > 0L }
            ?.let { ((downloadedBytes * 100L) / it).coerceIn(0L, 100L).toInt() }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 120_000
        const val HTTP_RANGE_NOT_SATISFIABLE = 416
    }
}
