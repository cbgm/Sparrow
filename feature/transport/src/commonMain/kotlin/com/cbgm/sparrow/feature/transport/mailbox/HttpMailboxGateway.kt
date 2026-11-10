package com.cbgm.sparrow.feature.transport.mailbox

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.transport.circuit.CircuitKey
import com.cbgm.sparrow.feature.transport.gateway.codec.decodeEnvelopes
import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

class HttpMailboxGateway internal constructor(
    private val httpClient: HttpClient,
    private val json: Json,
    private val creationWork: MailboxCreationWorkSolver,
    private val circuitSlot: (String) -> Int = { 0 }
) : MailboxGateway {
    private val logger = SparrowLog.withTag("HttpMailboxGateway")

    override suspend fun create(
        contactId: String,
        nodeId: String,
        routeEndpoint: String,
        accessEndpoint: String,
        sequence: Long,
        expiresAtEpochMilliseconds: Long,
        wakeTicket: MailboxWakeTicket
    ): Result<LocalMailboxCredential> =
        runCatching {
            val work = creationWork.solve()
            val response =
                httpClient.post("${accessEndpoint.trimEnd('/')}/v1/mailboxes") {
                    attributes.put(CircuitKey, contactId)
                    contentType(ContentType.Application.Json)
                    setBody(CreateMailboxRequest(nodeId, routeEndpoint, sequence, expiresAtEpochMilliseconds, wakeTicket, work))
                }
            check(response.status == HttpStatusCode.Created) {
                "Mailbox creation failed with ${response.status}"
            }
            val created = response.body<CreateMailboxResponse>()
            LocalMailboxCredential(
                contactId = contactId,
                deliveryRoute = created.deliveryRoute,
                accessEndpoint = accessEndpoint,
                retrievalCapability = created.retrievalCapability
            )
        }

    override suspend fun pending(credential: LocalMailboxCredential) =
        runCatching {
            val route = credential.deliveryRoute
            val response =
                httpClient.get(
                    "${credential.accessEndpoint.trimEnd('/')}/v1/mailboxes/${route.mailboxId}/envelopes"
                ) {
                    attributes.put(CircuitKey, route.mailboxId)
                    bearerAuth(credential.retrievalCapability)
                }
            check(response.status.isSuccess()) { "Mailbox retrieval failed with ${response.status}" }
            val envelopes = response.body<JsonObject>()["envelopes"] as? JsonArray ?: JsonArray(emptyList())
            val decoded = json.decodeEnvelopes<FederatedEnvelope>(envelopes)
            decoded.unreadableEnvelopeIds.forEach { envelopeId ->
                acknowledge(credential, envelopeId)
                    .onSuccess { logger.warn { "Unreadable mailbox envelope dropped: envelopeId=$envelopeId" } }
            }
            decoded.envelopes
        }.markingNodeUnavailable()

    override suspend fun acknowledge(
        credential: LocalMailboxCredential,
        envelopeId: String
    ): Result<Unit> =
        runCatching {
            val route = credential.deliveryRoute
            val response =
                httpClient.delete(
                    "${credential.accessEndpoint.trimEnd('/')}/v1/mailboxes/${route.mailboxId}/envelopes/$envelopeId"
                ) {
                    attributes.put(CircuitKey, route.mailboxId)
                    bearerAuth(credential.retrievalCapability)
                }
            check(response.status == HttpStatusCode.NoContent) {
                "Mailbox acknowledgement failed with ${response.status}"
            }
        }.markingNodeUnavailable()

    override suspend fun revoke(credential: LocalMailboxCredential): Result<Unit> =
        runCatching {
            val route = credential.deliveryRoute
            val response =
                httpClient.delete(
                    "${credential.accessEndpoint.trimEnd('/')}/v1/mailboxes/${route.mailboxId}"
                ) {
                    attributes.put(CircuitKey, route.mailboxId)
                    bearerAuth(credential.retrievalCapability)
                }
            check(response.status == HttpStatusCode.NoContent || response.status in MAILBOX_GONE) {
                "Mailbox revocation failed with ${response.status}"
            }
        }

    override suspend fun deliver(envelope: FederatedEnvelope): Result<Unit> =
        runCatching {
            val route = requireNotNull(envelope.mailboxRoute) { "The envelope names no mailbox" }
            val response =
                httpClient.post("${route.nodeEndpoint.trimEnd('/')}/v1/mailboxes/${route.mailboxId}/envelopes") {
                    attributes.put(CircuitKey, route.mailboxId)
                    bearerAuth(route.sendCapability)
                    contentType(ContentType.Application.Json)
                    setBody(MailboxEnvelopeRequest(envelope))
                }
            check(response.status == HttpStatusCode.Accepted) { "Mailbox delivery failed with ${response.status}" }
        }

    /** One wait per circuit, each naming only that circuit's mailboxes, so no session node sees them all. */
    override suspend fun ready(
        accessEndpoint: String,
        credentials: List<LocalMailboxCredential>
    ): Result<Set<String>> =
        runCatching {
            coroutineScope {
                credentials
                    .groupBy { credential -> circuitSlot(credential.deliveryRoute.mailboxId) }
                    .values
                    .map { onCircuit -> async { wait(accessEndpoint, onCircuit) } }
                    .awaitAll()
                    .flatten()
                    .toSet()
            }
        }.markingNodeUnavailable()

    private suspend fun wait(
        accessEndpoint: String,
        credentials: List<LocalMailboxCredential>
    ): List<String> {
        val response =
            httpClient.post("${accessEndpoint.trimEnd('/')}/v1/mailboxes/wait") {
                attributes.put(CircuitKey, credentials.first().deliveryRoute.mailboxId)
                contentType(ContentType.Application.Json)
                setBody(
                    MailboxWaitRequest(
                        credentials.map { credential -> MailboxWaitEntry(credential.deliveryRoute.mailboxId, credential.retrievalCapability) }
                    )
                )
            }
        check(response.status.isSuccess()) { "Waiting for mailbox envelopes failed with ${response.status}" }
        return response.body<MailboxWaitResponse>().ready
    }

    private companion object {
        /** The mailbox no longer exists for this capability; revoking it again can never succeed. */
        val MAILBOX_GONE = setOf(HttpStatusCode.NotFound, HttpStatusCode.Gone, HttpStatusCode.Unauthorized)
    }
}
