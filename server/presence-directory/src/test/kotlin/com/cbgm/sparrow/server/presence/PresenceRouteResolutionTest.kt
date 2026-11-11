package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.AliasClaimKeyResponse
import com.cbgm.sparrow.server.protocol.AliasClaimRequest
import com.cbgm.sparrow.server.protocol.AliasClaimResult
import com.cbgm.sparrow.server.protocol.ClientRoute
import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.protocol.ClientRoutingResult
import com.cbgm.sparrow.server.protocol.InvitationRouteLookup
import com.cbgm.sparrow.server.protocol.InvitationRouteResult
import com.cbgm.sparrow.server.protocol.MailboxCreationWork
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.protocol.unsigned
import com.cbgm.sparrow.server.security.ClientRoutingIds
import com.cbgm.sparrow.server.security.CreationWorkCheck
import com.cbgm.sparrow.server.security.InternalApiAuthentication
import com.cbgm.sparrow.server.security.NodeIdentity
import com.cbgm.sparrow.server.security.NodeRequestAuthorizer
import com.cbgm.sparrow.server.security.Signatures
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
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

    @Test
    fun appsClaimTheirAliasesWithoutANode() =
        withPresence { routingId ->
            val sealed =
                AliasClaimKeysTest.sealClaim(
                    device = device,
                    presenceKey = aliasClaimKey,
                    issuedAt = System.currentTimeMillis(),
                    aliases = listOf(PHONE_ALIAS)
                )
            val response =
                client.post("/v1/alias-claims") {
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(AliasClaimRequest(sealed)))
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(listOf(0), serverJson.decodeFromString<AliasClaimResult>(response.bodyAsText()).acceptedAliasIndexes)
            val resolved =
                client.get("/v1/routes/$PHONE_ALIAS") { header(InternalApiAuthentication.TOKEN_HEADER, TOKEN) }
            val result = serverJson.decodeFromString<ClientRoutingResult>(resolved.bodyAsText())
            assertEquals(routingId, result.ownerRoutingId)
            assertEquals(listOf(routingId), result.routes.map(ClientRoute::routingId))
        }

    @Test
    fun aPaidLookupFindsTheInvitationRouteOfAnAlias() =
        withPresence { routingId ->
            val invitation = AliasClaimKeysTest.invitationRoute(expiresAt = System.currentTimeMillis() + 60_000L)
            client.post("/v1/alias-claims") {
                contentType(ContentType.Application.Json)
                val sealed =
                    AliasClaimKeysTest.sealClaim(
                        device = device,
                        presenceKey = aliasClaimKey,
                        issuedAt = System.currentTimeMillis(),
                        aliases = listOf(PHONE_ALIAS),
                        invitationRoute = invitation
                    )
                setBody(serverJson.encodeToString(AliasClaimRequest(sealed)))
            }

            assertEquals(HttpStatusCode.Forbidden, lookup(PHONE_ALIAS, MailboxCreationWork("unpaid-stamp-0000", System.currentTimeMillis(), 0L)).status)
            val response = lookup(PHONE_ALIAS, paidWork())
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(InvitationRouteResult(routingId, invitation), serverJson.decodeFromString(response.bodyAsText()))
            assertEquals(InvitationRouteResult(routingId, invitation), serverJson.decodeFromString(lookup(routingId, paidWork()).bodyAsText()))
            assertEquals(HttpStatusCode.NotFound, lookup(ClientRoutingIds.fromPhoneNumber("+491709999999"), paidWork()).status)
            assertEquals(HttpStatusCode.BadRequest, lookup("not-a-routing-id", paidWork()).status)
        }

    private suspend fun PresenceTestScope.lookup(
        alias: String,
        work: MailboxCreationWork
    ) = client.post("/v1/invitation-routes") {
        contentType(ContentType.Application.Json)
        setBody(serverJson.encodeToString(InvitationRouteLookup(alias, work)))
    }

    @Test
    fun aClaimSealedForAnotherKeyIsRejected() =
        withPresence {
            val response =
                client.post("/v1/alias-claims") {
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(AliasClaimRequest(ByteArray(80))))
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    private fun withPresence(block: suspend PresenceTestScope.(String) -> Unit) =
        testApplication {
            val identity = NodeIdentity.generate()
            val store = PresenceStore()
            val registration = registration(identity)
            store.register(registration)
            val runtime =
                PresenceRuntime(
                    store = store,
                    httpClient = HttpClient(),
                    registryUrl = "http://localhost",
                    nodeRequestAuthorizer = NodeRequestAuthorizer({ null }),
                    phoneVerification = PhoneVerification(PhoneAttestations(directoryPublicKey = null), testAliases = setOf(PHONE_ALIAS)),
                    internalApiToken = TOKEN,
                    lookupWork = CreationWorkCheck(WORK_BITS)
                )
            application {
                installPresencePlugins(runtime)
                installPresenceRoutes(runtime)
            }
            PresenceTestScope(client, identity, runtime.aliasClaimKeys.publicKey).block(registration.route.routingId)
        }

    private class PresenceTestScope(
        val client: HttpClient,
        val device: NodeIdentity,
        val aliasClaimKey: ByteArray
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
        const val WORK_BITS = 8

        /** Solves the lookup proof of work the way the app does. */
        fun paidWork(): MailboxCreationWork {
            val stamp = java.util.UUID.randomUUID().toString()
            val createdAt = System.currentTimeMillis()
            var nonce = 0L
            while (leadingZeroBits("sparrow-mailbox-work-v1|$stamp|$createdAt|$nonce") < WORK_BITS) nonce++
            return MailboxCreationWork(stamp, createdAt, nonce)
        }

        private fun leadingZeroBits(value: String): Int {
            val digest = java.security.MessageDigest.getInstance("SHA-256").digest(value.encodeToByteArray())
            var bits = 0
            for (byte in digest) {
                val unsigned = byte.toInt() and 0xFF
                if (unsigned != 0) return bits + Integer.numberOfLeadingZeros(unsigned) - 24
                bits += 8
            }
            return bits
        }

        val PHONE_ALIAS = ClientRoutingIds.fromPhoneNumber("+491701234567")
    }
}
