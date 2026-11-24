package com.cbgm.sparrow.feature.transport.domain.gateway

interface ControlPlaneDirectorySynchronizer {
    /** Restore only a locally stored, cryptographically authenticated directory, without network I/O. */
    suspend fun restoreCached(): Result<Int> = Result.success(0)

    suspend fun refresh(): Result<Int>

    suspend fun synchronizeFrom(url: String): Result<Int>

    /** Import a separately configured, unsigned JSON feed on explicit user request. */
    suspend fun importJsonDirectory(url: String): Result<Int> =
        Result.failure(UnsupportedOperationException("JSON directory is unavailable"))

    /** Explicit directory removal only; no automatic reset after network/signature failures. */
    suspend fun forgetDirectoryTrustOnUserRemoval(): Result<Unit> = Result.success(Unit)
}
