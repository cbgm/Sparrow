package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.transport.push.PushDeviceRegistrationRequest
import com.cbgm.sparrow.feature.transport.push.PushPlatform
import com.cbgm.sparrow.feature.transport.push.pushDeviceRegistrationPayload
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider

/**
 * Signs a push token registration with the identity signing key the routing ID is derived from. The
 * issue time lets the server refuse old proofs; a fresh proof is made for every registration and is
 * never stored.
 */
class CreatePushDeviceRegistrationUseCase(
    private val signingKeyPairProvider: LocalSigningKeyPairProvider,
    private val signatureCrypto: DetachedSignatureCrypto
) {
    suspend operator fun invoke(
        routingId: String,
        token: String,
        platform: PushPlatform,
        issuedAtEpochMilliseconds: Long = SystemClock.nowEpochMilliseconds()
    ): Result<PushDeviceRegistrationRequest> =
        safeSuspendCall(logFailure = false) {
            require(routingId.isNotBlank()) { "Routing ID must not be blank" }
            require(token.isNotBlank()) { "Push token must not be blank" }
            val signingKeyPair = signingKeyPairProvider.getSigningKeyPair().getOrThrow()
            val payload =
                pushDeviceRegistrationPayload(
                    routingId = routingId,
                    token = token,
                    platform = platform.name,
                    issuedAtEpochMilliseconds = issuedAtEpochMilliseconds
                )
            PushDeviceRegistrationRequest(
                routingId = routingId,
                token = token,
                platform = platform.name,
                issuedAtEpochMilliseconds = issuedAtEpochMilliseconds,
                clientSigningPublicKey = signingKeyPair.publicKey,
                clientSignature =
                    signatureCrypto
                        .sign(payload = payload, signingPrivateKey = signingKeyPair.privateKey)
                        .getOrThrow()
            )
        }
}
