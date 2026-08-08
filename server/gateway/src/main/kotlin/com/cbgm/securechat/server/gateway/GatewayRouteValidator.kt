package com.cbgm.securechat.server.gateway

import com.cbgm.securechat.server.protocol.ClientRouteRegistration
import com.cbgm.securechat.server.security.ClientRoutingIds
import com.cbgm.securechat.server.security.ProtocolSignatures

internal class GatewayRouteValidator(
    private val maximumTtlMilliseconds: Long,
    private val now: () -> Long = System::currentTimeMillis
) {
    init {
        require(maximumTtlMilliseconds > 0L)
    }

    fun isValid(
        registration: ClientRouteRegistration,
        connectionRoutingId: String,
        connectionId: String,
        expectedNodeId: String
    ): Boolean {
        val route = registration.route
        val currentTime = now()
        val aliases = route.aliases.orEmpty()
        val routeMatchesConnection =
            route.routingId == connectionRoutingId &&
                route.connectionId == connectionId &&
                route.nodeId == expectedNodeId
        val routingIdentityMatches =
            ClientRoutingIds.matchesSigningPublicKey(
                route.routingId,
                registration.clientSigningPublicKey
            )
        val aliasesAreValid =
            aliases.all(ClientRoutingIds::isBootstrapRoutingId) &&
                aliases.distinct().size == aliases.size
        val expirationIsValid =
            route.expiresAtEpochMilliseconds > currentTime &&
                route.expiresAtEpochMilliseconds - currentTime <= maximumTtlMilliseconds
        val signatureIsValid =
            ProtocolSignatures.verifyClientRoute(
                route,
                registration.clientSigningPublicKey
            )

        return listOf(
            routeMatchesConnection,
            routingIdentityMatches,
            aliasesAreValid,
            expirationIsValid,
            signatureIsValid
        ).all { it }
    }
}
