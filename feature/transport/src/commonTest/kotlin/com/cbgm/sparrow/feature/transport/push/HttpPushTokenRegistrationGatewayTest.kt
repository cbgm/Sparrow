package com.cbgm.sparrow.feature.transport.push

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.feature.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.feature.transport.ControlPlaneEndpoint
import com.cbgm.sparrow.feature.transport.ControlPlaneEndpointStatus
import com.cbgm.sparrow.feature.transport.ControlPlaneReachability
import com.cbgm.sparrow.feature.transport.ControlPlaneStatusStore
import com.cbgm.sparrow.feature.transport.controlplane.ControlPlaneRequestRejectedException
import com.cbgm.sparrow.feature.transport.controlplane.ControlPlaneRequestRouter
import com.cbgm.sparrow.feature.transport.domain.usecase.CreatePushDeviceRegistrationUseCase
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HttpPushTokenRegistrationGatewayTest {
    private val configuration = FakeControlPlaneConfiguration()

    @Test
    fun clientErrorIsReportedAsRejectionNotAsOffline() =
        runTest {
            val result =
                register(HttpStatusCode.Forbidden, """{"code":"PUSH_PROOF_STALE","message":"stale"}""")

            val error = assertIs<ControlPlaneRequestRejectedException>(result.exceptionOrNull())
            assertTrue(error.message.orEmpty().contains("403"))
            assertTrue(error.message.orEmpty().contains("PUSH_PROOF_STALE"))
            assertEquals(ControlPlaneReachability.AVAILABLE, configuration.statuses.value.single().reachability)
        }

    @Test
    fun acceptedRegistrationSendsTheSignedProof() =
        runTest {
            var body = ""

            val result =
                register(HttpStatusCode.NoContent, "") { requestBody -> body = requestBody }

            assertTrue(result.isSuccess)
            assertTrue(body.contains("\"routingId\":\"$ROUTING_ID\""))
            assertTrue(body.contains("\"issuedAtEpochMilliseconds\""))
            assertTrue(body.contains("\"clientSigningPublicKey\":[1,2,3]"))
            assertTrue(body.contains("\"clientSignature\":[7,8,9]"))
        }

    private suspend fun register(
        status: HttpStatusCode,
        responseBody: String,
        onRequest: (String) -> Unit = {}
    ): Result<Unit> {
        val client =
            HttpClient(
                MockEngine { request ->
                    onRequest((request.body as TextContent).text)
                    respond(
                        content = responseBody,
                        status = status,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            ) {
                // Same as the platform client: any 4xx throws before the gateway sees the status.
                expectSuccess = true
                install(ContentNegotiation) { json() }
            }
        return try {
            // Keep the asynchronous MockEngine on real time, see HttpControlPlaneDirectorySynchronizerTest.
            withContext(Dispatchers.Default) {
                HttpPushTokenRegistrationGateway(
                    httpClient = client,
                    localRoutingIdProvider =
                        object : LocalRoutingIdProvider {
                            override suspend fun getLocalRoutingId(): Result<String> = Result.success(ROUTING_ID)
                        },
                    controlPlaneRequestRouter = ControlPlaneRequestRouter(configuration, configuration),
                    createPushDeviceRegistration = createPushDeviceRegistration()
                ).register(token = "fcm-token", platform = PushPlatform.ANDROID)
            }
        } finally {
            client.close()
        }
    }

    private fun createPushDeviceRegistration(): CreatePushDeviceRegistrationUseCase =
        CreatePushDeviceRegistrationUseCase(
            signingKeyPairProvider =
                object : LocalSigningKeyPairProvider {
                    override suspend fun getSigningKeyPair(): Result<LocalSigningKeyPair> =
                        Result.success(LocalSigningKeyPair(publicKey = byteArrayOf(1, 2, 3), privateKey = byteArrayOf(4)))
                },
            signatureCrypto =
                object : DetachedSignatureCrypto {
                    override suspend fun sign(
                        payload: ByteArray,
                        signingPrivateKey: ByteArray
                    ): Result<ByteArray> = Result.success(byteArrayOf(7, 8, 9))

                    override suspend fun verify(
                        payload: ByteArray,
                        signingPublicKey: ByteArray,
                        signature: ByteArray
                    ): Result<Unit> = Result.success(Unit)
                }
        )

    private class FakeControlPlaneConfiguration :
        ControlPlaneConfiguration,
        ControlPlaneStatusStore {
        override val endpoints = MutableStateFlow(listOf(ControlPlaneEndpoint("https://primary")))
        override val activeEndpoint = MutableStateFlow<ControlPlaneEndpoint?>(endpoints.value.first())
        override val manualBaseUrls = MutableStateFlow(setOf("https://primary"))
        override val directoryBaseUrls = MutableStateFlow(emptySet<String>())
        override val directoryUrl = MutableStateFlow<String?>(null)
        override val statuses =
            MutableStateFlow(
                listOf(
                    ControlPlaneEndpointStatus(
                        endpoint = endpoints.value.first(),
                        isActive = true,
                        reachability = ControlPlaneReachability.UNREACHABLE
                    )
                )
            )

        override fun orderedEndpoints(): List<ControlPlaneEndpoint> = endpoints.value

        override fun markActive(endpoint: ControlPlaneEndpoint) {
            activeEndpoint.value = endpoint
        }

        override suspend fun replace(baseUrls: List<String>): Result<Unit> = Result.success(Unit)

        override suspend fun addManual(baseUrl: String): Result<Unit> = Result.success(Unit)

        override suspend fun removeManual(baseUrl: String): Result<Unit> = Result.success(Unit)

        override suspend fun setDirectoryUrl(url: String?): Result<Unit> = Result.success(Unit)

        override suspend fun replaceDirectory(baseUrls: List<String>): Result<Unit> = Result.success(Unit)

        override fun markAvailable(endpoint: ControlPlaneEndpoint) {
            statuses.value = statuses.value.map { it.copy(reachability = ControlPlaneReachability.AVAILABLE) }
        }

        override fun markUnreachable(endpoint: ControlPlaneEndpoint) {
            statuses.value = statuses.value.map { it.copy(reachability = ControlPlaneReachability.UNREACHABLE) }
        }
    }

    private companion object {
        const val ROUTING_ID = "scrouting1_test"
    }
}
