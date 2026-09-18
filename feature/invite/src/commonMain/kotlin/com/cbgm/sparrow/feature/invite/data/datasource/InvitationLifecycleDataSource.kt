package com.cbgm.sparrow.feature.invite.data.datasource

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.InvitationDao
import com.cbgm.sparrow.data.database.entity.InvitationEntity
import com.cbgm.sparrow.feature.invite.data.lifecycle.InvitationLifecycleEffects
import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleRecord
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleStatus
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResult
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResultAction
import com.cbgm.sparrow.feature.invite.domain.model.InvitationStatus
import com.cbgm.sparrow.feature.invite.domain.provider.InvitationPeerMetadata
import com.cbgm.sparrow.feature.invite.domain.provider.InvitationPeerMetadataProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
internal class InvitationLifecycleDataSource(
    private val invitationDao: InvitationDao,
    private val effects: InvitationLifecycleEffects,
    private val peerMetadataProvider: InvitationPeerMetadataProvider
) {
    val payloadType: InvitationPayloadType = effects.payloadType

    init {
        require(peerMetadataProvider.payloadType == payloadType) {
            "Invitation peer metadata provider does not match $payloadType"
        }
    }

    fun observeInvitations(direction: InvitationDirection): Flow<List<Invitation>> =
        invitationDao
            .observeByPayloadTypeAndDirection(
                payloadType = payloadType.name,
                direction = direction.name
            ).transformLatest { invitations ->
                while (true) {
                    val now = SystemClock.nowEpochMilliseconds()
                    emit(
                        buildList {
                            for (storedInvitation in invitations) {
                                if (storedInvitation.hiddenAtEpochMilliseconds != null) continue

                                val invitation = expirePendingIfNeeded(storedInvitation, now)
                                val status = invitation.toVisibleStatus() ?: continue
                                if (!isVisible(
                                        direction,
                                        status,
                                        invitation.updatedAtEpochMilliseconds,
                                        now
                                    )
                                ) {
                                    continue
                                }

                                val metadata =
                                    peerMetadataProvider
                                        .get(
                                            payloadId = invitation.payloadId,
                                            peerId = invitation.peerId,
                                            direction = direction
                                        ).getOrElse { InvitationPeerMetadata() }

                                add(
                                    Invitation(
                                        invitationId = invitation.invitationId,
                                        payloadType = payloadType,
                                        payloadId = invitation.payloadId,
                                        peerId = invitation.peerId,
                                        peerDisplayName = metadata.displayName,
                                        peerSecondaryText = metadata.secondaryText,
                                        direction = direction,
                                        status = status,
                                        expiresAtEpochMilliseconds = invitation.expiresAtEpochMilliseconds,
                                        updatedAtEpochMilliseconds = invitation.updatedAtEpochMilliseconds,
                                        hasUnreadUpdate =
                                            direction == InvitationDirection.INCOMING &&
                                                status == InvitationStatus.PENDING &&
                                                invitation.hasUnreadUpdate()
                                    )
                                )
                            }
                        }.sortedByDescending(Invitation::updatedAtEpochMilliseconds)
                    )

                    val nextWakeAt = nextWakeAt(invitations, now) ?: awaitCancellation()
                    delay((nextWakeAt - now).coerceAtLeast(1L).milliseconds)
                }
            }

    fun observeLifecycleStatus(
        payloadId: String,
        peerId: String,
        direction: InvitationDirection
    ): Flow<InvitationLifecycleStatus?> =
        invitationDao
            .observeLatest(
                payloadType = payloadType.name,
                payloadId = payloadId,
                peerId = peerId,
                direction = direction.name
            ).map { invitation ->
                invitation?.status?.toLifecycleStatus()
            }.distinctUntilChanged()

    fun observeInvitationResults(): Flow<List<InvitationResult>> =
        invitationDao
            .observeByPayloadType(payloadType.name)
            .map { invitations ->
                invitations.mapNotNull { invitation ->
                    val response =
                        when (invitation.status) {
                            STATUS_ACCEPTED -> InvitationResponse.ACCEPTED
                            STATUS_DECLINED -> InvitationResponse.DECLINED
                            else -> null
                        } ?: return@mapNotNull null
                    val direction = invitation.direction.toDirection() ?: return@mapNotNull null

                    InvitationResult(
                        invitationId = invitation.invitationId,
                        payloadType = payloadType,
                        payloadId = invitation.payloadId,
                        peerId = invitation.peerId,
                        direction = direction,
                        response = response,
                        action = invitation.resultAction.toResultAction()
                    )
                }
            }.distinctUntilChanged()

    suspend fun contains(invitationId: String): Boolean =
        invitationDao.findById(invitationId)?.payloadType == payloadType.name

    suspend fun shouldRecordPending(record: InvitationLifecycleRecord): Result<Boolean> =
        safeSuspendCall {
            require(record.payloadType == payloadType) {
                "Invitation payload type does not match $payloadType"
            }
            val existing = invitationDao.findById(record.invitationId)
            if (existing != null) {
                validateReplay(existing, record)
                return@safeSuspendCall existing.status == STATUS_PENDING
            }

            val latest =
                invitationDao.findLatest(
                    payloadType = payloadType.name,
                    payloadId = record.payloadId,
                    peerId = record.peerId,
                    direction = record.direction.name
                )
            latest == null || record.createdAtEpochMilliseconds > latest.createdAtEpochMilliseconds
        }

    suspend fun recordPending(record: InvitationLifecycleRecord): Result<Unit> =
        safeSuspendCall {
            require(record.payloadType == payloadType) {
                "Invitation payload type does not match $payloadType"
            }
            persistPending(record)
        }

    suspend fun validatePending(
        invitationId: String,
        payloadId: String,
        peerId: String,
        direction: InvitationDirection,
        atEpochMilliseconds: Long
    ): Result<Unit> =
        safeSuspendCall {
            val invitation = requireInvitation(invitationId)
            check(invitation.payloadId == payloadId) { "Invitation uses the wrong payload" }
            check(invitation.peerId == peerId) { "Invitation uses the wrong peer" }
            check(invitation.direction == direction.name) { "Invitation uses the wrong direction" }
            val current = expirePendingIfNeeded(invitation, atEpochMilliseconds)
            check(current.status == STATUS_PENDING) {
                "Invitation is not pending: ${current.status}"
            }
            check(atEpochMilliseconds <= current.expiresAtEpochMilliseconds) {
                "Invitation has expired"
            }
        }

    suspend fun getPeerId(invitationId: String): Result<String> =
        safeSuspendCall {
            requireInvitation(invitationId).peerId
        }

    suspend fun send(
        payloadId: String,
        peerIds: Set<String>
    ): Result<Unit> =
        safeSuspendCall {
            require(peerIds.isNotEmpty()) { "Choose at least one invitation peer" }
            peerIds.sorted().forEach { peerId ->
                val record = effects.send(payloadId, peerId).getOrThrow() ?: return@forEach
                require(record.payloadType == payloadType) {
                    "Lifecycle effects returned a ${record.payloadType} invitation for $payloadType"
                }
                check(record.direction == InvitationDirection.OUTGOING) {
                    "Outgoing invitation effects must return OUTGOING records"
                }
                require(record.payloadId == payloadId) {
                    "Lifecycle effects changed the invitation payload ID"
                }
                require(record.peerId == peerId) {
                    "Lifecycle effects returned an unexpected invitation peer"
                }
                persistPending(record)
            }
        }

    suspend fun accept(invitationId: String): Result<Unit> =
        safeSuspendCall {
            val invitation = requireInvitation(invitationId)
            check(invitation.direction == InvitationDirection.INCOMING.name) {
                "Only incoming invitations can be accepted"
            }
            val current = expirePendingIfNeeded(invitation, SystemClock.nowEpochMilliseconds())
            if (current.status == STATUS_ACCEPTED) return@safeSuspendCall
            check(current.status == STATUS_PENDING) {
                "Invitation cannot be accepted from status ${current.status}"
            }

            effects.accept(invitationId).getOrThrow()
            val now = SystemClock.nowEpochMilliseconds()
            val changed =
                invitationDao.updateStatus(
                    invitationId = invitationId,
                    expectedStatus = STATUS_PENDING,
                    newStatus = STATUS_ACCEPTED,
                    updatedAt = now
                )
            if (changed == 0) {
                check(requireInvitation(invitationId).status == STATUS_ACCEPTED) {
                    "Invitation status changed while accepting"
                }
            }
            invitationDao.hideById(invitationId, now)
        }

    suspend fun decline(
        invitationId: String,
        action: InvitationResultAction?
    ): Result<Unit> =
        safeSuspendCall {
            val invitation = requireInvitation(invitationId)
            check(invitation.direction == InvitationDirection.INCOMING.name) {
                "Only incoming invitations can be declined"
            }
            val current = expirePendingIfNeeded(invitation, SystemClock.nowEpochMilliseconds())
            if (current.status == STATUS_DECLINED) return@safeSuspendCall
            check(current.status == STATUS_PENDING) {
                "Invitation cannot be declined from status ${current.status}"
            }

            effects.decline(invitationId, action).getOrThrow()
            val now = SystemClock.nowEpochMilliseconds()
            val changed =
                invitationDao.updateStatus(
                    invitationId = invitationId,
                    expectedStatus = STATUS_PENDING,
                    newStatus = STATUS_DECLINED,
                    updatedAt = now,
                    resultAction = action?.name
                )
            if (changed == 0) {
                check(requireInvitation(invitationId).status == STATUS_DECLINED) {
                    "Invitation status changed while declining"
                }
            }
            invitationDao.hideById(invitationId, now)
        }

    suspend fun applyResponse(
        invitationId: String,
        response: InvitationResponse
    ): Result<Unit> =
        safeSuspendCall {
            val invitation = invitationDao.findById(invitationId) ?: return@safeSuspendCall
            check(invitation.payloadType == payloadType.name) {
                "Invitation payload type does not match $payloadType"
            }
            check(invitation.direction == InvitationDirection.OUTGOING.name) {
                "Only outgoing invitations can receive a remote response"
            }

            val newStatus =
                when (response) {
                    InvitationResponse.ACCEPTED -> STATUS_ACCEPTED
                    InvitationResponse.DECLINED -> STATUS_DECLINED
                }
            if (invitation.status == newStatus) return@safeSuspendCall
            check(invitation.status == STATUS_PENDING) {
                "Invitation cannot receive a response from status ${invitation.status}"
            }

            val now = SystemClock.nowEpochMilliseconds()
            val changed =
                invitationDao.updateStatus(
                    invitationId = invitationId,
                    expectedStatus = STATUS_PENDING,
                    newStatus = newStatus,
                    updatedAt = now
                )
            if (changed == 0) {
                check(requireInvitation(invitationId).status == newStatus) {
                    "Invitation status changed while applying response"
                }
            }
            if (response == InvitationResponse.ACCEPTED) {
                invitationDao.hideById(invitationId, now)
            }
        }

    suspend fun markTransportFailed(invitationId: String): Result<Unit> =
        safeSuspendCall {
            val invitation = invitationDao.findById(invitationId) ?: return@safeSuspendCall
            check(invitation.payloadType == payloadType.name) {
                "Invitation payload type does not match $payloadType"
            }
            if (invitation.direction != InvitationDirection.OUTGOING.name || invitation.status != STATUS_PENDING) {
                return@safeSuspendCall
            }

            val changed =
                invitationDao.updateStatus(
                    invitationId = invitationId,
                    expectedStatus = STATUS_PENDING,
                    newStatus = STATUS_FAILED,
                    updatedAt = SystemClock.nowEpochMilliseconds()
                )
            if (changed == 1) {
                effects.onTransportFailed(invitationId).getOrThrow()
            }
        }

    suspend fun markViewed(direction: InvitationDirection): Result<Unit> =
        safeSuspendCall {
            invitationDao.markDirectionViewed(
                payloadType = payloadType.name,
                direction = direction.name,
                viewedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            )
        }

    suspend fun deleteDeclinedOutgoing(invitationId: String): Result<Unit> =
        safeSuspendCall {
            val invitation = requireInvitation(invitationId)
            check(invitation.direction == InvitationDirection.OUTGOING.name) {
                "Only outgoing invitations can be deleted"
            }
            check(invitation.status == STATUS_DECLINED) {
                "Only declined outgoing invitations can be deleted"
            }
            effects.onDeleteDeclinedOutgoing(invitationId).getOrThrow()
            check(
                invitationDao.hideById(
                    invitationId = invitationId,
                    hiddenAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                ) == 1
            ) {
                "Invitation was not found"
            }
        }

    private fun InvitationEntity.hasUnreadUpdate(): Boolean {
        val viewedAt = viewedAtEpochMilliseconds
        return viewedAt == null || updatedAtEpochMilliseconds > viewedAt
    }

    private suspend fun persistPending(record: InvitationLifecycleRecord) {
        val existing = invitationDao.findById(record.invitationId)
        if (existing != null) {
            validateReplay(existing, record)
            if (existing.status != STATUS_PENDING) return

            if (record.updatedAtEpochMilliseconds > existing.updatedAtEpochMilliseconds) {
                invitationDao.upsert(
                    existing.copy(updatedAtEpochMilliseconds = record.updatedAtEpochMilliseconds)
                )
            }
            return
        }

        val latest =
            invitationDao.findLatest(
                payloadType = payloadType.name,
                payloadId = record.payloadId,
                peerId = record.peerId,
                direction = record.direction.name
            )
        if (
            latest != null &&
            record.createdAtEpochMilliseconds <= latest.createdAtEpochMilliseconds
        ) {
            return
        }

        invitationDao.failSuperseded(
            payloadType = payloadType.name,
            payloadId = record.payloadId,
            peerId = record.peerId,
            currentInvitationId = record.invitationId,
            direction = record.direction.name,
            pendingStatus = STATUS_PENDING,
            failedStatus = STATUS_FAILED,
            updatedAt = record.updatedAtEpochMilliseconds
        )
        invitationDao.upsert(
            InvitationEntity(
                invitationId = record.invitationId,
                payloadType = payloadType.name,
                payloadId = record.payloadId,
                peerId = record.peerId,
                direction = record.direction.name,
                status = STATUS_PENDING,
                createdAtEpochMilliseconds = record.createdAtEpochMilliseconds,
                expiresAtEpochMilliseconds = record.expiresAtEpochMilliseconds,
                updatedAtEpochMilliseconds = record.updatedAtEpochMilliseconds
            )
        )
    }

    private fun validateReplay(
        existing: InvitationEntity,
        record: InvitationLifecycleRecord
    ) {
        check(existing.payloadType == payloadType.name) {
            "Invitation replay changed its payload type"
        }
        check(existing.payloadId == record.payloadId) {
            "Invitation replay changed its payload ID"
        }
        check(existing.peerId == record.peerId) {
            "Invitation replay changed its peer"
        }
        check(existing.direction == record.direction.name) {
            "Invitation replay changed its direction"
        }
        check(existing.createdAtEpochMilliseconds == record.createdAtEpochMilliseconds) {
            "Invitation replay changed its creation time"
        }
        check(existing.expiresAtEpochMilliseconds == record.expiresAtEpochMilliseconds) {
            "Invitation replay changed its expiration time"
        }
    }

    private suspend fun requireInvitation(invitationId: String): InvitationEntity {
        require(invitationId.isNotBlank()) { "Invitation ID must not be blank" }
        val invitation = invitationDao.findById(invitationId)
            ?: error("Invitation was not found: $invitationId")
        check(invitation.payloadType == payloadType.name) {
            "Invitation payload type does not match $payloadType"
        }
        return invitation
    }

    private suspend fun expirePendingIfNeeded(
        invitation: InvitationEntity,
        now: Long
    ): InvitationEntity {
        if (invitation.status != STATUS_PENDING || invitation.expiresAtEpochMilliseconds > now) {
            return invitation
        }

        val changed =
            invitationDao.updateStatus(
                invitationId = invitation.invitationId,
                expectedStatus = STATUS_PENDING,
                newStatus = STATUS_EXPIRED,
                updatedAt = now
            )
        if (changed == 1) {
            effects.onExpired(invitation.invitationId).getOrThrow()
        }
        return invitation.copy(
            status = STATUS_EXPIRED,
            updatedAtEpochMilliseconds = maxOf(invitation.createdAtEpochMilliseconds, now)
        )
    }

    private fun InvitationEntity.toVisibleStatus(): InvitationStatus? =
        when (status) {
            STATUS_PENDING -> InvitationStatus.PENDING
            STATUS_DECLINED -> InvitationStatus.DECLINED
            STATUS_EXPIRED -> InvitationStatus.EXPIRED
            STATUS_FAILED -> InvitationStatus.FAILED
            else -> null
        }

    private fun isVisible(
        direction: InvitationDirection,
        status: InvitationStatus,
        updatedAtEpochMilliseconds: Long,
        now: Long
    ): Boolean =
        when (direction) {
            InvitationDirection.INCOMING -> status == InvitationStatus.PENDING
            InvitationDirection.OUTGOING ->
                status == InvitationStatus.PENDING ||
                    (
                        status == InvitationStatus.DECLINED &&
                            now - updatedAtEpochMilliseconds < DECLINED_RETENTION_MILLISECONDS
                    )
        }

    private fun nextWakeAt(
        invitations: List<InvitationEntity>,
        now: Long
    ): Long? =
        invitations
            .asSequence()
            .filter { invitation -> invitation.hiddenAtEpochMilliseconds == null }
            .mapNotNull { invitation ->
                val wakeAt =
                    when (invitation.status) {
                        STATUS_PENDING -> invitation.expiresAtEpochMilliseconds
                        STATUS_DECLINED ->
                            invitation.updatedAtEpochMilliseconds + DECLINED_RETENTION_MILLISECONDS

                        else -> null
                    }
                wakeAt?.takeIf { it > now }
            }.minOrNull()

    private fun String.toLifecycleStatus(): InvitationLifecycleStatus? =
        when (this) {
            STATUS_PENDING -> InvitationLifecycleStatus.PENDING
            STATUS_ACCEPTED -> InvitationLifecycleStatus.ACCEPTED
            STATUS_DECLINED -> InvitationLifecycleStatus.DECLINED
            STATUS_EXPIRED -> InvitationLifecycleStatus.EXPIRED
            STATUS_FAILED -> InvitationLifecycleStatus.FAILED
            else -> null
        }

    private fun String.toDirection(): InvitationDirection? =
        InvitationDirection.entries.firstOrNull { direction -> direction.name == this }

    private fun String?.toResultAction(): InvitationResultAction? =
        this?.let { stored ->
            InvitationResultAction.entries.firstOrNull { action -> action.name == stored }
        }

    private companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_ACCEPTED = "ACCEPTED"
        const val STATUS_DECLINED = "DECLINED"
        const val STATUS_EXPIRED = "EXPIRED"
        const val STATUS_FAILED = "FAILED"
        const val DECLINED_RETENTION_MILLISECONDS = 24L * 60L * 60L * 1_000L
    }
}
