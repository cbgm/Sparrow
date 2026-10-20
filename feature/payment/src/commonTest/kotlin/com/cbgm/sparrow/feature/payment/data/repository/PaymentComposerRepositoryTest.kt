package com.cbgm.sparrow.feature.payment.data.repository

import com.cbgm.sparrow.core.base.messagepart.domain.model.PaymentTransfer
import com.cbgm.sparrow.core.base.messagepart.domain.model.PaymentTransferStatus
import com.cbgm.sparrow.feature.payment.domain.model.PaymentContext
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PaymentComposerRepositoryTest {
    private val event = PaymentTransfer(
        "payment-sent",
        "payment",
        "board",
        "payer",
        "recipient",
        5000L,
        "EUR",
        "SPARROW-test",
        PaymentTransferStatus.SENT,
        1L
    )
    private val context = PaymentContext("board", "EUR", "payer", emptyList(), emptyList(), emptyList())

    @Test
    fun recordWaitsForTheHostAndPropagatesItsFailure() = runTest {
        val repository = PaymentComposerRepositoryImpl()
        repository.publishContext("group", context).getOrThrow()
        var result: Result<Unit>? = null
        val job = launch(start = CoroutineStart.UNDISPATCHED) { result = repository.record("group", event) }
        val request = repository.observeRequests("group").first()
        assertEquals(null, result)
        val failure = IllegalStateException("send failed")
        repository.complete(request, Result.failure(failure))
        job.join()
        assertSame(failure, result?.exceptionOrNull())
    }

    @Test
    fun requestsStayWithTheirGroupAndCompletionCannotOverwriteSuccess() = runTest {
        val repository = PaymentComposerRepositoryImpl()
        repository.publishContext("a", context).getOrThrow()
        repository.publishContext("b", context).getOrThrow()
        var resultA: Result<Unit>? = null
        var resultB: Result<Unit>? = null
        val jobA = launch(start = CoroutineStart.UNDISPATCHED) { resultA = repository.record("a", event) }
        val jobB = launch(start = CoroutineStart.UNDISPATCHED) { resultB = repository.record("b", event) }
        val requestB = repository.observeRequests("b").first()
        assertEquals("b", requestB.groupId)
        repository.complete(requestB, Result.success(Unit))
        repository.complete(requestB, Result.failure(IllegalStateException("late cleanup")))
        jobB.join()
        assertTrue(resultB?.isSuccess == true)
        assertEquals(null, resultA)
        repository.complete(repository.observeRequests("a").first(), Result.success(Unit))
        jobA.join()
        assertTrue(resultA?.isSuccess == true)
    }

    @Test
    fun clearingAConversationReleasesItsWaitingCallerAndInvalidatesContext() = runTest {
        val repository = PaymentComposerRepositoryImpl()
        repository.publishContext("group", context).getOrThrow()
        var result: Result<Unit>? = null
        val job = launch(start = CoroutineStart.UNDISPATCHED) { result = repository.record("group", event) }
        repository.clearContext("group").getOrThrow()
        job.join()
        assertTrue(result?.isFailure == true)
        assertEquals(null, repository.observeContext("group").first())
        assertTrue(repository.record("group", event).isFailure)
    }

    @Test
    fun cancelledRequestsAreSkippedWhenTheHostNextCollects() = runTest {
        val repository = PaymentComposerRepositoryImpl()
        repository.publishContext("group", context).getOrThrow()
        val cancelled = launch(start = CoroutineStart.UNDISPATCHED) { repository.record("group", event) }
        cancelled.cancel()
        cancelled.join()
        val freshEvent = event.copy(id = "fresh-sent", transferId = "fresh")
        var freshResult: Result<Unit>? = null
        val fresh = launch(start = CoroutineStart.UNDISPATCHED) { freshResult = repository.record("group", freshEvent) }
        val request = repository.observeRequests("group").first()
        assertEquals("fresh", request.event.transferId)
        repository.complete(request, Result.success(Unit))
        fresh.join()
        assertTrue(freshResult?.isSuccess == true)
    }
}
