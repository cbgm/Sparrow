package com.cbgm.sparrow.feature.transport.domain.gateway

interface RoutingIdGenerator {
    fun deriveFromPhoneNumber(phoneNumber: String): Result<String>

    fun deriveFromSigningPublicKey(signingPublicKey: ByteArray): Result<String>
}
