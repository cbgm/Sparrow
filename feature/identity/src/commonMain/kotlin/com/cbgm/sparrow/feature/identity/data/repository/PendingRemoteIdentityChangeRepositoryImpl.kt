package com.cbgm.sparrow.feature.identity.data.repository

import com.cbgm.sparrow.core.extensions.toFingerprint
import com.cbgm.sparrow.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.entity.PendingRemoteIdentityChangeEntity
import com.cbgm.sparrow.feature.identity.data.datasource.PendingRemoteIdentityChangeDataSource
import com.cbgm.sparrow.feature.identity.domain.model.PendingRemoteIdentityChange
import com.cbgm.sparrow.feature.identity.domain.repository.PendingRemoteIdentityChangeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class PendingRemoteIdentityChangeRepositoryImpl(
    private val source: PendingRemoteIdentityChangeDataSource,
    private val mailboxCapabilityLifecycle: MailboxCapabilityLifecycle
) : PendingRemoteIdentityChangeRepository {
    override suspend fun stage(candidate: PendingRemoteIdentityChange): Result<Unit> = safeSuspendCall {
        require(candidate.peerId.isNotBlank() && candidate.sourcePeerId.isNotBlank())
        require(candidate.invitationId.isNotBlank())
        require(candidate.proposedEncryptionPublicKey.size == 32 && candidate.proposedSigningPublicKey.size == 32)
        require(candidate.expiresAtEpochMilliseconds > candidate.receivedAtEpochMilliseconds)
        check(
            candidate.originalInviteChallenge == null || (
                candidate.originalInviteChallenge.size == 32 &&
                    candidate.originalInviteCreatedAtEpochMilliseconds != null &&
                    candidate.originalInviteCreatedAtEpochMilliseconds < candidate.expiresAtEpochMilliseconds
            )
        ) { "Invalid staged invitation challenge" }
        // Replays must not push an older candidate over a more recent signed invitation.
        val current = source.find(candidate.peerId)
        // Idempotent retransmission MUST NOT erase a user's prior fingerprint confirmation.
        val exactReplay = current != null &&
            current.invitationId == candidate.invitationId &&
            current.proposedEncryptionPublicKey.contentEquals(candidate.proposedEncryptionPublicKey) &&
            current.proposedSigningPublicKey.contentEquals(candidate.proposedSigningPublicKey)
        if (exactReplay) return@safeSuspendCall
        if (current == null || candidate.receivedAtEpochMilliseconds > current.receivedAtEpochMilliseconds) {
            // A newer candidate intentionally resets confirmation. Old confirmation is never transferable.
            source.upsert(candidate.toEntity())
        }
    }

    override fun observeAll(): Flow<List<PendingRemoteIdentityChange>> =
        source.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun approveReplacement(
        peerId: String,
        invitationId: String,
        presentedSigningFingerprint: String,
        presentedEncryptionFingerprint: String
    ): Result<Unit> = safeSuspendCall {
        require(peerId.isNotBlank() && invitationId.isNotBlank())
        // Bind the user gesture to BOTH key fingerprints rendered in Mailbox.
        // A superseding invitation cannot inherit a previous approval.
        val candidate = source.find(peerId) ?: error("Identity change request no longer exists")
        check(candidate.invitationId == invitationId) { "A newer identity change request exists" }
        check(
            candidate.proposedSigningPublicKey.toFingerprint() == presentedSigningFingerprint &&
                candidate.proposedEncryptionPublicKey.toFingerprint() == presentedEncryptionFingerprint
        ) { "The proposed keys changed. Review the new request before approving" }
        val now = SystemClock.nowEpochMilliseconds()
        check(candidate.expiresAtEpochMilliseconds > now) {
            "Identity change request expired; ask your contact to resend an invitation"
        }
        // The existing DB column 'fingerprintConfirmedAt...' is now an explicit
        // acknowledgement of the SHOWN proposal, not independent verification.
        // Atomic DB compare-and-set pins the old keys AND exact proposed keys.
        if (candidate.fingerprintConfirmedAtEpochMilliseconds == null) {
            check(
                source.confirmFingerprintIfCurrent(
                    peerId,
                    invitationId,
                    candidate.proposedSigningPublicKey,
                    candidate.proposedEncryptionPublicKey,
                    now
                ) == 1
            ) { "Identity change was superseded or no longer matches the displayed keys" }
        } else {
            // A retry of the SAME accepted proposal is allowed after a transient
            // capability failure. The DB replacement still validates the old keys.
            check(
                candidate.confirmedPreviousEncryptionPublicKey != null &&
                    candidate.confirmedPreviousSigningPublicKey != null
            ) { "Identity change has no previous-key binding" }
        }
        mailboxCapabilityLifecycle.revokeForContact(peerId).getOrThrow()
        check(source.replaceConfirmedIdentity(peerId, invitationId, SystemClock.nowEpochMilliseconds())) {
            "Identity change is no longer current or an old-identity packet is still being sent; retry later"
        }
        // The approved-intent observer completes the ORIGINAL signed invitation when
        // its verified offer was durably stored. No second user approval is needed.
    }

    override suspend fun discard(peerId: String, invitationId: String): Result<Unit> = safeSuspendCall {
        source.discard(peerId, invitationId)
        Unit
    }
}

private fun PendingRemoteIdentityChange.toEntity() = PendingRemoteIdentityChangeEntity(
    peerId,
    sourcePeerId,
    invitationId,
    proposedEncryptionPublicKey.copyOf(),
    proposedSigningPublicKey.copyOf(),
    receivedAtEpochMilliseconds,
    expiresAtEpochMilliseconds,
    fingerprintConfirmedAtEpochMilliseconds,
    confirmedPreviousEncryptionPublicKey?.copyOf(),
    confirmedPreviousSigningPublicKey?.copyOf(),
    originalInviteChallenge?.copyOf(),
    originalInviteCreatedAtEpochMilliseconds,
    originalInviteAutoSharesIdentity
)

private fun PendingRemoteIdentityChangeEntity.toDomain() = PendingRemoteIdentityChange(
    peerId,
    sourcePeerId,
    invitationId,
    proposedEncryptionPublicKey.copyOf(),
    proposedSigningPublicKey.copyOf(),
    receivedAtEpochMilliseconds,
    expiresAtEpochMilliseconds,
    fingerprintConfirmedAtEpochMilliseconds,
    confirmedPreviousEncryptionPublicKey?.copyOf(),
    confirmedPreviousSigningPublicKey?.copyOf(),
    originalInviteChallenge?.copyOf(),
    originalInviteCreatedAtEpochMilliseconds,
    originalInviteAutoSharesIdentity
)
