package com.cbgm.securechat.server.observability

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ServerObservabilityTest {
    @Test
    fun `liveness echoes a valid caller request id`() =
        testApplication {
            application {
                installServerObservability("test-service")
            }

            val response =
                client.get("/health/live") {
                    header(HttpHeaders.XRequestId, "request-123")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("request-123", response.headers[HttpHeaders.XRequestId])
        }

    @Test
    fun `readiness returns service unavailable when dependency check fails`() =
        testApplication {
            application {
                installServerObservability("test-service") { false }
            }

            assertEquals(HttpStatusCode.ServiceUnavailable, client.get("/health/ready").status)
        }

    @Test
    fun `readiness returns service unavailable when dependency check throws`() =
        testApplication {
            application {
                installServerObservability("test-service") {
                    error("dependency unavailable")
                }
            }

            assertEquals(HttpStatusCode.ServiceUnavailable, client.get("/health/ready").status)
        }

    @Test
    fun `metrics expose ktor requests with service label`() =
        testApplication {
            application {
                installServerObservability("test-service")
            }

            client.get("/health/live")
            val metrics = client.get("/metrics").bodyAsText()

            assertContains(metrics, "ktor_http_server_requests")
            assertContains(metrics, "service=\"test-service\"")
        }
}
