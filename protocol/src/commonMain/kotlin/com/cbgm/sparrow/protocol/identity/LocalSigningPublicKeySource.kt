package com.cbgm.sparrow.protocol.identity

interface LocalSigningPublicKeySource {
    suspend fun getSigningPublicKey(): Result<ByteArray>
}
