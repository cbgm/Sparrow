package com.cbgm.sparrow.feature.identity.domain.usecase.restore

import com.cbgm.sparrow.core.result.flatMap
import com.cbgm.sparrow.core.result.safeCall
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.media.domain.repository.MediaSelectionFileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class ReadIdentityRestoreDocumentUseCase(
    private val selectedFiles: MediaSelectionFileRepository
) {
    suspend operator fun invoke(path: String, declaredByteSize: Long): Result<ByteArray> {
        var document: ByteArray? = null
        var transferred = false
        try {
            val read = try {
                safeSuspendCall(logFailure = false) {
                    safeCall(logFailure = false) {
                        require(declaredByteSize in 1..MAX_DOCUMENT_BYTES) { "Invalid backup size" }
                    }.flatMap {
                        selectedFiles.read(path).flatMap { bytes ->
                            document = bytes
                            safeCall(logFailure = false) {
                                require(bytes.size.toLong() in 1..MAX_DOCUMENT_BYTES) { "Invalid backup size" }
                                bytes
                            }
                        }
                    }
                }.flatMap { it }
            } catch (cancelled: CancellationException) {
                deleteCopy(path)
                throw cancelled
            }
            val result = deleteCopy(path).flatMap { read }
            transferred = result.isSuccess
            return result
        } finally {
            if (!transferred) document?.fill(0)
        }
    }

    private suspend fun deleteCopy(path: String): Result<Unit> =
        withContext(NonCancellable) {
            safeSuspendCall(logFailure = false) { selectedFiles.delete(path) }.flatMap { it }
        }

    companion object {
        const val MAX_DOCUMENT_BYTES = 16_384L
    }
}
