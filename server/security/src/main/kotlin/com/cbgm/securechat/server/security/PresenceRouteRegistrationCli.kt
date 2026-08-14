package com.cbgm.securechat.server.security

import com.cbgm.securechat.server.protocol.ClientRoute
import com.cbgm.securechat.server.protocol.ClientRouteRegistration
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.protocol.unsigned
import java.nio.file.Path

object PresenceRouteRegistrationCli {
    @JvmStatic
    fun main(arguments: Array<String>) {
        require(arguments.size >= REQUIRED_ARGUMENT_COUNT) {
            "Usage: <node-identity-path> <route-node-id> <connection-id> " +
                "<generation> <expires-at-epoch-milliseconds>"
        }
        val nodeIdentity =
            NodeIdentityStore(Path.of(arguments[NODE_IDENTITY_PATH_INDEX])).loadOrCreate()
        val clientIdentity = NodeIdentity.generate()
        val routingId = ClientRoutingIds.fromSigningPublicKey(clientIdentity.encodedPublicKey)
        val unsignedRoute =
            ClientRoute(
                routingId = routingId,
                nodeId = arguments[ROUTE_NODE_ID_INDEX],
                connectionId = arguments[CONNECTION_ID_INDEX],
                generation = arguments[GENERATION_INDEX].toLong(),
                expiresAtEpochMilliseconds = arguments[EXPIRY_INDEX].toLong(),
                clientSignature = byteArrayOf()
            )
        val route = createSignedRoute(clientIdentity, unsignedRoute)
        val registration =
            ClientRouteRegistration(
                route = route,
                clientSigningPublicKey = clientIdentity.encodedPublicKey
            )
        val path = "/v1/routes/$routingId"
        val body = serverJson.encodeToString(registration)
        val authentication = NodeRequestSigner(nodeIdentity).sign("PUT", path, body)

        CommandLineOutput.write(
            listOf(
                "routingId=$routingId",
                "path=$path",
                "body=$body",
                "nodeId=${authentication.nodeId}",
                "timestamp=${authentication.timestampEpochMilliseconds}",
                "nonce=${authentication.nonce}",
                "signature=${authentication.signature}"
            )
        )
    }

    private fun createSignedRoute(
        clientIdentity: NodeIdentity,
        unsignedRoute: ClientRoute
    ): ClientRoute {
        val signature =
            Signatures.sign(
                serverJson.encodeToString(unsignedRoute.unsigned()).encodeToByteArray(),
                clientIdentity.privateKey
            )
        return unsignedRoute.copy(clientSignature = signature)
    }

    private const val REQUIRED_ARGUMENT_COUNT = 5
    private const val NODE_IDENTITY_PATH_INDEX = 0
    private const val ROUTE_NODE_ID_INDEX = 1
    private const val CONNECTION_ID_INDEX = 2
    private const val GENERATION_INDEX = 3
    private const val EXPIRY_INDEX = 4
}
