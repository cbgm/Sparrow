package com.cbgm.sparrow.server.security

import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.protocol.serverJson
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue

class SmokeTestCliTest {
    @Test
    fun theSmokeClientSignsTheGatewayChallengeWithItsRouteKey() {
        val identityPath = Files.createTempDirectory("smoke").resolve("node.identity")
        val route =
            output {
                PresenceRouteRegistrationCli.main(arrayOf(identityPath.toString(), "node-a", "connection-1", "1", "999999999999999"))
            }
        val registration = serverJson.decodeFromString<ClientRouteRegistration>(route.getValue("body"))
        val nonce = ByteArray(32) { index -> (index * 7 - 100).toByte() }
        val proof =
            output {
                GatewaySessionProofCli.main(
                    arrayOf(
                        route.getValue("clientPrivateKey"),
                        registration.route.routingId,
                        "connection-1",
                        "node-a",
                        serverJson.encodeToString(nonce)
                    )
                )
            }

        assertTrue(
            ProtocolSignatures.verifyGatewaySession(
                session = GatewaySessionBinding(registration.route.routingId, "connection-1", "node-a", nonce),
                clientPublicKey = registration.clientSigningPublicKey,
                clientSignature = serverJson.decodeFromString<ByteArray>(proof.getValue("clientSignature"))
            )
        )
    }

    private fun output(block: () -> Unit): Map<String, String> {
        val captured = ByteArrayOutputStream()
        val previous = System.out
        System.setOut(PrintStream(captured))
        try {
            block()
        } finally {
            System.setOut(previous)
        }
        return captured.toString().lines().filter { '=' in it }.associate { it.substringBefore('=') to it.substringAfter('=') }
    }
}
