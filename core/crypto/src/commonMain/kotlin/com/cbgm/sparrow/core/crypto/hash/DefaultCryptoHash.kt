package com.cbgm.sparrow.core.crypto.hash

import org.kotlincrypto.hash.sha2.SHA256

class DefaultCryptoHash : CryptoHash {
    override fun sha256(input: ByteArray) = SHA256().digest(input = input)
}
