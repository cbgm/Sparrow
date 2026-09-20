package com.cbgm.sparrow.feature.identity.domain.repository

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.IdentityAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.IdentityPacket
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchange
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeAcceptance
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeBinding
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeClosure
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeOffer
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeReady
import com.cbgm.sparrow.feature.identity.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.identity.domain.model.IdentityPeerState
import com.cbgm.sparrow.feature.identity.domain.model.IdentityResult
import kotlinx.coroutines.flow.Flow

interface IdentityExchangeRepository {
    suspend fun stageRemoteIdentity(
        peerId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Boolean>

    suspend fun acceptRemoteIdentity(
        peerId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Unit>

    suspend fun establishMutualIdentity(
        peerId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Unit>

    suspend fun ensureRemoteSigningIdentity(
        peerId: String,
        signingPublicKey: ByteArray
    ): Result<Unit>

    suspend fun start(peerId: String, invitationSenderLabel: String): Result<IdentityExchange?>

    suspend fun startManual(peerId: String): Result<Unit>

    suspend fun accept(exchangeId: String): Result<Unit>

    suspend fun decline(exchangeId: String): Result<Unit>

    fun observeState(peerId: String): Flow<IdentityHandshakeState?>

    fun observeResults(): Flow<List<IdentityResult>>

    suspend fun cancel(peerId: String): Result<Unit>

    suspend fun getPeerState(peerId: String): Result<IdentityPeerState>

    suspend fun getExchangeClosure(peerId: String): Result<IdentityExchangeClosure?>

    suspend fun closeExchange(exchangeId: String, peerId: String): Result<Unit>

    suspend fun getExchangeBinding(exchangeId: String): Result<IdentityExchangeBinding?>

    suspend fun invalidateExchange(
        exchangeId: String,
        peerId: String,
        expectedChallenge: ByteArray,
        expectedSigningPublicKey: ByteArray,
        atEpochMilliseconds: Long
    ): Result<Unit>

    suspend fun receiveExchange(
        context: IncomingPacketContext,
        offer: IdentityExchangeOffer,
        wasKnownPeerAtReceive: Boolean
    ): Result<Unit>

    suspend fun reassignPeer(
        fromPeerId: String,
        toPeerId: String
    ): Result<Unit>

    suspend fun receiveAccepted(
        context: IncomingPacketContext,
        acceptance: IdentityExchangeAcceptance
    ): Result<Unit>

    suspend fun recordRemoteDecline(
        exchangeId: String,
        peerId: String,
        inviteChallenge: ByteArray,
        remoteSigningPublicKey: ByteArray,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit>

    suspend fun receiveReady(
        context: IncomingPacketContext,
        ready: IdentityExchangeReady
    ): Result<Unit>

    suspend fun receiveManualIdentity(
        context: IncomingPacketContext,
        packet: IdentityPacket
    ): Result<Boolean>

    suspend fun receiveIdentityAcknowledgement(
        context: IncomingPacketContext,
        packet: IdentityAcknowledgementPacket
    ): Result<Boolean>
}
