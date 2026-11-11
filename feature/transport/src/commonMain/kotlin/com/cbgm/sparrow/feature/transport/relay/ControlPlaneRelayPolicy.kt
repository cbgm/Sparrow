package com.cbgm.sparrow.feature.transport.relay

import com.cbgm.sparrow.feature.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.feature.transport.config.TransportConfig
import com.cbgm.sparrow.feature.transport.discovery.NodeDirectoryCache
import io.ktor.http.Url

/** Which of the app's Control Plane requests must, may or must not go through the relays ([OnionRelayEngine]). */
internal class ControlPlaneRelayPolicy(
    private val transportConfig: TransportConfig,
    private val controlPlaneConfiguration: ControlPlaneConfiguration,
    private val cache: NodeDirectoryCache
) {
    suspend fun requirement(url: Url): RelayRequirement {
        val path = url.encodedPath
        return when {
            // A phone alias or phone token with the app's address is exactly what the relays keep apart.
            path.endsWith(ALIAS_CLAIMS_PATH) || path.startsWith(PHONE_ATTESTATIONS_PATH) -> RelayRequirement.ROUTE_ONLY
            path.endsWith(NODES_PATH) && trustedRoot(url.toString().substringBefore(NODES_PATH)) == null ->
                RelayRequirement.DIRECT

            else -> RelayRequirement.ROUTE_PREFERRED
        }
    }

    /** The registry root the node directory of [baseUrl] must be signed under, or null before it is pinned. */
    suspend fun trustedRoot(baseUrl: String): String? =
        transportConfig.trustedRegistryRootNodeId
            ?: controlPlaneConfiguration.verifiedDirectoryRootId(baseUrl)
            ?: cache.read()?.let { cached ->
                cached.trustedRootFor(baseUrl)
                    ?: cached.trustedRootNodeId.takeIf { cached.sourceControlPlaneBaseUrl == baseUrl }
            }

    private companion object {
        const val NODES_PATH = "/v1/nodes"
        const val ALIAS_CLAIMS_PATH = "/v1/alias-claims"
        const val PHONE_ATTESTATIONS_PATH = "/v1/phone-attestations"
    }
}
