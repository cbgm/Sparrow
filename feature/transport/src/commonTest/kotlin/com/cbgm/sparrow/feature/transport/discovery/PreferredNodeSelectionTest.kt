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
                            descriptor("busy", activeConnections = 50),
                            descriptor("quiet", activeConnections = 1),
                            descriptor("medium", activeConnections = 10)
                        )
                ),
            authorityNodeId = "authority",
            authorityPublicKey = byteArrayOf(),
            signature = byteArrayOf()
        )

    @Test
    fun withoutAFavouriteTheLeastBusyNodeComesFirst() {
        assertEquals(listOf("quiet", "medium", "busy"), select(preferredNodeId = null))
    }

    @Test
    fun favouriteComesFirstAndTheOthersStayAsFailover() {
        assertEquals(listOf("busy", "quiet", "medium"), select(preferredNodeId = "busy"))
    }

    @Test
    fun aFavouriteMissingFromTheDirectoryChangesNothing() {
        assertEquals(listOf("quiet", "medium", "busy"), select(preferredNodeId = "gone"))
    }

    @Test
    fun searchMatchesAddressNodeIdAndDottedIp() {
        val node = GatewayNode("abcdef123456", "gateway-80-131-151-133.sslip.io", 3)

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

    private fun descriptor(
        nodeId: String,
        activeConnections: Int
    ) = SparrowNodeDescriptor(
        nodeId = nodeId,
        clientEndpoint = "wss://$nodeId.example.org/v1/gateway",
        federationEndpoint = "https://$nodeId.example.org",
        mailboxEndpoint = "https://$nodeId.example.org/mailbox",
        identityPublicKey = byteArrayOf(),
        protocolVersions = setOf(TransportConfig().supportedProtocolVersion),
        capabilities = setOf(NodeCapability.GATEWAY),
        validUntilEpochMilliseconds = 1L,
        activeConnections = activeConnections,
        signature = byteArrayOf()
    )
}
