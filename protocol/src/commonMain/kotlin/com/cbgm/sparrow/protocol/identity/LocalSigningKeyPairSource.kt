package com.cbgm.sparrow.protocol.identity

interface LocalSigningKeyPairSource {
    suspend fun getSigningKeyPair(): Result<LocalSigningKeyPair>
}
