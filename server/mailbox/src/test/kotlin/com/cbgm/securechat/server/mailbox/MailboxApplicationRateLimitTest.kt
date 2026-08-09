package com.cbgm.securechat.server.mailbox

import com.cbgm.securechat.server.protocol.CreateMailboxRequest
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.security.RateLimitPolicy
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MailboxApplicationRateLimitTest {
    @Test
    fun mailboxCreationReturnsRetryAfterWhenRateLimited() =
        testApplication {
            application {
                mailboxModule(
                    config =
                        MailboxConfig(
                            databaseUrl = null,
                            databaseUser = "",
                            databasePassword = "",
                            databaseMaximumPoolSize = 1,
                            maximumEnvelopeBytes = 1_048_576,
                            maximumMailboxBytes = 100L * 1_048_576L,
                            creationRateLimit =
                                RateLimitPolicy(
                                    maximumRequests = 1,
                                    windowMilliseconds = 60_000L
                                )
                        ),
                    store = MailboxStore(),
                    pushNotifier = MailboxPushNotifier.disabled()
                )
            }
            val request =
                CreateMailboxRequest(
                    nodeId = "node-a",
                    nodeEndpoint = "http://mailbox",
                    expiresAtEpochMilliseconds = System.currentTimeMillis() + 60_000L
                )

            val first =
                client.post("/v1/mailboxes") {
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(request))
                }
            val rejected =
                client.post("/v1/mailboxes") {
                    contentType(ContentType.Application.Json)
                    setBody(serverJson.encodeToString(request))
                }

            assertEquals(HttpStatusCode.Created, first.status)
            assertEquals(HttpStatusCode.TooManyRequests, rejected.status)
            assertEquals("60", rejected.headers["Retry-After"])
            assertTrue(rejected.bodyAsText().contains("RATE_LIMITED"))
        }
}
