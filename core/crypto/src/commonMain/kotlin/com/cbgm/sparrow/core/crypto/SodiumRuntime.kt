package com.cbgm.sparrow.core.crypto

import com.ionspin.kotlin.crypto.LibsodiumInitializer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Initializes libsodium once for the current application process.
 *
 * All libsodium-backed crypto implementations must check or initialize
 * this runtime before invoking the library.
 */
object SodiumRuntime {
    private val initializationMutex = Mutex()

    @Volatile
    private var initialized = false

    @Volatile
    private var initializationFailure: Throwable? = null

    suspend fun initialize(): Result<Unit> {
        if (initialized) {
            return Result.success(Unit)
        }

        initializationFailure?.let { error ->
            return Result.failure(error)
        }

        return initializationMutex.withLock {
            if (initialized) {
                return@withLock Result.success(Unit)
            }

            initializationFailure?.let { error ->
                return@withLock Result.failure(error)
            }

            runCatching {
                LibsodiumInitializer.initialize()

                initialized = true
            }.onFailure { error ->
                initializationFailure = error
            }
        }
    }

    fun isInitialized(): Boolean = initialized

    fun requireInitialized() {
        check(initialized) {
            "libsodium is not initialized"
        }
    }
}
