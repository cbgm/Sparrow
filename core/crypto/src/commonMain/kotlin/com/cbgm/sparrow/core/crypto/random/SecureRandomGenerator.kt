package com.cbgm.sparrow.core.crypto.random

interface SecureRandomGenerator {
    suspend fun generateBytes(size: Int): Result<ByteArray>
}
