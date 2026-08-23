package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.protocol.BlobUploadTicketClaims
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

class BlobUploadPermitStore(
    private val random: SecureRandom = SecureRandom()
) {
    private val permits = ConcurrentHashMap<String, BlobUploadTicketClaims>()

    fun issue(claims: BlobUploadTicketClaims): String {
        while (true) {
            val bytes = ByteArray(TOKEN_BYTES).also(random::nextBytes)
            val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
            if (permits.putIfAbsent(token, claims) == null) return token
        }
    }

    fun consume(token: String): BlobUploadTicketClaims? = permits.remove(token)

    fun purgeExpired(nowEpochMilliseconds: Long) {
        permits.entries.removeIf { (_, claims) ->
            claims.ticketExpiresAtEpochMilliseconds <= nowEpochMilliseconds
        }
    }

    private companion object {
        const val TOKEN_BYTES = 32
    }
}
