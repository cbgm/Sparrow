package com.cbgm.sparrow.feature.conversationorchestration.runtime

import com.cbgm.sparrow.feature.identity.domain.model.ApprovedIdentityReconnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

/**
 * Retry ONLY a key replacement explicitly approved from the Mailbox. An ordinary offline
 * state is not approval; a second unverified replacement pauses the old attempt.
 * Keeping the approved row until the invitation is enqueued allows restart retry.
 */
internal class ApprovedReconnectionRetryWorker(
    private val findApproval: suspend (String) -> ApprovedIdentityReconnection?,
    private val pendingUnverifiedReplacement: suspend (String) -> Boolean,
    private val hasFreshAuthorization: suspend (String) -> Boolean,
    private val enqueueInvitation: suspend (ApprovedIdentityReconnection) -> Unit,
    private val acknowledge: suspend (String, String) -> Unit,
    private val waitBeforeRetry: suspend (Long) -> Unit = { delay(it.milliseconds) }
) {
    suspend fun run(approval: ApprovedIdentityReconnection) {
        var queuedInThisProcess = false
        var backoffMillis = 1_000L
        while (currentCoroutineContext().isActive) {
            try {
                // Fail CLOSED on a DB error. A superseded/removed approval ends the worker.
                val persisted = findApproval(approval.peerId)
                if (!approval.sameApproval(persisted)) return
                if (pendingUnverifiedReplacement(approval.peerId)) {
                    waitBeforeRetry(backoffMillis)
                } else if (queuedInThisProcess || hasFreshAuthorization(approval.peerId)) {
                    // A peer can complete the existing handshake before we start a new
                    // invitation. Do not revoke a newly established exchange in that case.
                    acknowledge(approval.peerId, approval.approvalId)
                    return
                } else {
                    enqueueInvitation(requireNotNull(persisted))
                    // If clearing the approval fails, retry ONLY that acknowledgement;
                    // never enqueue another invitation in this same process.
                    queuedInThisProcess = true
                    backoffMillis = 1_000L
                    continue
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                waitBeforeRetry(backoffMillis)
            }
            backoffMillis = (backoffMillis * 2L).coerceAtMost(60_000L)
        }
    }
}
