package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.AliasClaimKeyResponse
import com.cbgm.sparrow.server.protocol.AliasClaimRequest
import com.cbgm.sparrow.server.protocol.AliasClaimResult
import com.cbgm.sparrow.server.protocol.InvitationRouteLookup
import com.cbgm.sparrow.server.protocol.InvitationRouteResult
import com.cbgm.sparrow.server.protocol.MailboxCreationWork
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.security.ClientRoutingIds
import com.cbgm.sparrow.server.security.CreationWorkCheck
import com.cbgm.sparrow.server.security.NodeIdentity
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class PresenceRoutesTest {
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
            assertEquals(routingId, store.aliasOwner(PHONE_ALIAS))
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
            val runtime =
                PresenceRuntime(
                    store = store,
                    phoneVerification = PhoneVerification(PhoneAttestations(directoryPublicKey = null), testAliases = setOf(PHONE_ALIAS)),
                    lookupWork = CreationWorkCheck(WORK_BITS)
                )
            application {
                installPresencePlugins()
                installPresenceRoutes(runtime)
            }
            PresenceTestScope(client, identity, runtime.aliasClaimKeys.publicKey, store)
                .block(ClientRoutingIds.fromSigningPublicKey(identity.encodedPublicKey))
        }

    private class PresenceTestScope(
        val client: HttpClient,
        val device: NodeIdentity,
        val aliasClaimKey: ByteArray,
        val store: PresenceStorage
    )

    private companion object {
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
