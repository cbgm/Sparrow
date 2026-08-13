package com.cbgm.securechat.feature.transport.push.inbox

import com.cbgm.securechat.feature.transport.controlplane.ControlPlaneRequestRouter
import com.cbgm.securechat.feature.transport.gateway.model.TransportEnvelope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode

class HttpPendingEnvelopeGateway(
    private val httpClient: HttpClient,
    private val controlPlaneRequestRouter: ControlPlaneRequestRouter
) : PendingEnvelopeGateway {
    override suspend fun getPendingEnvelopes(wakeUpId: String): Result<List<TransportEnvelope>> =
        runCatching {
            require(wakeUpId.isNotBlank()) {
                "Wake-up ID must not be blank"
            }

            controlPlaneRequestRouter
                .execute { endpoint ->
                    httpClient
                        .get(
                            urlString = "${endpoint.baseUrl}/push/wake/$wakeUpId/inbox"
                        ).body<PendingTransportEnvelopesResponse>()
                        .envelopes
                }.getOrThrow()
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
