package com.cbgm.sparrow.feature.transport.data.controlplane

import com.cbgm.sparrow.core.crypto.sealing.SealedMessageCipher
import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.transport.data.mailbox.MailboxCreationWorkSolver
import com.cbgm.sparrow.feature.transport.data.route.relay.RelayDirectoryLoader
import com.cbgm.sparrow.protocol.phone.PhoneVerificationSlot
import com.cbgm.sparrow.protocol.phone.PhoneVerificationSlotSource
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
 *
 * Before the SMS is sent, [reserve] buys a sign-up slot there with a small proof of work, the same way and as sealed;
 * the attestation then uses it up. During onboarding no node directory may be known yet: it is loaded first, so the slot
 * request goes through the relays too instead of showing the verifier the app's address.
 */
internal class PhoneAttestationRemoteDataSource(
    private val controlPlaneHttpClient: HttpClient,
    private val controlPlaneConfiguration: ControlPlaneLocalDataSource,
    /** The pinned Directory key, base64url X.509 DER Ed25519. */
    private val pinnedDirectoryKey: suspend () -> String?,
    private val signatureCrypto: DetachedSignatureCrypto,
    private val cipher: SealedMessageCipher,
    private val json: Json,
    private val work: MailboxCreationWorkSolver,
    private val relays: RelayDirectoryLoader
) : PhoneVerificationSlotSource {
    /** The attestation that the number of [token] belongs to [routingId], as the verifier signed it; uses up [slot]. */
    suspend fun attest(
        token: String,
        slot: String,
        routingId: String
    ): Result<String> =
        safeSuspendCall {
            val directoryUrl = checkNotNull(controlPlaneConfiguration.directoryUrl.value) { "No Control Plane Directory" }.trimEnd('/')
            val replyKey = cipher.newReplyKey()
            val request = json.encodeToString(PhoneAttestationRequestDto(token, routingId, BASE64.encode(replyKey), slot))
            val sealed = cipher.seal(request.encodeToByteArray(), requestKey(directoryUrl), REQUEST_CONTEXT).getOrThrow()
            val response = postSealed(directoryUrl, sealed)
            val attestation = cipher.openReply(BASE64.decode(response.sealedAttestation), replyKey)
            checkNotNull(attestation) { "Phone attestation does not open" }.decodeToString()
        }

    override suspend fun reserve(): PhoneVerificationSlot {
        val directoryUrl = controlPlaneConfiguration.directoryUrl.value?.trimEnd('/') ?: return PhoneVerificationSlot.NotOffered
        relays.ensureRelays()
        val proof = work.solve()
        val replyKey = cipher.newReplyKey()
        val request =
            PhoneSlotRequestDto(
                stamp = proof.stamp,
                createdAtEpochMilliseconds = proof.createdAtEpochMilliseconds,
                nonce = proof.nonce,
                replyKey = BASE64.encode(replyKey)
            )
        val key =
            try {
                requestKey(directoryUrl)
            } catch (refusal: ClientRequestException) {
                // No phone verifier behind this Directory.
                if (refusal.response.status == HttpStatusCode.NotFound) return PhoneVerificationSlot.NotOffered
                throw refusal
            }
        val sealed = cipher.seal(json.encodeToString(request).encodeToByteArray(), key, SLOT_CONTEXT).getOrThrow()
        return try {
            val response =
                controlPlaneHttpClient
                    .post("$directoryUrl$SLOTS_PATH") {
                        contentType(ContentType.Application.Json)
                        setBody(json.encodeToString(SealedPhoneAttestationRequestDto(BASE64.encode(sealed))))
                    }.body<SealedPhoneSlotDto>()
            val reply = checkNotNull(cipher.openReply(BASE64.decode(response.sealedSlot), replyKey)) { "Phone slot does not open" }
            json.decodeFromString<PhoneSlotDto>(reply.decodeToString()).let { slot ->
                PhoneVerificationSlot.Reserved(slot.slot, slot.expiresAtEpochMilliseconds, slot.resends)
            }
        } catch (refusal: ClientRequestException) {
            if (refusal.response.status != HttpStatusCode.TooManyRequests) throw refusal
            PhoneVerificationSlot.DayFull(json.decodeFromString<PhoneLimitDto>(refusal.response.bodyAsText()).retryAtEpochMilliseconds)
        }
    }

    private suspend fun postSealed(
        directoryUrl: String,
        sealed: ByteArray
    ): SealedPhoneAttestationDto =
        try {
            controlPlaneHttpClient
                .post("$directoryUrl$ATTESTATIONS_PATH") {
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(SealedPhoneAttestationRequestDto(BASE64.encode(sealed))))
                }.body<SealedPhoneAttestationDto>()
        } catch (refusal: ClientRequestException) {
            throw refusal.takeUnless { it.response.status == HttpStatusCode.TooManyRequests }
                ?: PhoneVerificationLimitReachedException()
        }

    /** The verifier's X25519 request key, from the certificate the pinned Directory key signed. */
    private suspend fun requestKey(directoryUrl: String): ByteArray {
        val pinned = BASE64.decode(checkNotNull(pinnedDirectoryKey()) { "No pinned Directory key" })
        require(pinned.isEd25519Der()) { "Directory key must be X.509 DER Ed25519" }
        val certificate = json.decodeFromString<SignedPhoneKeysDto>(controlPlaneHttpClient.get("$directoryUrl$KEYS_PATH").bodyAsText())
        val payload = BASE64.decode(certificate.payload)
        signatureCrypto
            .verify(
                payload = PHONE_KEYS_DOMAIN.encodeToByteArray() + payload,
                signingPublicKey = pinned.copyOfRange(ED25519_DER_PREFIX.size, pinned.size),
                signature = BASE64.decode(certificate.signature)
            ).getOrThrow()
        val keys = json.decodeFromString<PhoneKeysDto>(payload.decodeToString())
        require(keys.protocol == PHONE_KEYS_PROTOCOL && BASE64.decode(keys.attestationKey).isEd25519Der()) {
            "Unsupported phone key certificate"
        }
        return BASE64.decode(keys.requestKey).also { key -> require(key.size == RAW_KEY_BYTES) { "Invalid phone request key" } }
    }

    private fun ByteArray.isEd25519Der(): Boolean =
        size == ED25519_DER_PREFIX.size + RAW_KEY_BYTES && copyOf(ED25519_DER_PREFIX.size).contentEquals(ED25519_DER_PREFIX)

    private companion object {
        const val ATTESTATIONS_PATH = "/v1/phone-attestations"
        const val KEYS_PATH = "/v1/phone-attestations/keys"
        const val SLOTS_PATH = "/v1/phone-attestations/slots"
        const val REQUEST_CONTEXT = "sparrow-phone-attestation-request-v1"
        const val SLOT_CONTEXT = "sparrow-phone-slot-request-v1"
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
internal class PhoneAttestationRequestDto(
    val token: String,
    val routingId: String,
    val replyKey: String,
    val slot: String
) {
    override fun toString(): String = "PhoneAttestationRequestDto(redacted)"
}

/** Sealed for the verifier's request key, under its slot context (`phone_verifier.py`). */
@Serializable
internal class PhoneSlotRequestDto(
    val stamp: String,
    val createdAtEpochMilliseconds: Long,
    val nonce: Long,
    val replyKey: String
)

@Serializable
internal class SealedPhoneSlotDto(
    val sealedSlot: String
)

@Serializable
internal class PhoneSlotDto(
    val slot: String,
    val expiresAtEpochMilliseconds: Long,
    val resends: Int
) {
    override fun toString(): String = "PhoneSlotDto(redacted)"
}

/** The answer when today is full: when the next day starts. */
@Serializable
internal class PhoneLimitDto(
    val error: String,
    val retryAtEpochMilliseconds: Long
)

@Serializable
internal class SealedPhoneAttestationRequestDto(
    val sealedRequest: String
)

@Serializable
internal class SealedPhoneAttestationDto(
    val sealedAttestation: String
)

@Serializable
internal class SignedPhoneKeysDto(
    val payload: String,
    val signature: String
)

@Serializable
internal class PhoneKeysDto(
    val protocol: String,
    val attestationKey: String,
    val requestKey: String
)
