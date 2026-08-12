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
    ): Boolean =
        validationFailure(
            registration = registration,
            connectionRoutingId = connectionRoutingId,
            connectionId = connectionId,
            expectedNodeId = expectedNodeId
        ) == null

    fun validationFailure(
        registration: ClientRouteRegistration,
        connectionRoutingId: String,
        connectionId: String,
        expectedNodeId: String
    ): GatewayRouteValidationFailure? {
        val route = registration.route
        val currentTime = now()
        val aliases = route.aliases.orEmpty()

        return when {
            route.routingId != connectionRoutingId ||
                route.connectionId != connectionId ||
                route.nodeId != expectedNodeId -> GatewayRouteValidationFailure.ROUTE_BINDING

            !ClientRoutingIds.matchesSigningPublicKey(
                route.routingId,
                registration.clientSigningPublicKey
            ) -> GatewayRouteValidationFailure.ROUTING_IDENTITY

            aliases.any { alias -> !ClientRoutingIds.isBootstrapRoutingId(alias) } ||
                aliases.distinct().size != aliases.size -> GatewayRouteValidationFailure.ALIASES

            route.expiresAtEpochMilliseconds <= currentTime ||
                route.expiresAtEpochMilliseconds - currentTime > maximumTtlMilliseconds ->
                GatewayRouteValidationFailure.EXPIRATION

            !ProtocolSignatures.verifyClientRoute(
                route,
                registration.clientSigningPublicKey
            ) -> GatewayRouteValidationFailure.SIGNATURE

            else -> null
        }
    }
}

internal enum class GatewayRouteValidationFailure {
    ROUTE_BINDING,
    ROUTING_IDENTITY,
    ALIASES,
    EXPIRATION,
    SIGNATURE
}
