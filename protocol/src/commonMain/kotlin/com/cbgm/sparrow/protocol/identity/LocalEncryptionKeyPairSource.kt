package com.cbgm.sparrow.protocol.identity

interface LocalEncryptionKeyPairSource {
    suspend fun getEncryptionKeyPair(): Result<LocalEncryptionKeyPair>
}
