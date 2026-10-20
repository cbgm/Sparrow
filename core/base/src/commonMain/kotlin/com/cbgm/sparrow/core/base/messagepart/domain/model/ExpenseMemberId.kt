package com.cbgm.sparrow.core.base.messagepart.domain.model

/** Expense attachments identify members by their group signing key, never by local contact IDs. */
object ExpenseMemberId {
    fun fromSigningPublicKey(signingPublicKey: ByteArray): String {
        require(signingPublicKey.isNotEmpty()) { "Missing group signing key" }
        return buildString(signingPublicKey.size * 2) {
            for (byte in signingPublicKey) {
                val value = byte.toInt() and 0xff
                append(HEX[value ushr 4])
                append(HEX[value and 15])
            }
        }
    }

    private const val HEX = "0123456789abcdef"
}
