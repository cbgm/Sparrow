package com.cbgm.securechat.server.security

import java.util.concurrent.ConcurrentHashMap

private const val DEFAULT_REPLAY_RETENTION_MILLISECONDS = 5L * 60L * 1_000L

class ReplayProtection(
    private val retentionMilliseconds: Long = DEFAULT_REPLAY_RETENTION_MILLISECONDS,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val seenNonces = ConcurrentHashMap<String, Long>()

    fun accept(
        scope: String,
        nonce: String,
        timestampEpochMilliseconds: Long
    ): Boolean {
        val currentTime = now()
        val timestampIsFresh =
            kotlin.math.abs(currentTime - timestampEpochMilliseconds) <=
                retentionMilliseconds
        if (nonce.isBlank() || !timestampIsFresh) {
            return false
        }

        seenNonces.entries.removeIf { (_, expiresAt) ->
            expiresAt <= currentTime
        }
        return seenNonces.putIfAbsent(
            "$scope:$nonce",
            currentTime + retentionMilliseconds
        ) == null
    }
}
