package com.cbgm.securechat.feature.transport.push

import com.cbgm.securechat.feature.transport.controlplane.ControlPlaneRequestRouter
import com.cbgm.securechat.feature.transport.relay.api.PushDeviceRegistrationRequest
import com.cbgm.securechat.feature.transport.relay.identity.LocalRelayIdProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class HttpPushTokenRegistrationGateway(
    private val httpClient: HttpClient,
    private val localRelayIdProvider: LocalRelayIdProvider,
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

            val relayId =
                localRelayIdProvider
                    .getLocalRelayId()
                    .getOrThrow()

            controlPlaneRequestRouter
                .execute { endpoint ->
                    val response =
                        httpClient.post(
                            urlString = "${endpoint.baseUrl}/push/devices"
                        ) {
                            contentType(ContentType.Application.Json)
                            setBody(
                                PushDeviceRegistrationRequest(
                                    relayId = relayId,
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
