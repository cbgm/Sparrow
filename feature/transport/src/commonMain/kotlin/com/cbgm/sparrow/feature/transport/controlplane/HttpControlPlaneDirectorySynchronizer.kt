package com.cbgm.sparrow.feature.transport.controlplane

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.core.transport.ControlPlaneDirectorySynchronizer
import com.cbgm.sparrow.data.datastore.SparrowDataStore
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/** Coordinates independent signed and unsigned discovery sources; never treats JSON hints as trust pins. */
class HttpControlPlaneDirectorySynchronizer(
    httpClient: HttpClient,
    private val configuration: ControlPlaneConfiguration,
    verifier: SignedControlPlaneDirectoryVerifier,
    dataStore: SparrowDataStore,
    json: Json
) : ControlPlaneDirectorySynchronizer {
    private val logger = SparrowLog.withTag("ControlPlaneDirectory")
    private val mutex = Mutex()
    private val remote = ControlPlaneDirectoryRemoteSource(httpClient, json)
    private val verified = VerifiedControlPlaneDirectoryState(configuration, verifier, dataStore, json)

    override suspend fun restoreCached(): Result<Int> = mutex.withLock {
        safely {
            verified.restore()
            discoveredCount()
        }
    }

    override suspend fun refresh(): Result<Int> = mutex.withLock {
        // A failed signed source must not prevent an independent JSON source from refreshing.
        val signed = configuration.directoryUrl.value?.let { synchronize(it, saveUrl = false) }
        val unsigned = configuration.jsonDirectoryUrl.value?.let { importJson(it) }
        when {
            signed?.isFailure == true && unsigned?.isSuccess != true -> signed
            unsigned?.isFailure == true && signed?.isSuccess != true -> unsigned
            else -> Result.success(discoveredCount())
        }
    }

    override suspend fun synchronizeFrom(url: String): Result<Int> = mutex.withLock {
        synchronize(url, saveUrl = true)
    }

    override suspend fun importJsonDirectory(url: String): Result<Int> = mutex.withLock {
        importJson(url)
    }

    override suspend fun forgetDirectoryTrustOnUserRemoval(): Result<Unit> = mutex.withLock {
        safely { verified.forgetAfterRemoval() }
    }

    private suspend fun synchronize(url: String, saveUrl: Boolean): Result<Int> {
        val result = safely {
            verified.restore()
            verified.accept(remote.fetchSigned(url), saveUrl)
        }
        if (result.isFailure && verified.hasCachedSnapshot) {
            logger.debug {
                "Directory refresh unavailable/rejected; preserving last authenticated snapshot: " +
                    (result.exceptionOrNull()?.message ?: "unknown error")
            }
        }
        return result
    }

    private suspend fun importJson(url: String): Result<Int> = safely {
        val addresses = remote.fetchJsonAddresses(url)
        configuration.importManual(addresses).getOrThrow()
        addresses.size
    }

    private fun discoveredCount(): Int =
        configuration.directoryBaseUrls.value.size + configuration.jsonDirectoryBaseUrls.value.size

    private suspend fun <T> safely(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (error: Throwable) {
        if (error is CancellationException) throw error
        Result.failure(error)
    }
}
