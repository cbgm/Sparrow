package com.cbgm.securechat.feature.transport.routing

interface RoutingIdGenerator {
    fun deriveFromPhoneNumber(phoneNumber: String): Result<String>

    fun deriveFromSigningPublicKey(signingPublicKey: ByteArray): Result<String>
}
