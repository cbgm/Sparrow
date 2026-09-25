package com.cbgm.sparrow.feature.transport.controlplane

import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.extensions.toHexString
import com.cbgm.sparrow.core.time.SystemClock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** The directory's authenticated wire format, shared with the independent Python service. */
@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
internal data class DirectoryEnvelope(
    val payload: DirectoryPayload,
    val keyId: String,
    val signature: String
)

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
internal data class DirectoryPayload(
    val protocol: String,
    val version: Long,
    val generatedAtEpochMilliseconds: Long,
    val validUntilEpochMilliseconds: Long,
    val controlPlanes: List<DirectoryPlane>,
    val revokedControlPlaneIds: List<String>
)

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
internal data class DirectoryPlane(
    val controlPlaneId: String,
    val baseUrl: String,
    val publicKey: String
)

/** A URL only supplies a network location; the independently pinned signing key supplies trust. */
class SignedControlPlaneDirectoryVerifier(
    private val json: Json,
    private val signatureCrypto: DetachedSignatureCrypto,
    private val hash: CryptoHash,
    private val now: () -> Long = SystemClock::nowEpochMilliseconds
) {
    internal suspend fun verify(document: String, pinnedPublicKey: String, allowExpired: Boolean = false): DirectoryPayload {
        require(document.encodeToByteArray().size <= MAX_DOCUMENT_BYTES) { "Directory document is too large" }
        val raw = json.parseToJsonElement(document).jsonObject
        require(raw.keys == ENVELOPE_KEYS) { "Unexpected directory envelope fields" }
        val payloadJson = raw.getValue("payload").jsonObject
        require(payloadJson.keys == PAYLOAD_KEYS) { "Unexpected directory payload fields" }
        val envelope = json.decodeFromString<DirectoryEnvelope>(document)
        val payload = envelope.payload
        val der = decodeCanonicalBase64Url(pinnedPublicKey)
        val publicKey = rawPublicKey(der)
        require(envelope.keyId == hash.sha256(der).toHexString()) {
            "Directory signing key is not the configured trust anchor"
        }
        val encodedSignature = decodeCanonicalBase64Url(envelope.signature)
        require(encodedSignature.size == SIGNATURE_LENGTH) { "Invalid directory signature length" }
        signatureCrypto.verify(
            payload = SNAPSHOT_DOMAIN + canonicalPayload(payload).encodeToByteArray(),
            signingPublicKey = publicKey,
            signature = encodedSignature
        ).getOrThrow()

        val time = now()
        require(payload.protocol == PROTOCOL && payload.version >= 0L) { "Unsupported directory protocol/version" }
        require(
            payload.generatedAtEpochMilliseconds >= 0L &&
                payload.generatedAtEpochMilliseconds <= time + MAX_CLOCK_SKEW_MS &&
                payload.validUntilEpochMilliseconds > payload.generatedAtEpochMilliseconds &&
                payload.validUntilEpochMilliseconds - payload.generatedAtEpochMilliseconds <= MAX_SNAPSHOT_AGE_MS
        ) { "Invalid directory timestamps" }
        require(allowExpired || time < payload.validUntilEpochMilliseconds) { "Directory snapshot expired" }
        require(payload.controlPlanes.size <= MAX_PLANES && payload.revokedControlPlaneIds.size <= MAX_REVOKED) {
            "Directory exceeds entry limits"
        }
        require(
            payload.revokedControlPlaneIds.all(::isIdentity) &&
                payload.revokedControlPlaneIds.size == payload.revokedControlPlaneIds.toSet().size
        ) { "Invalid or duplicated revoked identity" }
        val revoked = payload.revokedControlPlaneIds.toSet()
        val identities = mutableSetOf<String>()
        val urls = mutableSetOf<String>()
        payload.controlPlanes.forEach { plane ->
            require(isIdentity(plane.controlPlaneId) && plane.controlPlaneId !in revoked) {
                "Invalid/revoked Control Plane identity"
            }
            require(identities.add(plane.controlPlaneId)) { "Duplicate Control Plane identity" }
            require(urls.add(plane.baseUrl) && isCanonicalHttpsOrigin(plane.baseUrl)) {
                "Invalid/duplicate Control Plane HTTPS origin"
            }
            require(
                hash.sha256(decodeCanonicalBase64Url(plane.publicKey).also { rawPublicKey(it) })
                    .toHexString() == plane.controlPlaneId
            ) { "Control Plane public key does not match its signed identity" }
        }
        return payload
    }

    private fun canonicalPayload(payload: DirectoryPayload): String = buildString {
        // Python: json.dumps(payload, sort_keys=True, separators=(",", ":"), ensure_ascii=False)
        // All wire strings here are restricted to ASCII (identifiers, base64url, canonical DNS origins).
        append("{\"controlPlanes\":[")
        payload.controlPlanes.forEachIndexed { index, plane ->
            if (index != 0) append(',')
            append("{\"baseUrl\":")
            append(json.encodeToString(plane.baseUrl))
            append(",\"controlPlaneId\":")
            append(json.encodeToString(plane.controlPlaneId))
            append(",\"publicKey\":")
            append(json.encodeToString(plane.publicKey))
            append('}')
        }
        append("],\"generatedAtEpochMilliseconds\":")
        append(payload.generatedAtEpochMilliseconds)
        append(",\"protocol\":")
        append(json.encodeToString(payload.protocol))
        append(",\"revokedControlPlaneIds\":[")
        payload.revokedControlPlaneIds.forEachIndexed { index, identity ->
            if (index != 0) append(',')
            append(json.encodeToString(identity))
        }
        append("],\"validUntilEpochMilliseconds\":")
        append(payload.validUntilEpochMilliseconds)
        append(",\"version\":")
        append(payload.version)
        append('}')
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeCanonicalBase64Url(value: String): ByteArray {
        require(
            value.isNotEmpty() && value.length <= 512 && value.all {
                it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '-' || it == '_'
            }
        ) { "Invalid base64url encoding" }
        // The Directory Server deliberately sends canonical *unpadded* base64url.
        // Kotlin's default decoder expects padding for the final quantum, while
        // Python's server decoder restores it before decoding. Match the wire
        // protocol without permitting noncanonical, padded or truncated input.
        require(value.length % 4 != 1) { "Invalid base64url length" }
        val padded = value + "=".repeat((4 - value.length % 4) % 4)
        val bytes = Base64.UrlSafe.decode(padded)
        require(Base64.UrlSafe.encode(bytes).trimEnd('=') == value) { "Noncanonical base64url" }
        return bytes
    }

    private fun rawPublicKey(der: ByteArray): ByteArray {
        require(
            der.size == ED25519_DER_PREFIX.size + ED25519_RAW_KEY_LENGTH &&
                der.copyOfRange(0, ED25519_DER_PREFIX.size).contentEquals(ED25519_DER_PREFIX)
        ) { "Directory/Control Plane key must be X.509 DER Ed25519" }
        return der.copyOfRange(ED25519_DER_PREFIX.size, der.size)
    }

    private fun isIdentity(value: String): Boolean =
        value.length == 64 && value.all { it in '0'..'9' || it in 'a'..'f' }

    private fun isCanonicalHttpsOrigin(value: String): Boolean {
        if (!value.startsWith("https://") || value.length > 253) return false
        val host = value.removePrefix("https://")
        if (host != host.lowercase() || host == "localhost" || host.endsWith(".local") ||
            host.endsWith(".internal") || host.count { it == '.' } < 1 ||
            host.all { it == '.' || it in '0'..'9' }
        ) {
            return false
        }
        return host.split('.').all { label ->
            label.length in 1..63 && label.first().isLetterOrDigit() && label.last().isLetterOrDigit() &&
                label.all { it in 'a'..'z' || it in '0'..'9' || it == '-' }
        }
    }

    private companion object {
        const val PROTOCOL = "sparrow-control-plane-directory-v1"
        val SNAPSHOT_DOMAIN = "sparrow-control-plane-directory-snapshot-v1\n".encodeToByteArray()
        val ENVELOPE_KEYS = setOf("payload", "keyId", "signature")
        val PAYLOAD_KEYS = setOf(
            "protocol",
            "version",
            "generatedAtEpochMilliseconds",
            "validUntilEpochMilliseconds",
            "controlPlanes",
            "revokedControlPlaneIds"
        )
        val ED25519_DER_PREFIX = byteArrayOf(0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00)
        const val ED25519_RAW_KEY_LENGTH = 32
        const val SIGNATURE_LENGTH = 64
        const val MAX_DOCUMENT_BYTES = 512 * 1024
        const val MAX_PLANES = 2048
        const val MAX_REVOKED = 4096
        const val MAX_CLOCK_SKEW_MS = 5 * 60 * 1000L
        const val MAX_SNAPSHOT_AGE_MS = 24 * 60 * 60 * 1000L
    }
}
