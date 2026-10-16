package com.cbgm.sparrow.feature.identity.presentation.details.mapper

import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity
import com.cbgm.sparrow.feature.identity.presentation.details.model.PublicHexUi

fun PublicIdentity.toUi(): PublicHexUi = PublicHexUi(
    encryptionPublicKey = encryptionPublicKey.toHexValueString(),
    signingPublicKey = signingPublicKey.toHexValueString()
)

private fun ByteArray.toHexValueString(): String {
    val hexChars = "0123456789ABCDEF"
    val result = StringBuilder(size * 2)
    for (byte in this) {
        val octet = byte.toInt() and 0xFF
        result.append(hexChars[octet ushr 4])
        result.append(hexChars[octet and 0x0F])
    }
    return result.toString()
}
