package com.cbgm.sparrow.feature.transport.push

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.transport.ControlPlaneEndpoint
import com.cbgm.sparrow.feature.transport.controlplane.ControlPlaneRequestRejectedException
import com.cbgm.sparrow.feature.transport.controlplane.ControlPlaneRequestRouter
import com.cbgm.sparrow.feature.transport.domain.usecase.CreatePushDeviceRegistrationUseCase
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class HttpPushTokenRegistrationGateway(
    private val httpClient: HttpClient,
    private val localRoutingIdProvider: LocalRoutingIdProvider,
    private val controlPlaneRequestRouter: ControlPlaneRequestRouter,
    private val createPushDeviceRegistration: CreatePushDeviceRegistrationUseCase
) : PushTokenRegistrationGateway {
    private val logger = SparrowLog.withTag("HttpPushTokenRegistrationGateway")

    override suspend fun register(
        token: String,
        platform: PushPlatform
    ): Result<Unit> =
        runCatching {
            require(token.isNotBlank()) {
                "Push token must not be blank"
            }

            val routingId =
                localRoutingIdProvider
                    .getLocalRoutingId()
                    .getOrThrow()
            val request =
                createPushDeviceRegistration(
                    routingId = routingId,
                    token = token,
                    platform = platform
                ).getOrThrow()

            controlPlaneRequestRouter
                .executeFirstAvailable { endpoint ->
                    val response = post(endpoint, request)
                    if (response.status != HttpStatusCode.NoContent) {
                        throw ControlPlaneRequestRejectedException(
                            "Push-token registration rejected with ${response.status} " +
                                "at ${endpoint.baseUrl}"
                        )
                    }
                    // Never include FCM token or routing ID in diagnostic logs.
                    logger.info { "Push-token registration accepted by ${endpoint.baseUrl}" }
                }.getOrThrow()
        }

    /**
     * The shared client uses expectSuccess, so a 4xx arrives as [ClientRequestException]. It is the server
     * refusing the request (for example a stale or invalid proof), not an unreachable control plane.
     */
    private suspend fun post(
        endpoint: ControlPlaneEndpoint,
        request: PushDeviceRegistrationRequest
    ): HttpResponse =
        try {
            httpClient.post(
                urlString = "${endpoint.baseUrl}/push/devices"
            ) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        } catch (error: ClientRequestException) {
            val reason = runCatching { error.response.bodyAsText().take(MAXIMUM_REASON_LENGTH) }.getOrDefault("")
            throw ControlPlaneRequestRejectedException(
                "Push-token registration rejected with ${error.response.status} " +
                    "at ${endpoint.baseUrl}: $reason"
            )
        }

    private companion object {
        const val MAXIMUM_REASON_LENGTH = 200
    }
}
