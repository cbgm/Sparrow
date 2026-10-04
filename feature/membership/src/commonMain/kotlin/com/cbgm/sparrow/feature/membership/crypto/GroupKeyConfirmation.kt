package com.cbgm.sparrow.feature.membership.crypto

import com.cbgm.sparrow.core.bytes.ByteArrays
import com.cbgm.sparrow.core.crypto.hash.CryptoHash

/** Protocol v1 confirmation; changing the encoding invalidates persisted group handshakes. */
object GroupKeyConfirmation {
    private val domain = "sparrow.group-key-confirmation.v1".encodeToByteArray()

    fun create(cryptoHash: CryptoHash, groupId: String, epoch: Int, groupKey: ByteArray): ByteArray {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(epoch > 0) { "Group epoch must be positive" }
        require(groupKey.isNotEmpty()) { "Group key must not be empty" }
        return cryptoHash.sha256(
            ByteArrays.concatenate(
                domain,
                ByteArrays.withLengthPrefix(groupKey),
                ByteArrays.withLengthPrefix(groupId.encodeToByteArray()),
                ByteArrays.encodeInt(epoch)
            )
        )
    }
}
