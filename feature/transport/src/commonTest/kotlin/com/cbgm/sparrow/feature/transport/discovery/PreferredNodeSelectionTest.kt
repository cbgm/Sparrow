package com.cbgm.sparrow.feature.transport.discovery

import com.cbgm.sparrow.feature.transport.config.TransportConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PreferredNodeSelectionTest {
    private val selector = NodeEndpointSelector(TransportConfig())
    private val directory =
        SignedNodeDirectory(
            directory =
                NodeDirectory(
                    generatedAtEpochMilliseconds = 0L,
                    validUntilEpochMilliseconds = 1L,
                    nodes =
                        listOf(
                            descriptor("node-a"),
                            descriptor("node-b"),
                            descriptor("node-c")
                        )
                ),
            authorityNodeId = "authority",
            authorityPublicKey = byteArrayOf(),
            signature = byteArrayOf()
        )

    @Test
    fun withoutAFavouriteEveryNodeIsListedInTheSameOrderEachTime() {
        val automatic = select(preferredNodeId = null)

        assertEquals(setOf("node-a", "node-b", "node-c"), automatic.toSet())
        assertEquals(automatic, select(preferredNodeId = null))
    }

    @Test
    fun favouriteComesFirstAndTheOthersStayAsFailover() {
        val automatic = select(preferredNodeId = null)

        assertEquals(listOf("node-b") + (automatic - "node-b"), select(preferredNodeId = "node-b"))
    }

    @Test
    fun aFavouriteMissingFromTheDirectoryChangesNothing() {
        assertEquals(select(preferredNodeId = null), select(preferredNodeId = "gone"))
    }

    @Test
    fun searchMatchesAddressNodeIdAndDottedIp() {
        val node = GatewayNode("abcdef123456", "gateway-80-131-151-133.sslip.io")

        assertTrue(node.matches(""))
        assertTrue(node.matches("SSLIP"))
        assertTrue(node.matches("80.131.151.133"))
        assertTrue(node.matches("abcdef"))
        assertFalse(node.matches("example.org"))
    }

    @Test
    fun addressIsTheHostOfTheGatewayUrl() {
        val node = NodeEndpoint(nodeId = "n", websocketUrl = "wss://node.example.org:8443/v1/gateway").toGatewayNode()

        assertEquals("node.example.org:8443", node.address)
    }

    private fun select(preferredNodeId: String?): List<String> =
        selector.select(directory, localRoutingId = "me", preferredNodeId = preferredNodeId).map(NodeEndpoint::nodeId)

    private fun descriptor(nodeId: String) =
        SparrowNodeDescriptor(
            nodeId = nodeId,
            clientEndpoint = "wss://$nodeId.example.org/v1/gateway",
            mailboxEndpoint = "https://$nodeId.example.org/mailbox",
            identityPublicKey = byteArrayOf(),
            protocolVersions = setOf(TransportConfig().supportedProtocolVersion),
            capabilities = setOf(NodeCapability.GATEWAY),
            validUntilEpochMilliseconds = 1L,
            signature = byteArrayOf()
        )
}
