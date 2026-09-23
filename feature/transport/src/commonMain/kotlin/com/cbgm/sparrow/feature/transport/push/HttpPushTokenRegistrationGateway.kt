package com.cbgm.sparrow.feature.transport.push

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.transport.controlplane.ControlPlaneRequestRejectedException
import com.cbgm.sparrow.feature.transport.controlplane.ControlPlaneRequestRouter
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class HttpPushTokenRegistrationGateway(
    private val httpClient: HttpClient,
    private val localRoutingIdProvider: LocalRoutingIdProvider,
    private val controlPlaneRequestRouter: ControlPlaneRequestRouter
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

            controlPlaneRequestRouter
                .executeFirstAvailable { endpoint ->
                    val response =
                        httpClient.post(
                            urlString = "${endpoint.baseUrl}/push/devices"
                        ) {
                            contentType(ContentType.Application.Json)
                            setBody(
                                PushDeviceRegistrationRequest(
                                    routingId = routingId,
                                    token = token,
                                    platform = platform.name
                                )
                            )
                        }

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
}
