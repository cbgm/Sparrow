package com.cbgm.sparrow.feature.transport.data.route.circuit

import com.cbgm.sparrow.feature.transport.FakeTransportPreferencesRepository
import com.cbgm.sparrow.feature.transport.data.directory.CachedNodeDirectoryDto
import com.cbgm.sparrow.feature.transport.data.directory.NodeDirectoryCache
import com.cbgm.sparrow.feature.transport.data.mailbox.createGatewayJson
import com.cbgm.sparrow.feature.transport.data.route.RelaySelector
import com.cbgm.sparrow.feature.transport.domain.model.ProtectedRouteMissingException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
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
            selector = RelaySelector(EmptyCache, json, preferences = null, connectedNodeId = { null }),
            preferences = FakeTransportPreferencesRepository(circuitCount = 1)
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
                FileTransferCircuits(runner, FakeTransportPreferencesRepository(sendsDirectly = true), direct, waitMilliseconds = 50)
                    .transfer { route -> route.request(FileTransferRequest("GET", "https://node.test/v1/blobs/b/chunks/0", "read")) }
            val held = FileTransferCircuits(runner, FakeTransportPreferencesRepository(sendsDirectly = false), direct, waitMilliseconds = 50)

            assertEquals(200, directly.status)
            assertFailsWith<ProtectedRouteMissingException> { held.transfer { route -> route.request(FileTransferRequest("GET", "https://node.test/x")) } }
            assertEquals(listOf("https://node.test/v1/blobs/b/chunks/0 Bearer read"), seenDirectly)
        }

    private fun mailboxClient(sendsDirectly: Boolean): HttpClient =
        createMailboxHttpClient(
            CircuitMailboxEngine(runner, FakeTransportPreferencesRepository(sendsDirectly = sendsDirectly), direct, waitMilliseconds = 50),
            json
        )

    private object EmptyCache : NodeDirectoryCache {
        override suspend fun read(): CachedNodeDirectoryDto? = null

        override suspend fun write(directory: CachedNodeDirectoryDto) = Unit
    }
}
