package com.cbgm.sparrow.feature.transport.data.mailbox

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.transport.data.route.circuit.CircuitKey
import com.cbgm.sparrow.feature.transport.domain.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.domain.repository.InvitationMailboxRepository
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

internal class HttpMailboxRemoteDataSource internal constructor(
    private val httpClient: HttpClient,
    private val json: Json,
    private val creationWork: MailboxCreationWorkSolver,
    private val circuitSlot: (String) -> Int = { 0 }
) : MailboxRemoteDataSource {
    private val logger = SparrowLog.withTag("HttpMailboxRemoteDataSource")

    override suspend fun create(
        contactId: String,
        nodeId: String,
        routeEndpoint: String,
        accessEndpoint: String,
        sequence: Long,
        expiresAtEpochMilliseconds: Long
    ): Result<LocalMailboxCredential> =
        runCatching {
            open(contactId, accessEndpoint) { work -> CreateMailboxRequestDto(nodeId, routeEndpoint, sequence, expiresAtEpochMilliseconds, work) }
        }

    override suspend fun createInvitation(
        nodeId: String,
        routeEndpoint: String,
        accessEndpoint: String,
        expiresAtEpochMilliseconds: Long
    ): Result<LocalMailboxCredential> =
        runCatching {
            open(InvitationMailboxRepository.CONTACT_ID, accessEndpoint) { work ->
                CreateMailboxRequestDto(nodeId, routeEndpoint, routeSequence = 0L, expiresAtEpochMilliseconds, work, invitation = true)
            }
        }

    private suspend fun open(
        contactId: String,
        accessEndpoint: String,
        request: (MailboxCreationWorkDto) -> CreateMailboxRequestDto
    ): LocalMailboxCredential {
        val work = creationWork.solve()
        val response =
            httpClient.post("${accessEndpoint.trimEnd('/')}/v1/mailboxes") {
                attributes.put(CircuitKey, contactId)
                contentType(ContentType.Application.Json)
                setBody(request(work))
            }
        response.checkMailboxStatus("Mailbox creation", accepted = response.status == HttpStatusCode.Created)
        val created = response.body<CreateMailboxResponseDto>()
        return LocalMailboxCredential(
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
                    attributes.put(CircuitKey, credential.contactId)
                    bearerAuth(credential.retrievalCapability)
                }
            response.checkMailboxStatus("Mailbox retrieval", accepted = response.status.isSuccess())
            val envelopes = response.body<JsonObject>()["envelopes"] as? JsonArray ?: JsonArray(emptyList())
            val decoded = json.decodeEnvelopes<FederatedEnvelopeDto>(envelopes)
            decoded.unreadableEnvelopeIds.forEach { envelopeId ->
                acknowledge(credential, envelopeId)
                    .onSuccess { logger.warn { "Unreadable mailbox envelope dropped: envelopeId=$envelopeId" } }
            }
            decoded.envelopes.map(FederatedEnvelopeDto::toDomain)
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
                    attributes.put(CircuitKey, credential.contactId)
                    bearerAuth(credential.retrievalCapability)
                }
            response.checkMailboxStatus("Mailbox acknowledgement", accepted = response.status == HttpStatusCode.NoContent)
        }.markingNodeUnavailable()

    override suspend fun revoke(credential: LocalMailboxCredential): Result<Unit> =
        runCatching {
            val route = credential.deliveryRoute
            val response =
                httpClient.delete(
                    "${credential.accessEndpoint.trimEnd('/')}/v1/mailboxes/${route.mailboxId}"
                ) {
                    attributes.put(CircuitKey, credential.contactId)
                    bearerAuth(credential.retrievalCapability)
                }
            check(response.status == HttpStatusCode.NoContent || response.status in MAILBOX_GONE) {
                "Mailbox revocation failed with ${response.status}"
            }
        }

    override suspend fun deliver(envelope: FederatedEnvelope): Result<Unit> =
        runCatching { put(MailboxEnvelopeRequestDto(envelope.toDto())) }.markingNodeUnavailable()

    override suspend fun depositInvitation(envelope: FederatedEnvelope): Result<Unit> =
        runCatching { put(MailboxEnvelopeRequestDto(envelope.toDto(), depositWork = creationWork.solve())) }.markingNodeUnavailable()

    private suspend fun put(request: MailboxEnvelopeRequestDto) {
        val route = requireNotNull(request.envelope.mailboxRoute) { "The envelope names no mailbox" }
        val response =
            httpClient.post("${route.nodeEndpoint.trimEnd('/')}/v1/mailboxes/${route.mailboxId}/envelopes") {
                attributes.put(CircuitKey, route.mailboxId)
                bearerAuth(route.sendCapability)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        response.checkMailboxStatus("Mailbox delivery", accepted = response.status == HttpStatusCode.Accepted)
    }

    /** One wait per circuit, each naming only that circuit's mailboxes, so no session node sees them all. */
    override suspend fun ready(
        accessEndpoint: String,
        credentials: List<LocalMailboxCredential>
    ): Result<Set<String>> =
        runCatching {
            coroutineScope {
                credentials
                    .groupBy { credential -> circuitSlot(credential.contactId) }
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
                attributes.put(CircuitKey, credentials.first().contactId)
                contentType(ContentType.Application.Json)
                setBody(
                    MailboxWaitRequestDto(
                        credentials.map { credential -> MailboxWaitEntryDto(credential.deliveryRoute.mailboxId, credential.retrievalCapability) }
                    )
                )
            }
        response.checkMailboxStatus("Waiting for mailbox envelopes", accepted = response.status.isSuccess())
        return response.body<MailboxWaitResponseDto>().ready
    }
}
