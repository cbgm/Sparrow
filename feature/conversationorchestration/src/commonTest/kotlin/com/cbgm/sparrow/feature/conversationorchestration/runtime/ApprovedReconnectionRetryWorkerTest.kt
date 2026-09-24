package com.cbgm.sparrow.feature.conversationorchestration.runtime

import com.cbgm.sparrow.feature.identity.domain.model.ApprovedIdentityReconnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ApprovedReconnectionRetryWorkerTest {
    private val approval = ApprovedIdentityReconnection("peer", "approved-invitation")

    @Test
    fun transientFailureRetriesWithoutAnotherRoomEmissionOrUserAction() = runBlocking {
        var attempts = 0
        var acknowledged = 0
        val waits = mutableListOf<Long>()
        worker(
            enqueue = {
                if (++attempts == 1) error("offline")
            },
            acknowledge = { acknowledged++ },
            wait = { waits += it }
        ).run(approval)
        assertEquals(2, attempts)
        assertEquals(1, acknowledged)
        assertEquals(listOf(1_000L), waits)
    }

    @Test
    fun failedApprovalLookupNeverCountsAsConsent() = runBlocking {
        var reads = 0
        var attempts = 0
        val waits = mutableListOf<Long>()
        worker(
            find = { if (++reads == 1) error("database unavailable") else approval },
            enqueue = { attempts++ },
            wait = { waits += it }
        ).run(approval)
        assertEquals(3, reads)
        assertEquals(1, attempts)
        assertEquals(listOf(1_000L), waits)
    }

    @Test
    fun newerApprovalCancelsTheOldIntentBeforeAnyRetry() = runBlocking {
        var current = approval
        var attempts = 0
        worker(
            find = { current },
            enqueue = {
                attempts++
                error("temporarily unavailable")
            },
            wait = { current = approval.copy(approvalId = "new-approval") }
        ).run(approval)
        assertEquals(1, attempts)
    }

    @Test
    fun unverifiedReplacementPausesTheOldApproval() = runBlocking {
        var pending = true
        var attempts = 0
        worker(
            pending = { pending },
            enqueue = { attempts++ },
            wait = { pending = false }
        ).run(approval)
        assertEquals(1, attempts)
    }

    @Test
    fun establishedFreshExchangeDoesNotTriggerAnotherInvitation() = runBlocking {
        var attempts = 0
        var acknowledgements = 0
        worker(
            authorized = { true },
            enqueue = { attempts++ },
            acknowledge = { acknowledgements++ }
        ).run(approval)
        assertEquals(0, attempts)
        assertEquals(1, acknowledgements)
    }

    @Test
    fun transientAcknowledgementFailureMustNotRequeueTheInvitation() = runBlocking {
        var attempts = 0
        var acknowledgements = 0
        worker(
            enqueue = { attempts++ },
            acknowledge = {
                if (++acknowledgements == 1) error("DB temporarily unavailable")
            }
        ).run(approval)
        assertEquals(1, attempts)
        assertEquals(2, acknowledgements)
    }

    @Test
    fun cancellationIsNeverRetried() = runBlocking {
        var attempts = 0
        assertFailsWith<CancellationException> {
            worker(enqueue = {
                attempts++
                throw CancellationException("worker stopped")
            })
                .run(approval)
        }
        assertEquals(1, attempts)
    }

    @Test
    fun repeatedFailuresCapTheRetryDelay() = runBlocking {
        var attempts = 0
        val waits = mutableListOf<Long>()
        worker(
            enqueue = { if (++attempts <= 8) error("offline") },
            wait = { waits += it }
        ).run(approval)
        assertEquals(9, attempts)
        assertEquals(
            listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 32_000L, 60_000L, 60_000L),
            waits
        )
    }

    private fun worker(
        find: suspend () -> ApprovedIdentityReconnection? = { approval },
        pending: suspend () -> Boolean = { false },
        authorized: suspend () -> Boolean = { false },
        enqueue: suspend () -> Unit = {},
        acknowledge: suspend () -> Unit = {},
        wait: suspend (Long) -> Unit = {}
    ): ApprovedReconnectionRetryWorker = ApprovedReconnectionRetryWorker(
        findApproval = { find() },
        pendingUnverifiedReplacement = { pending() },
        hasFreshAuthorization = { authorized() },
        enqueueInvitation = { _ -> enqueue() },
        acknowledge = { _, _ -> acknowledge() },
        waitBeforeRetry = wait
    )
}
