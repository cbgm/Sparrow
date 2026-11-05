package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.AliasClaimKeyResponse
import com.cbgm.sparrow.server.protocol.ClientRoute
import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.protocol.ClientRoutingResult
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.protocol.unsigned
import com.cbgm.sparrow.server.security.ClientRoutingIds
import com.cbgm.sparrow.server.security.InternalApiAuthentication
import com.cbgm.sparrow.server.security.NodeIdentity
import com.cbgm.sparrow.server.security.NodeRequestAuthorizer
import com.cbgm.sparrow.server.security.Signatures
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PresenceRouteResolutionTest {
    @Test
    fun lookupWithoutNodeSignatureOrInternalTokenIsRefused() =
        withPresence { routingId ->
            assertEquals(HttpStatusCode.Unauthorized, client.get("/v1/routes/$routingId").status)
            assertEquals(
                HttpStatusCode.Unauthorized,
                client.get("/v1/routes/$routingId") { header(InternalApiAuthentication.TOKEN_HEADER, "wrong") }.status
            )
        }

    @Test
    fun internalTokenResolvesRoutesWithoutPhoneAliases() =
        withPresence { routingId ->
            val response = client.get("/v1/routes/$routingId") { header(InternalApiAuthentication.TOKEN_HEADER, TOKEN) }

            assertEquals(HttpStatusCode.OK, response.status)
            val result = serverJson.decodeFromString<ClientRoutingResult>(response.bodyAsText())
            assertEquals(1, result.routes.size)
            assertNull(result.routes.single().aliases)
        }

    @Test
    fun aliasClaimKeyIsPublishedForTheNodeRegistry() =
        withPresence {
            val response = client.get("/v1/alias-claim-key")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(32, serverJson.decodeFromString<AliasClaimKeyResponse>(response.bodyAsText()).publicKey.size)
        }

    private fun withPresence(block: suspend PresenceTestScope.(String) -> Unit) =
        testApplication {
            val identity = NodeIdentity.generate()
            val store = PresenceStore()
            val registration = registration(identity)
            store.register(
                registration,
                AliasClaims(provenAlias = null, unprovenClaimsAllowed = true, aliases = listOf(PHONE_ALIAS))
            )
            val runtime =
                PresenceRuntime(
                    store = store,
                    httpClient = HttpClient(),
                    registryUrl = "http://localhost",
                    nodeRequestAuthorizer = NodeRequestAuthorizer({ null }),
                    phoneVerification = PhoneVerification(verifier = null),
                    internalApiToken = TOKEN
                )
            application {
                installPresencePlugins(runtime)
                installPresenceRoutes(runtime)
            }
            PresenceTestScope(client).block(registration.route.routingId)
        }

    private class PresenceTestScope(
        val client: HttpClient
    )

    private fun registration(identity: NodeIdentity): ClientRouteRegistration {
        val unsigned =
            ClientRoute(
                routingId = ClientRoutingIds.fromSigningPublicKey(identity.encodedPublicKey),
                nodeId = "node-a",
                connectionId = "connection-1",
                generation = 1L,
                expiresAtEpochMilliseconds = System.currentTimeMillis() + 60_000L,
                clientSignature = byteArrayOf()
            )
        val signature =
            Signatures.sign(
                serverJson.encodeToString(unsigned.unsigned()).encodeToByteArray(),
                identity.privateKey
            )
        return ClientRouteRegistration(unsigned.copy(clientSignature = signature), identity.encodedPublicKey)
    }

    private companion object {
        const val TOKEN = "presence-test-token"
        val PHONE_ALIAS = ClientRoutingIds.fromPhoneNumber("+491701234567")
    }
}
