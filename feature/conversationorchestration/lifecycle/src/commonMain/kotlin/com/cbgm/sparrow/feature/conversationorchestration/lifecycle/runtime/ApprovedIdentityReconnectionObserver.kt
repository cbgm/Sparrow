package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.runtime

import com.cbgm.sparrow.core.util.flow.retryWithBackoff
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command.LifecycleCommand
import com.cbgm.sparrow.feature.identity.domain.model.ApprovedIdentityReconnection
import com.cbgm.sparrow.feature.identity.domain.usecase.AcknowledgeQueuedRecoveryInvitationUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetApprovedIdentityReconnectionUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityPeerStateUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveApprovedIdentityReconnectionsUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ObservePendingRemoteIdentityChangesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * App-lifetime resumption of the EXISTING, explicitly approved recovery workflow.
 * This class only manages one worker per approved peer; it does not implement a
 * second invitation protocol or delete conversations.
 */
class ApprovedIdentityReconnectionObserver internal constructor(
    private val observeApprovals: ObserveApprovedIdentityReconnectionsUseCase,
    private val getApproval: GetApprovedIdentityReconnectionUseCase,
    private val acknowledgeQueued: AcknowledgeQueuedRecoveryInvitationUseCase,
    private val engine: WorkflowEngine,
    private val observePendingKeyChanges: ObservePendingRemoteIdentityChangesUseCase,
    private val getIdentityPeerState: GetIdentityPeerStateUseCase
) {
    suspend fun run(): Unit = coroutineScope {
        val workers = mutableMapOf<String, Pair<String, Job>>()
        try {
            // A temporary Room observation error must not permanently disable
            // automatic recovery for the remainder of the app process.
            observeApprovals().retryWithBackoff().collect { approvals ->
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
                                enqueueInvitation = { approved ->
                                    engine.run(LifecycleCommand.StartRecoveryInvitation(approved)).getOrThrow()
                                },
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
