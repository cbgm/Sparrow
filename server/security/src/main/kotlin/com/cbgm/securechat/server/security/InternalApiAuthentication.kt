package com.cbgm.securechat.server.security

import java.security.MessageDigest

object InternalApiAuthentication {
    const val TOKEN_HEADER = "X-SecureChat-Internal-Token"

    fun isAuthorized(
        expectedToken: String?,
        presentedToken: String?
    ): Boolean =
        expectedToken?.let { expected ->
            presentedToken?.let { presented ->
                MessageDigest.isEqual(
                    expected.encodeToByteArray(),
                    presented.encodeToByteArray()
                )
            } ?: false
        } ?: true
}
