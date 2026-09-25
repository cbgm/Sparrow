package com.cbgm.sparrow.feature.conversationorchestration.runtime.mailbox

import com.cbgm.sparrow.core.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.core.protocol.mailbox.MailboxDeliveryRoute
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpoint
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MailboxRouteNodeRetirementTest {
    @Test
    fun `old credential is retired when freshly discovered server has a different node identity`() {
        assertTrue(oldCredential().isRetiredFrom(listOf(node("new-node"))))
    }

    @Test
    fun `same node remains valid even if its mailbox endpoint changes`() {
        assertFalse(oldCredential().isRetiredFrom(listOf(node("old-node"))))
    }

    @Test
    fun `multi node directory must not retire a credential on a non selected node`() {
        assertFalse(oldCredential().isRetiredFrom(listOf(node("new-node"), node("old-node"))))
    }

    @Test
    fun `no directory is not proof that the original mailbox was deleted`() {
        assertFalse(oldCredential().isRetiredFrom(emptyList()))
    }

    private fun node(nodeId: String): NodeEndpoint =
        NodeEndpoint(
            nodeId = nodeId,
            websocketUrl = "wss://node.example/v1/gateway",
            mailboxRouteEndpoint = "https://node.example",
            mailboxAccessEndpoint = "https://node.example"
        )

    private fun oldCredential(): LocalMailboxCredential =
        LocalMailboxCredential(
            contactId = "contact-1",
            deliveryRoute = MailboxDeliveryRoute(
                routeId = "route-1",
                nodeId = "old-node",
                nodeEndpoint = "http://192.168.178.60:8490",
                mailboxId = "mailbox-1",
                sendCapability = "send-capability",
                sequence = 1L,
                expiresAtEpochMilliseconds = Long.MAX_VALUE,
                identitySignature = byteArrayOf(1)
            ),
            accessEndpoint = "http://192.168.178.60:8490",
            retrievalCapability = "retrieval-capability"
        )
}
