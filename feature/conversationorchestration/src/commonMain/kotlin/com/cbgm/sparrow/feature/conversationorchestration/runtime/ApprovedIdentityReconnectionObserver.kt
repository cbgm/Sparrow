package com.cbgm.sparrow.feature.conversationorchestration.runtime

import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.StartRecoveryInvitationUseCase
import com.cbgm.sparrow.feature.identity.domain.model.ApprovedIdentityReconnection
import com.cbgm.sparrow.feature.identity.domain.usecase.AcknowledgeQueuedRecoveryInvitationUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetApprovedIdentityReconnectionUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityPeerStateUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveApprovedIdentityReconnectionsUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObservePendingRemoteIdentityChangesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * App-lifetime resumption of the EXISTING, explicitly approved recovery workflow.
 * This class only manages one worker per approved peer; it does not implement a
 * second invitation protocol or delete conversations.
 */
class ApprovedIdentityReconnectionObserver(
    private val observeApprovals: ObserveApprovedIdentityReconnectionsUseCase,
    private val getApproval: GetApprovedIdentityReconnectionUseCase,
    private val acknowledgeQueued: AcknowledgeQueuedRecoveryInvitationUseCase,
    private val startInvitation: StartRecoveryInvitationUseCase,
    private val observePendingKeyChanges: ObservePendingRemoteIdentityChangesUseCase,
    private val getIdentityPeerState: GetIdentityPeerStateUseCase
) {
    suspend fun run(): Unit = coroutineScope {
        val workers = mutableMapOf<String, Pair<String, Job>>()
        try {
            observeApprovals().retryWhen { cause, attempt ->
                if (cause is CancellationException) throw cause
                // A temporary Room observation error must not permanently disable
                // automatic recovery for the remainder of the app process.
                delay((1_000L shl attempt.coerceAtMost(6L).toInt()).coerceAtMost(60_000L).milliseconds)
                true
            }.collect { approvals ->
                val latest = approvals.associateBy(ApprovedIdentityReconnection::peerId)
                val obsolete = workers.filter { (peer, active) ->
                    latest[peer]?.approvalId != active.first
                }.keys.toList()
                obsolete.forEach { peer -> workers.remove(peer)?.second?.cancelAndJoin() }
                approvals.forEach { approval ->
                    if (workers[approval.peerId] == null) {
                        workers[approval.peerId] = approval.approvalId to launch {
                            ApprovedReconnectionRetryWorker(
                                findApproval = { getApproval(it).getOrThrow() },
                                pendingUnverifiedReplacement = { peer ->
                                    observePendingKeyChanges().first().any { it.peerId == peer }
                                },
                                hasFreshAuthorization = { peer ->
                                    getIdentityPeerState(peer).getOrThrow().hasEstablishedExchange
                                },
                                enqueueInvitation = { approved -> startInvitation(approved).getOrThrow() },
                                acknowledge = { peer, id -> acknowledgeQueued(peer, id).getOrThrow() }
                            ).run(approval)
                        }
                    }
                }
            }
        } finally {
            workers.values.forEach { it.second.cancel() }
        }
    }
}
