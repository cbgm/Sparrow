package com.cbgm.sparrow.feature.transport.push

import com.cbgm.sparrow.feature.transport.signing.lengthPrefixed
import com.cbgm.sparrow.feature.transport.signing.toBigEndianBytes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Binds this device's push token to its routing ID. The signature over [pushDeviceRegistrationPayload]
 * proves the routing ID belongs to this identity key, so nobody else can redirect our wake-ups.
 */
@Serializable
class PushDeviceRegistrationRequest(
    @SerialName("routingId")
    val routingId: String,
    val token: String,
    val platform: String,
    val issuedAtEpochMilliseconds: Long,
    val clientSigningPublicKey: ByteArray,
    val clientSignature: ByteArray
)

/**
 * Bytes signed to register a push token. Must match the server's ProtocolSignatures.pushDeviceContent
 * byte for byte: length-prefixed fields, its own domain string, issue time as 8-byte big-endian.
 */
fun pushDeviceRegistrationPayload(
    routingId: String,
    token: String,
    platform: String,
    issuedAtEpochMilliseconds: Long
): ByteArray =
    lengthPrefixed(
        PUSH_DEVICE_DOMAIN.encodeToByteArray(),
        routingId.encodeToByteArray(),
        token.encodeToByteArray(),
        platform.encodeToByteArray(),
        issuedAtEpochMilliseconds.toBigEndianBytes()
    )

private const val PUSH_DEVICE_DOMAIN = "sparrow-push-device-v1"
