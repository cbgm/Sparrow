package com.cbgm.sparrow.server.push

import com.cbgm.sparrow.server.protocol.PushDeviceRegistrationRequest
import com.cbgm.sparrow.server.security.ClientRoutingIds
import com.cbgm.sparrow.server.security.ProtocolSignatures
import com.cbgm.sparrow.server.security.PushDeviceBinding
import io.ktor.http.HttpStatusCode
import kotlin.math.abs

/**
 * Decides whether a push device registration proves ownership of its routing ID. Without this check
 * anyone could bind their own token to another user's routing ID, receive that user's wake-up IDs and
 * read and acknowledge (delete) the user's pending envelopes.
 *
 * Only device routing IDs derived from the signing key are accepted; phone aliases are never bound to a
 * push token because the app registers its device routing ID only.
 */
internal object PushDeviceRegistrationValidator {
    fun validationFailure(
        request: PushDeviceRegistrationRequest,
        nowEpochMilliseconds: Long
    ): PushDeviceRegistrationFailure? =
        when {
            !request.hasRequiredFields() -> PushDeviceRegistrationFailure.INVALID_REQUEST

            ClientRoutingIds.isBootstrapRoutingId(request.routingId) ->
                PushDeviceRegistrationFailure.BOOTSTRAP_ROUTING_ID

            !ClientRoutingIds.matchesSigningPublicKey(request.routingId, request.clientSigningPublicKey) ->
                PushDeviceRegistrationFailure.ROUTING_IDENTITY

            abs(nowEpochMilliseconds - request.issuedAtEpochMilliseconds) > MAXIMUM_PROOF_CLOCK_SKEW_MILLISECONDS ->
                PushDeviceRegistrationFailure.STALE_PROOF

            !ProtocolSignatures.verifyPushDevice(
                device =
                    PushDeviceBinding(
                        routingId = request.routingId,
                        token = request.token,
                        platform = request.platform,
                        issuedAtEpochMilliseconds = request.issuedAtEpochMilliseconds
                    ),
                clientPublicKey = request.clientSigningPublicKey,
                clientSignature = request.clientSignature
            ) -> PushDeviceRegistrationFailure.SIGNATURE

            else -> null
        }

    private fun PushDeviceRegistrationRequest.hasRequiredFields(): Boolean =
        listOf(routingId, token, platform).all(String::isNotBlank)

    /** A captured proof only re-binds the same token to the same routing ID, so a few minutes are enough. */
    const val MAXIMUM_PROOF_CLOCK_SKEW_MILLISECONDS = 5L * 60L * 1_000L
}

internal enum class PushDeviceRegistrationFailure(
    val status: HttpStatusCode,
    val code: String,
    val message: String
) {
    INVALID_REQUEST(HttpStatusCode.BadRequest, "INVALID_PUSH_DEVICE", "Routing ID, token and platform are required"),
    BOOTSTRAP_ROUTING_ID(HttpStatusCode.Forbidden, "PUSH_ROUTING_ID_NOT_ALLOWED", "Aliases cannot receive push"),
    ROUTING_IDENTITY(HttpStatusCode.Forbidden, "PUSH_ROUTING_ID_MISMATCH", "Routing ID does not match the key"),
    STALE_PROOF(HttpStatusCode.Forbidden, "PUSH_PROOF_STALE", "Proof issue time is outside the allowed window"),
    SIGNATURE(HttpStatusCode.Forbidden, "PUSH_PROOF_INVALID", "Proof signature is invalid")
}
