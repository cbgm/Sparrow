package com.cbgm.sparrow.core.crypto.hash

interface CryptoHash {
    /**
     * Returns the complete SHA-256 digest.
     */
    fun sha256(input: ByteArray): ByteArray
}
