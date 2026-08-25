package com.cbgm.sparrow.server.gateway

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

class BlobUploadPermitCleanupAgent(
    private val permitStore: BlobUploadPermitStore,
    private val cleanupIntervalMilliseconds: Long,
    private val now: () -> Long = System::currentTimeMillis
) {
    init {
        require(cleanupIntervalMilliseconds > 0L)
    }

    suspend fun run() {
        while (currentCoroutineContext().isActive) {
            permitStore.purgeExpired(now())
            delay(cleanupIntervalMilliseconds.milliseconds)
        }
    }
}
