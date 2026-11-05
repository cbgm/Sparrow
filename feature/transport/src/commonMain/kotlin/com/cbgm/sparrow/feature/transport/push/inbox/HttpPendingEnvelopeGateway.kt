package com.cbgm.sparrow.feature.transport.push.inbox

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.transport.controlplane.ControlPlaneRequestRouter
import com.cbgm.sparrow.feature.transport.gateway.codec.decodeEnvelopes
import com.cbgm.sparrow.feature.transport.gateway.model.TransportEnvelope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

class HttpPendingEnvelopeGateway(
    private val httpClient: HttpClient,
    private val controlPlaneRequestRouter: ControlPlaneRequestRouter,
    private val json: Json
) : PendingEnvelopeGateway {
    private val logger = SparrowLog.withTag("HttpPendingEnvelopeGateway")

    override suspend fun getPendingEnvelopes(wakeUpId: String): Result<List<TransportEnvelope>> =
        runCatching {
            require(wakeUpId.isNotBlank()) {
                "Wake-up ID must not be blank"
            }

            val response =
                controlPlaneRequestRouter
                    .execute { endpoint ->
                        httpClient
                            .get(
                                urlString = "${endpoint.baseUrl}/push/wake/$wakeUpId/inbox"
                            ).body<JsonObject>()
                    }.getOrThrow()
            val decoded = json.decodeEnvelopes<TransportEnvelope>(response["envelopes"] as? JsonArray ?: JsonArray(emptyList()))
            decoded.unreadableEnvelopeIds.forEach { envelopeId ->
                acknowledge(wakeUpId, envelopeId)
                    .onSuccess { logger.warn { "Unreadable pending envelope dropped: envelopeId=$envelopeId" } }
            }
            decoded.envelopes
        }

    override suspend fun acknowledge(
        wakeUpId: String,
        envelopeId: String
    ): Result<Unit> =
        runCatching {
            require(wakeUpId.isNotBlank()) {
                "Wake-up ID must not be blank"
            }

            require(envelopeId.isNotBlank()) {
                "Envelope ID must not be blank"
            }

            controlPlaneRequestRouter
                .execute { endpoint ->
                    val response =
                        httpClient.post(
                            urlString =
                                "${endpoint.baseUrl}/push/wake/$wakeUpId/inbox/" +
                                    "$envelopeId/ack"
                        )

                    check(response.status == HttpStatusCode.NoContent) {
                        "Pending-envelope acknowledgement failed with ${response.status}"
                    }
                }.getOrThrow()
        }
}
