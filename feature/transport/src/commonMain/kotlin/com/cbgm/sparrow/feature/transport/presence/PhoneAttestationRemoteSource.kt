package com.cbgm.sparrow.feature.transport.presence

import com.cbgm.sparrow.core.crypto.sealing.SealedMessageCipher
import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.transport.ControlPlaneConfiguration
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

/**
 * Gets the phone attestation for this device from the phone verifier next to the Control Plane Directory
 * (`server/control-plane-directory/phone_verifier.py`): the Firebase phone token goes sealed for the verifier's request
 * key, whose certificate is checked against the pinned Directory key, through the relays of the Control Plane client.
 * Neither relay reads the token or the answer, and the verifier does not see the app's address. The attestation is
 * kept in memory only and goes into the alias claims; every presence directory checks it.
 */
internal class PhoneAttestationRemoteSource(
    private val controlPlaneHttpClient: HttpClient,
    private val controlPlaneConfiguration: ControlPlaneConfiguration,
    /** The pinned Directory key, base64url X.509 DER Ed25519. */
    private val pinnedDirectoryKey: suspend () -> String?,
    private val signatureCrypto: DetachedSignatureCrypto,
    private val cipher: SealedMessageCipher,
    private val json: Json
) {
    /** The attestation that the number of [token] belongs to [routingId], as the verifier signed it. */
    suspend fun attest(
        token: String,
        routingId: String
    ): Result<String> =
        safeSuspendCall {
            val directoryUrl = checkNotNull(controlPlaneConfiguration.directoryUrl.value) { "No Control Plane Directory" }.trimEnd('/')
            val replyKey = cipher.newReplyKey()
            val request = json.encodeToString(PhoneAttestationRequest(token, routingId, BASE64.encode(replyKey)))
            val sealed = cipher.seal(request.encodeToByteArray(), requestKey(directoryUrl), REQUEST_CONTEXT).getOrThrow()
            val response = postSealed(directoryUrl, sealed)
            val attestation = cipher.openReply(BASE64.decode(response.sealedAttestation), replyKey)
            checkNotNull(attestation) { "Phone attestation does not open" }.decodeToString()
        }

    private suspend fun postSealed(
        directoryUrl: String,
        sealed: ByteArray
    ): SealedPhoneAttestation =
        try {
            controlPlaneHttpClient
                .post("$directoryUrl$ATTESTATIONS_PATH") {
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(SealedPhoneAttestationRequest(BASE64.encode(sealed))))
                }.body<SealedPhoneAttestation>()
        } catch (refusal: ClientRequestException) {
            throw refusal.takeUnless { it.response.status == HttpStatusCode.TooManyRequests }
                ?: PhoneVerificationLimitReachedException()
        }

    /** The verifier's X25519 request key, from the certificate the pinned Directory key signed. */
    private suspend fun requestKey(directoryUrl: String): ByteArray {
        val pinned = BASE64.decode(checkNotNull(pinnedDirectoryKey()) { "No pinned Directory key" })
        require(pinned.size == ED25519_DER_PREFIX.size + RAW_KEY_BYTES && pinned.copyOf(ED25519_DER_PREFIX.size).contentEquals(ED25519_DER_PREFIX)) {
            "Directory key must be X.509 DER Ed25519"
        }
        val certificate = json.decodeFromString<SignedPhoneKeys>(controlPlaneHttpClient.get("$directoryUrl$KEYS_PATH").bodyAsText())
        val payload = BASE64.decode(certificate.payload)
        signatureCrypto
            .verify(
                payload = PHONE_KEYS_DOMAIN.encodeToByteArray() + payload,
                signingPublicKey = pinned.copyOfRange(ED25519_DER_PREFIX.size, pinned.size),
                signature = BASE64.decode(certificate.signature)
            ).getOrThrow()
        val keys = json.decodeFromString<PhoneKeys>(payload.decodeToString())
        require(keys.protocol == PHONE_KEYS_PROTOCOL) { "Unsupported phone key certificate" }
        return BASE64.decode(keys.requestKey).also { key -> require(key.size == RAW_KEY_BYTES) { "Invalid phone request key" } }
    }

    private companion object {
        const val ATTESTATIONS_PATH = "/v1/phone-attestations"
        const val KEYS_PATH = "/v1/phone-attestations/keys"
        const val REQUEST_CONTEXT = "sparrow-phone-attestation-request-v1"
        const val PHONE_KEYS_PROTOCOL = "sparrow-phone-keys-v1"
        const val PHONE_KEYS_DOMAIN = "sparrow-phone-keys-v1\n"
        const val RAW_KEY_BYTES = 32
        val ED25519_DER_PREFIX = byteArrayOf(0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00)

        /** Unpadded base64url, like everything the Directory and its phone verifier send. */
        val BASE64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
    }
}

/** The phone verifier reached its daily maximum of verifications. */
internal class PhoneVerificationLimitReachedException : IllegalStateException("Daily limit of phone verifications reached")

/** Sealed for the verifier's request key; the keys must be exactly these (`phone_verifier.py`). */
@Serializable
internal class PhoneAttestationRequest(
    val token: String,
    val routingId: String,
    val replyKey: String
) {
    override fun toString(): String = "PhoneAttestationRequest(redacted)"
}

@Serializable
internal class SealedPhoneAttestationRequest(
    val sealedRequest: String
)

@Serializable
internal class SealedPhoneAttestation(
    val sealedAttestation: String
)

@Serializable
internal class SignedPhoneKeys(
    val payload: String,
    val signature: String
)

@Serializable
internal class PhoneKeys(
    val protocol: String,
    val attestationKey: String,
    val requestKey: String
)
