package com.cbgm.sparrow.feature.identity.data.repository

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.IdentityAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.IdentityPacket
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.data.datasource.IdentityExchangeDataSource
import com.cbgm.sparrow.feature.identity.data.datasource.ManualIdentityExchangeDataSource
import com.cbgm.sparrow.feature.identity.data.datasource.RemoteIdentityDataSource
import com.cbgm.sparrow.feature.identity.data.model.IdentityAcceptanceReviewRequiredDtoException
import com.cbgm.sparrow.feature.identity.domain.error.IdentityAcceptanceRequiresReviewException
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchange
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeAcceptance
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeBinding
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeClosure
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeOffer
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeReady
import com.cbgm.sparrow.feature.identity.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.identity.domain.model.IdentityPeerState
import com.cbgm.sparrow.feature.identity.domain.model.IdentityResult
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository
import kotlinx.coroutines.flow.Flow

internal class IdentityExchangeRepositoryImpl(
    private val dataSource: IdentityExchangeDataSource,
    private val manualDataSource: ManualIdentityExchangeDataSource,
    private val remoteIdentityDataSource: RemoteIdentityDataSource
) : IdentityExchangeRepository {
    override suspend fun stageRemoteIdentity(
        peerId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Boolean> =
        safeSuspendCall {
            remoteIdentityDataSource.stage(peerId, encryptionPublicKey, signingPublicKey)
        }

    override suspend fun acceptRemoteIdentity(
        peerId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Unit> =
        safeSuspendCall {
            remoteIdentityDataSource.accept(peerId, encryptionPublicKey, signingPublicKey)
        }

    override suspend fun establishMutualIdentity(
        peerId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Unit> =
        safeSuspendCall {
            remoteIdentityDataSource.accept(peerId, encryptionPublicKey, signingPublicKey)
            remoteIdentityDataSource.markMutual(peerId, encryptionPublicKey, signingPublicKey)
        }

    override suspend fun ensureRemoteSigningIdentity(
        peerId: String,
        signingPublicKey: ByteArray
    ): Result<Unit> =
        safeSuspendCall {
            remoteIdentityDataSource.ensureSigningIdentityMatches(peerId, signingPublicKey)
        }

    override suspend fun start(peerId: String, invitationSenderLabel: String): Result<IdentityExchange?> =
        dataSource.start(peerId, invitationSenderLabel)

    override suspend fun startManual(peerId: String): Result<Unit> = dataSource.startManual(peerId)

    override suspend fun accept(exchangeId: String): Result<Unit> = dataSource.accept(exchangeId)

    override suspend fun decline(exchangeId: String): Result<Unit> = dataSource.decline(exchangeId)

    override fun observeState(peerId: String): Flow<IdentityHandshakeState?> = dataSource.observeState(peerId)

    override fun observeResults(): Flow<List<IdentityResult>> = dataSource.observeResults()

    override suspend fun cancel(peerId: String): Result<Unit> = dataSource.cancel(peerId)

    override suspend fun getPeerState(peerId: String): Result<IdentityPeerState> =
        dataSource.getPeerState(peerId)

    override suspend fun getExchangeClosure(peerId: String): Result<IdentityExchangeClosure?> =
        dataSource.getExchangeClosure(peerId)

    override suspend fun closeExchange(exchangeId: String, peerId: String): Result<Unit> =
        dataSource.closeExchange(exchangeId, peerId)

    override suspend fun getExchangeBinding(exchangeId: String): Result<IdentityExchangeBinding?> =
        dataSource.getExchangeBinding(exchangeId).map { binding ->
            binding?.let {
                IdentityExchangeBinding(
                    exchangeId = it.exchangeId,
                    peerId = it.peerId,
                    inviteChallenge = it.inviteChallenge,
                    remoteSigningPublicKey = it.remoteSigningPublicKey
                )
            }
        }

    override suspend fun invalidateExchange(
        exchangeId: String,
        peerId: String,
        expectedChallenge: ByteArray,
        expectedSigningPublicKey: ByteArray,
        atEpochMilliseconds: Long
    ): Result<Unit> = dataSource.invalidateExchange(
        exchangeId = exchangeId,
        peerId = peerId,
        expectedChallenge = expectedChallenge,
        expectedSigningPublicKey = expectedSigningPublicKey,
        atEpochMilliseconds = atEpochMilliseconds
    )

    override suspend fun receiveExchange(
        context: IncomingPacketContext,
        offer: IdentityExchangeOffer,
        wasKnownPeerAtReceive: Boolean
    ): Result<Unit> = dataSource.receiveExchange(context, offer, wasKnownPeerAtReceive)

    override suspend fun reassignPeer(
        fromPeerId: String,
        toPeerId: String
    ): Result<Unit> = dataSource.reassignPeer(fromPeerId, toPeerId)

    override suspend fun receiveAccepted(
        context: IncomingPacketContext,
        acceptance: IdentityExchangeAcceptance
    ): Result<Unit> = dataSource.receiveAccepted(context, acceptance).recoverCatching { error ->
        if (error is IdentityAcceptanceReviewRequiredDtoException) {
            throw IdentityAcceptanceRequiresReviewException(
                peerId = error.peerId,
                invitationExpiresAtEpochMilliseconds = error.invitationExpiresAtEpochMilliseconds
            )
        }
        throw error
    }

    override suspend fun recordRemoteDecline(
        exchangeId: String,
        peerId: String,
        inviteChallenge: ByteArray,
        remoteSigningPublicKey: ByteArray,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> = dataSource.recordRemoteDecline(
        exchangeId = exchangeId,
        peerId = peerId,
        inviteChallenge = inviteChallenge,
        remoteSigningPublicKey = remoteSigningPublicKey,
        receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
    )

    override suspend fun receiveReady(
        context: IncomingPacketContext,
        ready: IdentityExchangeReady
    ): Result<Unit> = dataSource.receiveReady(context, ready)

    override suspend fun receiveManualIdentity(
        context: IncomingPacketContext,
        packet: IdentityPacket
    ): Result<Boolean> = manualDataSource.receiveIdentity(context, packet)

    override suspend fun receiveIdentityAcknowledgement(
        context: IncomingPacketContext,
        packet: IdentityAcknowledgementPacket
    ): Result<Boolean> = manualDataSource.receiveAcknowledgement(context, packet)
}
