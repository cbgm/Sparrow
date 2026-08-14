package com.cbgm.sparrow.feature.transport.routing

import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer
import okio.ByteString.Companion.toByteString

class Sha256RoutingIdGenerator(
    private val phoneNumberNormalizer: PhoneNumberNormalizer
) : RoutingIdGenerator {
    override fun deriveFromPhoneNumber(phoneNumber: String): Result<String> =
        runCatching {
            val normalizedPhoneNumber =
                phoneNumberNormalizer.normalize(phoneNumber = phoneNumber).getOrThrow()

            val digest =
                normalizedPhoneNumber
                    .encodeToByteArray()
                    .toByteString()
                    .sha256()
                    .base64Url()
                    .trimEnd('=')

            "$BOOTSTRAP_ROUTING_ID_PREFIX$digest"
        }

    override fun deriveFromSigningPublicKey(signingPublicKey: ByteArray): Result<String> =
        runCatching {
            require(signingPublicKey.isNotEmpty()) {
                "Signing public key must not be empty"
            }

            val digest =
                signingPublicKey
                    .toByteString()
                    .sha256()
                    .base64Url()
                    .trimEnd('=')

            "$DEVICE_ROUTING_ID_PREFIX$digest"
        }

    private companion object {
        const val BOOTSTRAP_ROUTING_ID_PREFIX = "scphone1_"
        const val DEVICE_ROUTING_ID_PREFIX = "scrouting1_"
    }
}
