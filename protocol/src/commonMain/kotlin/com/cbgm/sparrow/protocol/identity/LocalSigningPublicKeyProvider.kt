package com.cbgm.sparrow.protocol.identity

interface LocalSigningPublicKeyProvider {
    suspend fun getSigningPublicKey(): Result<ByteArray>
}
