package com.cbgm.securechat.server.security

import java.security.MessageDigest
import java.util.Base64

object ClientRoutingIds {
    fun fromSigningPublicKey(signingPublicKey: ByteArray): String {
        require(signingPublicKey.isNotEmpty()) { "Signing public key must not be empty" }
        val digest = MessageDigest.getInstance("SHA-256").digest(signingPublicKey)
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
        return "$DEVICE_ROUTING_ID_PREFIX$encoded"
    }

    fun matchesSigningPublicKey(routingId: String, signingPublicKey: ByteArray): Boolean =
        runCatching { routingId == fromSigningPublicKey(signingPublicKey) }.getOrDefault(false)

    fun isDeviceRoutingId(routingId: String): Boolean =
        routingId.startsWith(DEVICE_ROUTING_ID_PREFIX) &&
            routingId.length > DEVICE_ROUTING_ID_PREFIX.length

    fun isBootstrapRoutingId(routingId: String): Boolean =
        routingId.startsWith(BOOTSTRAP_ROUTING_ID_PREFIX) &&
            routingId.length > BOOTSTRAP_ROUTING_ID_PREFIX.length

    private const val DEVICE_ROUTING_ID_PREFIX = "scrouting1_"
    private const val BOOTSTRAP_ROUTING_ID_PREFIX = "scphone1_"
}
