package com.cbgm.sparrow.feature.transport.mailbox

import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.protocol.mailbox.MailboxDeliveryRoute
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs

class HttpMailboxGatewayTest {
    @Test
    fun aDownNodeIsReportedAsUnavailable() =
        runTest {
            val result = gateway(HttpStatusCode.BadGateway).pending(credential)

            assertIs<MailboxNodeUnavailableException>(result.exceptionOrNull())
        }

    @Test
    fun aRefusedRequestIsNotTreatedAsADownNode() =
        runTest {
            val error = gateway(HttpStatusCode.Unauthorized).pending(credential).exceptionOrNull()

            assertFalse(error is MailboxNodeUnavailableException)
        }

    private fun gateway(status: HttpStatusCode) =
        HttpMailboxGateway(
            HttpClient(MockEngine { respond(content = "", status = status) }) { expectSuccess = true }
        )

    private val credential =
        LocalMailboxCredential(
            contactId = "contact",
            deliveryRoute =
                MailboxDeliveryRoute(
                    routeId = "route",
                    nodeId = "node",
                    nodeEndpoint = "https://node.example.org",
                    mailboxId = "mailbox",
                    sendCapability = "send",
                    sequence = 0L,
                    expiresAtEpochMilliseconds = Long.MAX_VALUE,
                    identitySignature = byteArrayOf()
                ),
            accessEndpoint = "https://node.example.org",
            retrievalCapability = "retrieve"
        )
}
