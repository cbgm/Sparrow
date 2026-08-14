package com.cbgm.securechat.server.persistence

import java.util.concurrent.ConcurrentHashMap

class BoundedIdempotencyStore(
    private val maximumEntries: Int,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val entries = ConcurrentHashMap<String, Long>()

    fun record(
        id: String,
        expiresAtEpochMilliseconds: Long
    ): Boolean {
        purgeExpired()
        if (entries.size >= maximumEntries && !entries.containsKey(id)) {
            return false
        }

        return entries.putIfAbsent(id, expiresAtEpochMilliseconds) == null
    }

    fun contains(id: String): Boolean {
        purgeExpired()
        return entries.containsKey(id)
    }

    private fun purgeExpired() {
        val currentTime = now()
        entries.entries.removeIf { (_, expiresAt) -> expiresAt <= currentTime }
    }
}
