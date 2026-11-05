package com.cbgm.sparrow.server.federation

import com.cbgm.sparrow.server.persistence.ControlPlaneEndpointPool
import com.cbgm.sparrow.server.protocol.NodeDirectory
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.security.NodeIdentity
import com.cbgm.sparrow.server.security.ProtocolSignatures
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CachingNodeRegistryClientRootTest {
    private val trusted = NodeIdentity.generate()
    private val other = NodeIdentity.generate()

    @Test
    fun configuredRootIsRequired() =
        runTest {
            registry(signer = { other }, configuredRoots = setOf(trusted.nodeId)).let { client ->
                assertFailsWith<Exception> { client.refresh() }
            }
            registry(signer = { trusted }, configuredRoots = setOf(trusted.nodeId)).refresh()
        }

    @Test
    fun withoutAConfiguredRootTheFirstVerifiedRootStays() =
        runTest {
            var signer = trusted
            val client = registry(signer = { signer }, configuredRoots = emptySet())

            client.refresh()
            signer = other
            assertFailsWith<Exception> { client.refresh() }
        }

    private fun registry(
        signer: () -> NodeIdentity,
        configuredRoots: Set<String>
    ): CachingNodeRegistryClient {
        val engine =
            MockEngine {
                val directory =
                    NodeDirectory(
                        generatedAtEpochMilliseconds = NOW,
                        validUntilEpochMilliseconds = NOW + 60_000L,
                        nodes = emptyList()
                    )
                respond(
                    content = serverJson.encodeToString(ProtocolSignatures.signDirectory(directory, signer())),
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        return CachingNodeRegistryClient(
            httpClient = HttpClient(engine) { install(ContentNegotiation) { json(serverJson) } },
            endpointPool = ControlPlaneEndpointPool(listOf("http://control-plane")),
            now = { NOW },
            configuredRootNodeIds = configuredRoots
        )
    }

    private companion object {
        const val NOW = 1_000_000L
    }
}
