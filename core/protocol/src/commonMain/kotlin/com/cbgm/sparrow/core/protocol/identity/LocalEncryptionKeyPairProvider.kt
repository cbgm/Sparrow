package com.cbgm.sparrow.core.protocol.identity

interface LocalEncryptionKeyPairProvider {
    suspend fun getEncryptionKeyPair(): Result<LocalEncryptionKeyPair>
}
