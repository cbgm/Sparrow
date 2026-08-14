package com.cbgm.securechat.server.security

import java.security.MessageDigest

object NodeIds {
    fun fromPublicKey(publicKey: ByteArray): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(publicKey)
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
}
