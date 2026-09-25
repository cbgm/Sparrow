package com.cbgm.sparrow.feature.identity.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.data.database.entity.ContactPublicIdentityEntity
import com.cbgm.sparrow.feature.identity.data.datasource.RemoteIdentityDataSource
import com.cbgm.sparrow.feature.identity.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.identity.domain.model.RemotePeerIdentity
import com.cbgm.sparrow.feature.identity.domain.repository.RemoteIdentityReadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RemoteIdentityReadRepositoryImpl(
    private val dataSource: RemoteIdentityDataSource
) : RemoteIdentityReadRepository {
    override suspend fun get(peerId: String): Result<RemotePeerIdentity?> = safeSuspendCall {
        dataSource.findByPeerId(peerId)?.toRemotePeerIdentity()
    }

    override suspend fun findPeerIdBySigningPublicKey(signingPublicKey: ByteArray): Result<String?> = safeSuspendCall {
        require(signingPublicKey.isNotEmpty()) { "Signing public key must not be empty" }
        dataSource.findBySigningPublicKey(signingPublicKey)?.contactId
    }

    override fun observeAll(): Flow<List<RemotePeerIdentity>> =
        dataSource.observeAll().map { identities -> identities.map { it.toRemotePeerIdentity() } }
}

private fun ContactPublicIdentityEntity.toRemotePeerIdentity(): RemotePeerIdentity = RemotePeerIdentity(
    peerId = contactId,
    encryptionPublicKey = encryptionPublicKey.copyOf(),
    signingPublicKey = signingPublicKey.copyOf(),
    verificationStatus = ContactVerificationStatus.valueOf(verificationStatus),
    keyExchangeStatus = KeyExchangeStatus.valueOf(keyExchangeStatus),
    verifiedByContact = verifiedByContact,
    locallyImported = locallyImported,
    updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
)
