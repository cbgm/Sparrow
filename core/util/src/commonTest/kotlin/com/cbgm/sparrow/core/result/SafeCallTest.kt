package com.cbgm.sparrow.core.result

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SafeCallTest {
    private class ExpectedReview : IllegalStateException("Review remote identity")

    @Test
    fun expectedFailureIsReturnedForCallerToHandle() = runBlocking {
        val review = ExpectedReview()
        val result = safeSuspendCall(expectedFailure = { it is ExpectedReview }) {
            throw review
        }
        assertSame(review, result.exceptionOrNull())
    }

    @Test
    fun expectedFailurePredicateDoesNotConvertUnexpectedFailuresIntoSuccess() = runBlocking {
        val unexpected = IllegalStateException("Storage unavailable")
        val result = safeSuspendCall(expectedFailure = { it is ExpectedReview }) {
            throw unexpected
        }
        assertSame(unexpected, result.exceptionOrNull())
        assertTrue(result.isFailure)
    }

    @Test
    fun cancellationStillPropagates() {
        runBlocking {
            assertFailsWith<CancellationException> {
                safeSuspendCall(expectedFailure = { it is ExpectedReview }) {
                    throw CancellationException("cancelled")
                }
            }
        }
    }
}
