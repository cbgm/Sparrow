package com.cbgm.sparrow.feature.identity.data.repository

import com.cbgm.sparrow.core.extensions.toFingerprint
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.entity.PendingRemoteIdentityChangeEntity
import com.cbgm.sparrow.feature.identity.data.datasource.PendingRemoteIdentityChangeDataSource
import com.cbgm.sparrow.feature.identity.domain.model.PendingRemoteIdentityChange
import com.cbgm.sparrow.feature.identity.domain.repository.PendingRemoteIdentityChangeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class PendingRemoteIdentityChangeRepositoryImpl(
    private val source: PendingRemoteIdentityChangeDataSource
) : PendingRemoteIdentityChangeRepository {
    override suspend fun stage(candidate: PendingRemoteIdentityChange): Result<Unit> = safeSuspendCall {
        require(candidate.peerId.isNotBlank() && candidate.sourcePeerId.isNotBlank())
        require(candidate.invitationId.isNotBlank())
        require(candidate.proposedEncryptionPublicKey.size == 32 && candidate.proposedSigningPublicKey.size == 32)
        require(candidate.expiresAtEpochMilliseconds > candidate.receivedAtEpochMilliseconds)
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

    override suspend fun confirmFingerprint(
        peerId: String,
        invitationId: String,
        independentlyCheckedSigningFingerprint: String
    ): Result<Unit> = safeSuspendCall {
        require(peerId.isNotBlank() && invitationId.isNotBlank())
        val candidate = source.find(peerId) ?: error("Identity change request no longer exists")
        check(candidate.invitationId == invitationId) { "A newer identity change request exists" }
        val supplied = independentlyCheckedSigningFingerprint.trim().uppercase()
            .filterNot { it == '-' || it.isWhitespace() }
        require(supplied.length == 64 && supplied.all { it in '0'..'9' || it in 'A'..'F' }) {
            "Enter the complete signing-key fingerprint verified directly with your contact"
        }
        check(supplied == candidate.proposedSigningPublicKey.toFingerprint().replace("-", "")) {
            "Fingerprint does not match the proposed identity"
        }
        val now = SystemClock.nowEpochMilliseconds()
        check(candidate.expiresAtEpochMilliseconds > now) { "Identity change request expired; ask for a new invitation" }
        check(source.confirmFingerprintIfCurrent(peerId, invitationId, candidate.proposedSigningPublicKey, now) == 1) {
            "Identity change was already confirmed, changed, or no longer matches the stored identity"
        }
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
    confirmedPreviousSigningPublicKey?.copyOf()
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
    confirmedPreviousSigningPublicKey?.copyOf()
)
