package com.cbgm.sparrow.feature.transport.presence

import com.cbgm.sparrow.core.crypto.sealing.SealedMessageCipher
import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.feature.transport.ControlPlaneEndpoint
import com.cbgm.sparrow.feature.transport.config.TransportConfig
import com.cbgm.sparrow.feature.transport.discovery.NodeDirectoryVerifier
import com.cbgm.sparrow.feature.transport.discovery.SignedNodeDirectory
import com.cbgm.sparrow.feature.transport.relay.ControlPlaneRelayPolicy
import com.cbgm.sparrow.feature.transport.routing.LocalBootstrapRoutingIdProvider
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider
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
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/**
 * Claims the device's phone alias at every Control Plane the user has (from the directory or added by hand), through
 * the relays of the Control Plane HTTP client ([com.cbgm.sparrow.feature.transport.relay.OnionRelayEngine]). For each
 * one it takes the presence directory's key from that Control Plane's own signed node directory, checked against its
 * pinned root, so neither a relay nor the gateway can swap in a key. A Control Plane whose root is not pinned yet is
 * skipped until the app has used it.
 */
internal class AliasClaimRegistrar(
    private val controlPlaneHttpClient: HttpClient,
    private val controlPlaneConfiguration: ControlPlaneConfiguration,
    private val relayPolicy: ControlPlaneRelayPolicy,
    private val directoryVerifier: NodeDirectoryVerifier,
    private val transportConfig: TransportConfig,
    private val signingKeyPairProvider: LocalSigningKeyPairProvider,
    private val signatureCrypto: DetachedSignatureCrypto,
    private val cipher: SealedMessageCipher,
    private val localBootstrapRoutingIdProvider: LocalBootstrapRoutingIdProvider,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val json: Json,
    private val now: () -> Long = SystemClock::nowEpochMilliseconds
) {
    private val logger = SparrowLog.withTag("AliasClaimRegistrar")

    /** Returns the aliases at least one Control Plane bound to [routingId]. */
    suspend fun claim(routingId: String): Set<String> {
        val alias = localBootstrapRoutingIdProvider.getLocalBootstrapRoutingId().getOrNull() ?: return emptySet()
        val token = localPhoneNumberProvider.getPhoneVerificationToken().getOrNull()
        val signingKeyPair = signingKeyPairProvider.getSigningKeyPair().getOrThrow()
        val results =
            controlPlaneConfiguration.endpoints.value.distinctBy(ControlPlaneEndpoint::baseUrl).map { endpoint ->
                claimAt(endpoint.baseUrl, routingId, listOf(alias), token, signingKeyPair)
            }
        // The token proves the number once; it is not sent again after a Control Plane answered it.
        if (token != null && results.any { result -> result != ClaimOutcome.Unreachable }) {
            localPhoneNumberProvider.clearPhoneVerificationToken()
        }
        return results.filterIsInstance<ClaimOutcome.Bound>().flatMap(ClaimOutcome.Bound::aliases).toSet()
    }

    private suspend fun claimAt(
        baseUrl: String,
        routingId: String,
        aliases: List<String>,
        token: String?,
        signingKeyPair: LocalSigningKeyPair
    ): ClaimOutcome =
        try {
            val key = aliasClaimKey(baseUrl)
            if (key == null) {
                ClaimOutcome.Unreachable
            } else {
                val sealed = sealedClaim(routingId, aliases, token, signingKeyPair, key)
                val result =
                    controlPlaneHttpClient
                        .post("${baseUrl.trimEnd('/')}/v1/alias-claims") {
                            contentType(ContentType.Application.Json)
                            setBody(json.encodeToString(AliasClaimRequest(sealed)))
                        }.body<AliasClaimResult>()
                ClaimOutcome.Bound(result.acceptedAliasIndexes.mapNotNull(aliases::getOrNull))
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (rejection: ClientRequestException) {
            logger.warn { "Alias claim refused with ${rejection.response.status.value}" }
            ClaimOutcome.Refused.takeIf { rejection.response.status == HttpStatusCode.Forbidden }
                ?: ClaimOutcome.Unreachable
        } catch (failure: Exception) {
            logger.warn { "Alias claim failed: ${failure::class.simpleName}" }
            ClaimOutcome.Unreachable
        }

    private suspend fun aliasClaimKey(baseUrl: String): ByteArray? {
        val root = relayPolicy.trustedRoot(baseUrl) ?: return null
        val directory =
            json.decodeFromString<SignedNodeDirectory>(
                controlPlaneHttpClient.get("${baseUrl.trimEnd('/')}/v1/nodes").bodyAsText()
            )
        directoryVerifier
            .verify(
                signedDirectory = directory,
                trustedRootNodeId = root,
                supportedProtocolVersion = transportConfig.supportedProtocolVersion,
                nowEpochMilliseconds = now()
            ).getOrThrow()
        return directory.directory.aliasClaimKey
    }

    private suspend fun sealedClaim(
        routingId: String,
        aliases: List<String>,
        token: String?,
        signingKeyPair: LocalSigningKeyPair,
        presenceKey: ByteArray
    ): ByteArray {
        val unsigned =
            UnsignedAliasClaim(
                routingId = routingId,
                clientSigningPublicKey = signingKeyPair.publicKey,
                aliases = aliases,
                phoneVerificationToken = token,
                issuedAtEpochMilliseconds = now()
            )
        val signature =
            signatureCrypto
                .sign(json.encodeToString(unsigned).encodeToByteArray(), signingKeyPair.privateKey)
                .getOrThrow()
        val claim =
            AliasClaim(
                routingId = unsigned.routingId,
                clientSigningPublicKey = unsigned.clientSigningPublicKey,
                aliases = unsigned.aliases,
                phoneVerificationToken = unsigned.phoneVerificationToken,
                issuedAtEpochMilliseconds = unsigned.issuedAtEpochMilliseconds,
                signature = signature
            )
        return cipher.seal(json.encodeToString(claim).encodeToByteArray(), presenceKey, ALIAS_CLAIM_CONTEXT).getOrThrow()
    }

    private sealed interface ClaimOutcome {
        class Bound(
            val aliases: List<String>
        ) : ClaimOutcome

        data object Refused : ClaimOutcome

        data object Unreachable : ClaimOutcome
    }
}
