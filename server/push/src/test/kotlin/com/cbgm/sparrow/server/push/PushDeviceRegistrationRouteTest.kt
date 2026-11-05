package com.cbgm.sparrow.server.push

import com.cbgm.sparrow.server.protocol.PushDeviceRegistrationRequest
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.security.ClientRoutingIds
import com.cbgm.sparrow.server.security.NodeIdentity
import com.cbgm.sparrow.server.security.ProtocolSignatures
import com.cbgm.sparrow.server.security.PushDeviceBinding
import com.cbgm.sparrow.server.security.Signatures
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PushDeviceRegistrationRouteTest {
    private val owner = NodeIdentity.generate()
    private val ownerPublicKey = owner.encodedPublicKey.takeLast(RAW_KEY_BYTES).toByteArray()
    private val routingId = ClientRoutingIds.fromSigningPublicKey(ownerPublicKey)

    @Test
    fun validProofRegistersTheDevice() =
        pushApplication {
            val response = client.register(signedRequest())

            assertEquals(HttpStatusCode.NoContent, response.status)
            assertTrue(client.health().contains("devices=1"))
        }

    @Test
    fun unsignedRegistrationIsRejected() =
        pushApplication {
            val response = client.register(PushDeviceRegistrationRequest(routingId, TOKEN, PLATFORM))

            assertRejected(response, HttpStatusCode.Forbidden, "PUSH_ROUTING_ID_MISMATCH")
            assertTrue(client.health().contains("devices=0"))
        }

    @Test
    fun keyOfAnotherRoutingIdIsRejected() =
        pushApplication {
            val attacker = NodeIdentity.generate()
            val attackerKey = attacker.encodedPublicKey.takeLast(RAW_KEY_BYTES).toByteArray()
            val request = signedRequest(identity = attacker).copy(clientSigningPublicKey = attackerKey)

            assertRejected(client.register(request), HttpStatusCode.Forbidden, "PUSH_ROUTING_ID_MISMATCH")
            assertTrue(client.health().contains("devices=0"))
        }

    @Test
    fun signatureByAnotherKeyIsRejected() =
        pushApplication {
            val request = signedRequest(identity = NodeIdentity.generate())

            assertRejected(client.register(request), HttpStatusCode.Forbidden, "PUSH_PROOF_INVALID")
        }

    @Test
    fun signatureOverAnotherTokenIsRejected() =
        pushApplication {
            val request = signedRequest().copy(token = "attacker-token")

            assertRejected(client.register(request), HttpStatusCode.Forbidden, "PUSH_PROOF_INVALID")
        }

    @Test
    fun staleOrFutureProofIsRejected() =
        pushApplication {
            val skew = PushDeviceRegistrationValidator.MAXIMUM_PROOF_CLOCK_SKEW_MILLISECONDS + 60_000L
            val now = System.currentTimeMillis()

            assertRejected(
                client.register(signedRequest(issuedAt = now - skew)),
                HttpStatusCode.Forbidden,
                "PUSH_PROOF_STALE"
            )
            assertRejected(
                client.register(signedRequest(issuedAt = now + skew)),
                HttpStatusCode.Forbidden,
                "PUSH_PROOF_STALE"
            )
        }

    @Test
    fun phoneAliasCannotBeRegistered() =
        pushApplication {
            val alias = ClientRoutingIds.fromPhoneNumber("+491701234567")
            val request = signedRequest(routing = alias)

            assertRejected(client.register(request), HttpStatusCode.Forbidden, "PUSH_ROUTING_ID_NOT_ALLOWED")
        }

    @Test
    fun blankTokenIsRejected() =
        pushApplication {
            assertRejected(
                client.register(signedRequest(token = " ")),
                HttpStatusCode.BadRequest,
                "INVALID_PUSH_DEVICE"
            )
        }

    @Test
    fun tokenRegisteredForANewRoutingIdReplacesTheOldBinding() =
        pushApplication {
            val other = NodeIdentity.generate()
            val otherKey = other.encodedPublicKey.takeLast(RAW_KEY_BYTES).toByteArray()
            val otherRoutingId = ClientRoutingIds.fromSigningPublicKey(otherKey)

            assertEquals(HttpStatusCode.NoContent, client.register(signedRequest()).status)
            assertEquals(
                HttpStatusCode.NoContent,
                client
                    .register(
                        signedRequest(identity = other, routing = otherRoutingId)
                            .copy(clientSigningPublicKey = otherKey)
                    ).status
            )
            assertTrue(client.health().contains("devices=1"))
        }

    private fun signedRequest(
        identity: NodeIdentity = owner,
        routing: String = routingId,
        token: String = TOKEN,
        issuedAt: Long = System.currentTimeMillis()
    ): PushDeviceRegistrationRequest {
        val signature =
            Signatures.sign(
                ProtocolSignatures.pushDeviceContent(PushDeviceBinding(routing, token, PLATFORM, issuedAt)),
                identity.privateKey
            )
        return PushDeviceRegistrationRequest(
            routingId = routing,
            token = token,
            platform = PLATFORM,
            issuedAtEpochMilliseconds = issuedAt,
            clientSigningPublicKey = ownerPublicKey,
            clientSignature = signature
        )
    }

    private fun pushApplication(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                pushModule(
                    PushConfig(
                        pushInternalApiToken = null,
                        nodeRegistryUrl = null,
                        presenceDirectoryUrl = null,
                        databaseUrl = null,
                        databaseUser = "",
                        databasePassword = "",
                        databaseMaximumPoolSize = 1,
                        maximumEnvelopes = 100,
                        envelopeRetentionMilliseconds = 60_000L,
                        wakeUpLifetimeMilliseconds = 60_000L
                    )
                )
            }
            block()
        }

    private suspend fun HttpClient.register(request: PushDeviceRegistrationRequest): HttpResponse =
        post("/push/devices") {
            contentType(ContentType.Application.Json)
            setBody(serverJson.encodeToString(request))
        }

    private suspend fun HttpClient.health(): String = get("/health").bodyAsText()

    private suspend fun assertRejected(
        response: HttpResponse,
        status: HttpStatusCode,
        code: String
    ) {
        assertEquals(status, response.status)
        assertTrue(response.bodyAsText().contains(code), response.bodyAsText())
    }

    private companion object {
        const val RAW_KEY_BYTES = 32
        const val TOKEN = "fcm-token"
        const val PLATFORM = "ANDROID"
    }
}
