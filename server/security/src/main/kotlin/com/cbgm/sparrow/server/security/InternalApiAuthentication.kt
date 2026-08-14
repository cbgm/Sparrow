package com.cbgm.sparrow.server.security

import java.security.MessageDigest

object InternalApiAuthentication {
    const val TOKEN_HEADER = "X-Sparrow-Internal-Token"

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
