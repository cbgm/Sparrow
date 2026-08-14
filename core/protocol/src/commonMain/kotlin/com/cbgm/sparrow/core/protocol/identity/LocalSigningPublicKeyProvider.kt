package com.cbgm.sparrow.core.protocol.identity

interface LocalSigningPublicKeyProvider {
    suspend fun getSigningPublicKey(): Result<ByteArray>
}
