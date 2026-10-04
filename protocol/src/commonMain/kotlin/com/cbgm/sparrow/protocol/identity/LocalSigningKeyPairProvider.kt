package com.cbgm.sparrow.protocol.identity

interface LocalSigningKeyPairProvider {
    suspend fun getSigningKeyPair(): Result<LocalSigningKeyPair>
}
