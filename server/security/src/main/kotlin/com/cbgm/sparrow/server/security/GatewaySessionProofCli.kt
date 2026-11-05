package com.cbgm.sparrow.server.security

import com.cbgm.sparrow.server.protocol.serverJson
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

/**
 * Smoke tests: signs a gateway's sign-in challenge like the app does, with the throwaway client key from
 * [PresenceRouteRegistrationCli]. The nonce and the signature are JSON byte arrays, as in the WebSocket frames.
 */
object GatewaySessionProofCli {
    @JvmStatic
    fun main(arguments: Array<String>) {
        require(arguments.size == ARGUMENT_COUNT) {
            "Usage: <client-private-key-base64> <routing-id> <connection-id> <node-id> <nonce-json-array>"
        }
        val privateKey =
            KeyFactory.getInstance("Ed25519").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(arguments[0])))
        val session =
            GatewaySessionBinding(
                routingId = arguments[1],
                connectionId = arguments[2],
                nodeId = arguments[3],
                nonce = serverJson.decodeFromString<ByteArray>(arguments[4])
            )
        val signature = Signatures.sign(ProtocolSignatures.gatewaySessionContent(session), privateKey)

        CommandLineOutput.write(listOf("clientSignature=${serverJson.encodeToString(signature)}"))
    }

    private const val ARGUMENT_COUNT = 5
}
