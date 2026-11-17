package com.cbgm.sparrow.feature.transport.circuit

import com.cbgm.sparrow.feature.transport.discovery.CachedNodeDirectory
import com.cbgm.sparrow.feature.transport.discovery.NodeDirectoryCache
import com.cbgm.sparrow.feature.transport.gateway.codec.createGatewayJson
import com.cbgm.sparrow.feature.transport.relay.RelaySelector
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CircuitMailboxEngineTest {
    private val json = createGatewayJson()
    private val seenDirectly = mutableListOf<String>()
    private val direct =
        HttpClient(
            MockEngine { request ->
                seenDirectly += "${request.url} ${request.headers[HttpHeaders.Authorization]}"
                respond("""{"envelopes":[]}""", HttpStatusCode.OK)
            }
        )
    private val runner =
        CircuitRunner(
            httpClient = direct,
            cipher = ReadableCircuitCipher(),
            selector = RelaySelector(EmptyCache, json, preferredNodeStore = null, connectedNodeId = { null }),
            hopCountStore = FixedHopCount,
            circuitCountStore = OneCircuit
        )

    @Test
    fun withoutACircuitARequestGoesOutDirectlyByDefault() =
        runTest {
            val client = mailboxClient(sendsDirectly = true)

            val response = client.get("https://box.test/v1/mailboxes/m/envelopes") { bearerAuth("retrieval") }

            assertEquals("""{"envelopes":[]}""", response.bodyAsText())
            assertEquals(listOf("https://box.test/v1/mailboxes/m/envelopes Bearer retrieval"), seenDirectly)
        }

    @Test
    fun withoutACircuitARequestWaitsWhenTheUserChoseSo() =
        runTest {
            val client = mailboxClient(sendsDirectly = false)

            assertFailsWith<IllegalStateException> { client.get("https://box.test/v1/mailboxes/m/envelopes") }
            assertEquals(emptyList(), seenDirectly)
        }

    @Test
    fun withoutACircuitAFileTransferGoesOutDirectlyOnlyWhenTheUserAllowsIt() =
        runTest {
            val directly =
                FileTransferCircuits(runner, FixedFallback(sendsDirectly = true), direct, waitMilliseconds = 50)
                    .transfer { route -> route.request(FileTransferRequest("GET", "https://node.test/v1/blobs/b/chunks/0", "read")) }
            val held = FileTransferCircuits(runner, FixedFallback(sendsDirectly = false), direct, waitMilliseconds = 50)

            assertEquals(200, directly.status)
            assertFailsWith<ProtectedRouteMissingException> { held.transfer { route -> route.request(FileTransferRequest("GET", "https://node.test/x")) } }
            assertEquals(listOf("https://node.test/v1/blobs/b/chunks/0 Bearer read"), seenDirectly)
        }

    private fun mailboxClient(sendsDirectly: Boolean): HttpClient =
        createMailboxHttpClient(
            CircuitMailboxEngine(runner, FixedFallback(sendsDirectly), direct, waitMilliseconds = 50),
            json
        )

    private object EmptyCache : NodeDirectoryCache {
        override suspend fun read(): CachedNodeDirectory? = null

        override suspend fun write(directory: CachedNodeDirectory) = Unit
    }

    private object OneCircuit : CircuitCountStore {
        override fun observe(): Flow<Int> = flowOf(1)

        override suspend fun set(circuitCount: Int) = Unit
    }

    private object FixedHopCount : CircuitHopCountStore {
        override fun observe(): Flow<Int> = flowOf(2)

        override suspend fun set(hopCount: Int) = Unit
    }

    private class FixedFallback(
        sendsDirectly: Boolean
    ) : CircuitFallbackStore {
        private val state = MutableStateFlow(sendsDirectly)

        override fun observeSendsDirectly(): Flow<Boolean> = state

        override suspend fun setSendsDirectly(sendsDirectly: Boolean) {
            state.value = sendsDirectly
        }
    }
}
