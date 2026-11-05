package com.cbgm.sparrow.server.registry

import com.cbgm.sparrow.server.protocol.AliasClaimKeyResponse
import com.cbgm.sparrow.server.protocol.serverJson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException

/** The presence directory's key that apps seal their phone aliases for; signed into the node directory. */
fun interface AliasClaimKeySource {
    suspend fun current(): ByteArray?
}

/**
 * Asks the presence directory (`PRESENCE_DIRECTORY_URL`) for its key and keeps it for [CACHE_MILLISECONDS], so the
 * key a restarted presence directory generates reaches apps with the next directories. Without the URL, or while the
 * presence directory is unreachable, the directory carries no key and apps leave their phone aliases out.
 */
internal class PresenceAliasClaimKeySource(
    private val httpClient: HttpClient,
    private val presenceDirectoryUrl: String,
    private val now: () -> Long = System::currentTimeMillis
) : AliasClaimKeySource {
    @Volatile
    private var cached: CachedKey? = null

    override suspend fun current(): ByteArray? {
        val currentTime = now()
        cached?.takeIf { key -> currentTime - key.fetchedAtEpochMilliseconds < CACHE_MILLISECONDS }?.let { key ->
            return key.publicKey
        }
        val publicKey =
            try {
                val response = httpClient.get("${presenceDirectoryUrl.trimEnd('/')}/v1/alias-claim-key")
                if (response.status.isSuccess()) response.body<AliasClaimKeyResponse>().publicKey else null
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            }
        cached = publicKey?.let { key -> CachedKey(key, currentTime) }
        return publicKey
    }

    private class CachedKey(
        val publicKey: ByteArray,
        val fetchedAtEpochMilliseconds: Long
    )

    private companion object {
        const val CACHE_MILLISECONDS = 30_000L
    }
}

internal fun presenceAliasClaimKeySource(): AliasClaimKeySource =
    System.getenv("PRESENCE_DIRECTORY_URL")?.takeIf(String::isNotBlank)?.let { url ->
        PresenceAliasClaimKeySource(
            httpClient = HttpClient(CIO) { install(ContentNegotiation) { json(serverJson) } },
            presenceDirectoryUrl = url
        )
    } ?: AliasClaimKeySource { null }
