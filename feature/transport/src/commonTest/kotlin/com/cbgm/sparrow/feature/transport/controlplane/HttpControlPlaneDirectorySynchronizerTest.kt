package com.cbgm.sparrow.feature.transport.controlplane

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Exercises the current JSON-directory fetcher, not the removed parseControlPlaneDirectory API. */
class HttpControlPlaneDirectorySynchronizerTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun plainTextResponseBodyIsDecodedAsJson() {
        runTest {
            val body =
                """
                {
                  "controlPlanes": [
                    "http://plane-a.example.test",
                    "https://plane-b.example.test"
                  ]
                }
                """.trimIndent()

            assertEquals(
                listOf("http://plane-a.example.test", "https://plane-b.example.test"),
                fetchJsonAddresses(body)
            )
        }
    }

    @Test
    fun duplicateOriginsAreNormalizedAndRemoved() {
        runTest {
            val body =
                """
                {
                  "controlPlanes": [
                    " https://plane.example.test/ ",
                    "https://plane.example.test/"
                  ]
                }
                """.trimIndent()

            assertEquals(listOf("https://plane.example.test"), fetchJsonAddresses(body))
        }
    }

    @Test
    fun emptyDirectoryIsRejected() {
        runTest {
            assertFailsWith<IllegalArgumentException> {
                fetchJsonAddresses("""{"controlPlanes": []}""")
            }
        }
    }

    @Test
    fun blankOriginIsRejected() {
        runTest {
            assertFailsWith<IllegalArgumentException> {
                fetchJsonAddresses("""{"controlPlanes": ["   "]}""")
            }
        }
    }

    @Test
    fun invalidOriginIsRejected() {
        runTest {
            assertFailsWith<IllegalArgumentException> {
                fetchJsonAddresses(
                    """{"controlPlanes": ["https://valid.example.test", "not a URL"]}"""
                )
            }
        }
    }

    private suspend fun fetchJsonAddresses(body: String): List<String> {
        val client =
            HttpClient(
                MockEngine {
                    respond(
                        content = body,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/plain")
                    )
                }
            )
        return try {
            // Keep Ktor’s asynchronous MockEngine and the request timeout on real time.
            // runTest can otherwise advance virtual time to the 8 s timeout before
            // the engine gets scheduled, producing a false TimeoutCancellationException.
            withContext(Dispatchers.Default) {
                ControlPlaneDirectoryRemoteSource(client, json).fetchJsonAddresses(
                    "https://directory.example.test/directory.json"
                )
            }
        } finally {
            client.close()
        }
    }
}
