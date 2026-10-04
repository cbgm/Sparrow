package com.cbgm.sparrow.protocol.identity

interface LocalEncryptionKeyPairProvider {
    suspend fun getEncryptionKeyPair(): Result<LocalEncryptionKeyPair>
}
