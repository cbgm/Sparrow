package com.cbgm.sparrow.server.presence

import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

internal class CachingPhoneTokenVerifier(
    private val delegate: PhoneTokenVerifier,
    private val maximumEntries: Int = DEFAULT_MAXIMUM_ENTRIES,
    private val currentTime: () -> Long = System::currentTimeMillis
) : PhoneTokenVerifier {
    private val verified = ConcurrentHashMap<String, VerifiedPhone>()

    override suspend fun verify(token: String): VerifiedPhone {
        val key = token.sha256()
        verified[key]?.takeIf { it.expiresAtEpochMilliseconds > currentTime() }?.let { return it }
        return delegate.verify(token).also { phone ->
            if (verified.size >= maximumEntries) evictExpired()
            if (verified.size < maximumEntries) verified[key] = phone
        }
    }

    private fun evictExpired() {
        val now = currentTime()
        verified.entries.removeIf { it.value.expiresAtEpochMilliseconds <= now }
    }

    private fun String.sha256(): String =
        Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
            )

    private companion object {
        const val DEFAULT_MAXIMUM_ENTRIES = 50_000
    }
}
