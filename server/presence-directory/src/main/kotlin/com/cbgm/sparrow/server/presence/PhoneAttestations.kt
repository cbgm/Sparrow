package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.security.ClientRoutingIds
import com.cbgm.sparrow.server.security.Signatures
import kotlinx.serialization.Serializable
import java.security.PublicKey
import java.util.Base64

/**
 * Checks phone attestations of the phone verifier next to the Control Plane Directory
 * (`server/control-plane-directory/phone_verifier.py`): the Directory key this Control Plane pins certifies the
 * verifier's attestation key, and that key signs that a phone alias belongs to one device routing ID until it expires.
 * The presence directory never sees a phone token or number.
 */
internal class PhoneAttestations(
    private val directoryPublicKey: PublicKey?,
    private val now: () -> Long = System::currentTimeMillis
) {
    /** The alias [attestation] proves for [routingId], or null when it proves nothing. */
    fun provenAlias(
        attestation: String,
        routingId: String
    ): String? {
        val directoryKey = directoryPublicKey ?: return null
        return runCatching {
            val signed = serverJson.decodeFromString<SignedPhoneAttestation>(attestation)
            val keys = serverJson.decodeFromString<PhoneKeys>(signed.keys.verified(PHONE_KEYS_DOMAIN, directoryKey))
            val attestationKey = Signatures.decodePublicKey(decode(keys.attestationKey))
            val claim = serverJson.decodeFromString<PhoneAttestation>(signed.verified(ATTESTATION_DOMAIN, attestationKey))
            claim.alias.takeIf { keys.protocol == PHONE_KEYS_PROTOCOL && claim.proves(routingId, now()) }
        }.getOrNull()
    }

    private fun PhoneAttestation.proves(
        deviceRoutingId: String,
        now: Long
    ): Boolean =
        protocol == ATTESTATION_PROTOCOL &&
            routingId == deviceRoutingId &&
            now in issuedAtEpochMilliseconds - CLOCK_SKEW_MILLISECONDS..expiresAtEpochMilliseconds &&
            ClientRoutingIds.isBootstrapRoutingId(alias)

    private fun Signed.verified(
        domain: String,
        key: PublicKey
    ): String {
        val content = decode(payload)
        check(Signatures.verify(domain.encodeToByteArray() + content, decode(signature), key))
        return content.decodeToString()
    }

    private fun decode(value: String): ByteArray = Base64.getUrlDecoder().decode(value)

    private companion object {
        const val PHONE_KEYS_PROTOCOL = "sparrow-phone-keys-v1"
        const val PHONE_KEYS_DOMAIN = "sparrow-phone-keys-v1\n"
        const val ATTESTATION_PROTOCOL = "sparrow-phone-attestation-v1"
        const val ATTESTATION_DOMAIN = "sparrow-phone-attestation-v1\n"
        const val CLOCK_SKEW_MILLISECONDS = 5L * 60L * 1_000L
    }
}

/** The pinned Directory key: base64url X.509 DER Ed25519, as in `CONTROL_PLANE_DIRECTORY_PUBLIC_KEY`. */
internal fun directoryPublicKey(value: String?): PublicKey? =
    value?.trim()?.takeIf(String::isNotEmpty)?.let { key -> Signatures.decodePublicKey(Base64.getUrlDecoder().decode(key)) }

private sealed interface Signed {
    /** Base64url of the signed JSON bytes. */
    val payload: String
    val signature: String
}

@Serializable
private class SignedPhoneAttestation(
    override val payload: String,
    override val signature: String,
    val keys: SignedPhoneKeys
) : Signed

@Serializable
private class SignedPhoneKeys(
    override val payload: String,
    override val signature: String
) : Signed

@Serializable
private class PhoneKeys(
    val protocol: String,
    val attestationKey: String,
    val requestKey: String
)

@Serializable
private class PhoneAttestation(
    val protocol: String,
    val alias: String,
    val routingId: String,
    val issuedAtEpochMilliseconds: Long,
    val expiresAtEpochMilliseconds: Long
)
