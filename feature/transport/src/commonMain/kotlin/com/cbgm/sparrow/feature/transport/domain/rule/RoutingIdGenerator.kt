package com.cbgm.sparrow.feature.transport.domain.rule

interface RoutingIdGenerator {
    fun deriveFromPhoneNumber(phoneNumber: String): Result<String>

    fun deriveFromSigningPublicKey(signingPublicKey: ByteArray): Result<String>
}
