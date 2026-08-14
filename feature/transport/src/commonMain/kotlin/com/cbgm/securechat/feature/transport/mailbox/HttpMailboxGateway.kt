package com.cbgm.securechat.feature.transport.mailbox

import com.cbgm.securechat.core.protocol.mailbox.LocalMailboxCredential
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

class HttpMailboxGateway(
    private val httpClient: HttpClient
) : MailboxGateway {
    override suspend fun create(
        contactId: String,
        nodeId: String,
        routeEndpoint: String,
        accessEndpoint: String,
        sequence: Long,
        expiresAtEpochMilliseconds: Long
    ): Result<LocalMailboxCredential> =
        runCatching {
            val response =
                httpClient.post("${accessEndpoint.trimEnd('/')}/v1/mailboxes") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateMailboxRequest(nodeId, routeEndpoint, sequence, expiresAtEpochMilliseconds))
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
                ) { bearerAuth(credential.retrievalCapability) }
            check(response.status.isSuccess()) { "Mailbox retrieval failed with ${response.status}" }
            response.body<MailboxEnvelopesResponse>().envelopes
        }

    override suspend fun acknowledge(
        credential: LocalMailboxCredential,
        envelopeId: String
    ): Result<Unit> =
        runCatching {
            val route = credential.deliveryRoute
            val response =
                httpClient.delete(
                    "${credential.accessEndpoint.trimEnd('/')}/v1/mailboxes/${route.mailboxId}/envelopes/$envelopeId"
                ) { bearerAuth(credential.retrievalCapability) }
            check(response.status == HttpStatusCode.NoContent) {
                "Mailbox acknowledgement failed with ${response.status}"
            }
        }

    override suspend fun revoke(credential: LocalMailboxCredential): Result<Unit> =
        runCatching {
            val route = credential.deliveryRoute
            val response =
                httpClient.delete(
                    "${credential.accessEndpoint.trimEnd('/')}/v1/mailboxes/${route.mailboxId}"
                ) { bearerAuth(credential.retrievalCapability) }
            check(response.status == HttpStatusCode.NoContent) {
                "Mailbox revocation failed with ${response.status}"
            }
        }
}
