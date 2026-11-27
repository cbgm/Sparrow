package com.cbgm.sparrow.core.crypto.hash

import org.kotlincrypto.hash.sha2.SHA256

class Sha256CryptoHash : CryptoHash {
    override fun sha256(input: ByteArray) = SHA256().digest(input = input)
}
