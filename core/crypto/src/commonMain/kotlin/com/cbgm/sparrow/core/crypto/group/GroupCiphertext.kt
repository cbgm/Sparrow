package com.cbgm.sparrow.core.crypto.group

data class GroupCiphertext(
    val nonce: ByteArray,
    val ciphertext: ByteArray
) {
    init {
        require(nonce.isNotEmpty()) { "Group-message nonce must not be empty" }
        require(ciphertext.isNotEmpty()) { "Group-message ciphertext must not be empty" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupCiphertext) return false

        return nonce.contentEquals(other.nonce) &&
            ciphertext.contentEquals(other.ciphertext)
    }

    override fun hashCode(): Int {
        var result = nonce.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        return result
    }
}
