package com.cbgm.sparrow.server.presence

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CachingPhoneTokenVerifierTest {
    private var now = 0L
    private var calls = 0

    @Test
    fun verifiedTokenIsReusedUntilItExpires() =
        runTest {
            val verifier = cachingVerifier(expiresAt = 100L)

            verifier.verify("token")
            now = 99L
            verifier.verify("token")
            assertEquals(1, calls)

            now = 100L
            verifier.verify("token")
            assertEquals(2, calls)
        }

    @Test
    fun failedVerificationIsNotCached() =
        runTest {
            val verifier =
                CachingPhoneTokenVerifier(
                    delegate = {
                        calls++
                        error("invalid")
                    },
                    currentTime = { now }
                )

            repeat(2) { assertFailsWith<IllegalStateException> { verifier.verify("token") } }
            assertEquals(2, calls)
        }

    @Test
    fun fullCacheStillVerifiesNewTokens() =
        runTest {
            val verifier = cachingVerifier(expiresAt = 100L, maximumEntries = 1)

            verifier.verify("first")
            verifier.verify("second")
            verifier.verify("second")
            assertEquals(3, calls)
        }

    private fun cachingVerifier(expiresAt: Long, maximumEntries: Int = 10) =
        CachingPhoneTokenVerifier(
            delegate = {
                calls++
                VerifiedPhone("+491701234567", expiresAt)
            },
            maximumEntries = maximumEntries,
            currentTime = { now }
        )
}
