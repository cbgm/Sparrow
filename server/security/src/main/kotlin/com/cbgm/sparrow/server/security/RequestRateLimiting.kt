package com.cbgm.sparrow.server.security

import com.cbgm.sparrow.server.protocol.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import io.ktor.server.response.respond
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class RateLimitPolicy(
    val maximumRequests: Int,
    val windowMilliseconds: Long,
    val maximumTrackedClients: Int = DEFAULT_MAXIMUM_TRACKED_CLIENTS
) {
    init {
        require(maximumRequests > 0) { "Rate-limit request count must be positive" }
        require(windowMilliseconds > 0L) { "Rate-limit window must be positive" }
        require(maximumTrackedClients > 0) { "Tracked-client limit must be positive" }
    }

    private companion object {
        const val DEFAULT_MAXIMUM_TRACKED_CLIENTS = 100_000
    }
}

sealed interface RateLimitDecision {
    data object Allowed : RateLimitDecision

    data class Rejected(
        val retryAfterSeconds: Long
    ) : RateLimitDecision
}

class BoundedRateLimiter(
    private val policy: RateLimitPolicy,
    private val now: () -> Long = System::currentTimeMillis
) {
    private data class Window(
        val startedAtEpochMilliseconds: Long,
        var requests: Int
    )

    private val lock = Any()
    private val windows =
        LinkedHashMap<String, Window>(INITIAL_CAPACITY, LOAD_FACTOR, true)

    fun acquire(key: String): RateLimitDecision =
        synchronized(lock) {
            require(key.isNotBlank()) { "Rate-limit key must not be blank" }
            val currentTime = now()
            val current = windows[key]
            val active =
                if (
                    current == null ||
                    currentTime - current.startedAtEpochMilliseconds >= policy.windowMilliseconds
                ) {
                    Window(currentTime, requests = 0).also { replacement ->
                        windows[key] = replacement
                        evictOverflow()
                    }
                } else {
                    current
                }

            if (active.requests < policy.maximumRequests) {
                active.requests += 1
                RateLimitDecision.Allowed
            } else {
                val remainingMilliseconds =
                    policy.windowMilliseconds -
                        (currentTime - active.startedAtEpochMilliseconds)
                RateLimitDecision.Rejected(
                    retryAfterSeconds =
                        ((remainingMilliseconds.coerceAtLeast(1L) + 999L) / 1_000L)
                            .coerceAtLeast(1L)
                )
            }
        }

    internal fun trackedClientCount(): Int = synchronized(lock) { windows.size }

    private fun evictOverflow() {
        while (windows.size > policy.maximumTrackedClients) {
            val eldestKey = windows.entries.iterator().next().key
            windows.remove(eldestKey)
        }
    }

    private companion object {
        const val INITIAL_CAPACITY = 16
        const val LOAD_FACTOR = 0.75f
    }
}

fun ApplicationCall.hashedClientAddress(): String =
    ClientRateLimitKeys.hash(request.origin.remoteAddress)

fun ApplicationCall.clientRateLimitKey(): String =
    ClientRateLimitKeys.hash(request.origin.remoteAddress)

suspend fun ApplicationCall.enforceRateLimit(
    limiter: BoundedRateLimiter
): Boolean =
    enforceRateLimitP(
        limiter = limiter,
        key = clientRateLimitKey()
    )

suspend fun ApplicationCall.enforceRateLimit(
    limiter: BoundedRateLimiter,
    scope: String
): Boolean {
    require(scope.isNotBlank()) {
        "Rate-limit scope must not be blank"
    }

    return enforceRateLimitP(
        limiter = limiter,
        key = "${clientRateLimitKey()}:$scope"
    )
}

private suspend fun ApplicationCall.enforceRateLimitP(
    limiter: BoundedRateLimiter,
    key: String
): Boolean =
    when (val decision = limiter.acquire(key)) {
        RateLimitDecision.Allowed -> true
        is RateLimitDecision.Rejected -> {
            respondTooManyRequests(decision.retryAfterSeconds)
            false
        }
    }

suspend fun ApplicationCall.respondTooManyRequests(
    retryAfterSeconds: Long,
    code: String = "RATE_LIMITED",
    message: String = "Request limit exceeded"
) {
    response.headers.append(RETRY_AFTER_HEADER, retryAfterSeconds.coerceAtLeast(1L).toString())
    respond(HttpStatusCode.TooManyRequests, ErrorResponse(code, message))
}

/**
 * Client addresses are never kept in a form that can be reversed. An unsalted hash of an IPv4 address is undone by
 * trying all 2^32 addresses, so the key is a random HMAC key that exists only in memory and is replaced every day:
 * once it is gone, nothing stored or logged leads back to an address. Limits keyed by it restart with the key.
 */
private object ClientRateLimitKeys {
    private val random = SecureRandom()

    @Volatile
    private var current = DailyKey(day = currentDay(), key = newKey())

    fun hash(value: String): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(keyForToday(), HMAC_ALGORITHM))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.encodeToByteArray()))
    }

    private fun keyForToday(): ByteArray {
        val today = currentDay()
        current.takeIf { it.day == today }?.let { return it.key }
        return synchronized(this) {
            current.takeIf { it.day == today } ?: DailyKey(today, newKey()).also { current = it }
        }.key
    }

    private fun newKey(): ByteArray = ByteArray(KEY_BYTES).also(random::nextBytes)

    private fun currentDay(): Long = System.currentTimeMillis() / MILLISECONDS_PER_DAY

    private class DailyKey(
        val day: Long,
        val key: ByteArray
    )

    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val KEY_BYTES = 32
    private const val MILLISECONDS_PER_DAY = 24L * 60L * 60L * 1_000L
}

private const val RETRY_AFTER_HEADER = "Retry-After"
