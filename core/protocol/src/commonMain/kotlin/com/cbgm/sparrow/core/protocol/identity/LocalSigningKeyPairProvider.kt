package com.cbgm.sparrow.core.protocol.identity

interface LocalSigningKeyPairProvider {
    suspend fun getSigningKeyPair(): Result<LocalSigningKeyPair>
}
