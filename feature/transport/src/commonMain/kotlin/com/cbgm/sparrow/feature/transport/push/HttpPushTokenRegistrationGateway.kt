package com.cbgm.sparrow.feature.transport.push

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
                .executeAll { endpoint ->
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

                    check(response.status == HttpStatusCode.NoContent) {
                        "Push-token registration failed with ${response.status}"
                    }
                }.getOrThrow()
        }
}
