package com.cbgm.securechat.feature.transport.connection

import com.cbgm.securechat.core.transport.TransportNodeDiagnosticState
import com.cbgm.securechat.feature.transport.discovery.NodeEndpoint
import kotlin.test.Test
import kotlin.test.assertEquals

class TransportDiagnosticsStateTest {
    @Test
    fun missingNodeRemainsVisibleAsCooldownWithZeroConnections() {
        var now = 1_000L
        val state = createState(now = { now })
        val nodeA = node("node-a", activeConnections = 3)
        val nodeB = node("node-b", activeConnections = 2)

        state.resolve(nodeA, nodeB)
        now += 1_000L
        state.resolve(nodeB)

        val missingNode = state.diagnostics.value.availableNodes.single { it.nodeId == nodeA.nodeId }

        assertEquals(TransportNodeDiagnosticState.COOLDOWN, missingNode.state)
        assertEquals(0, missingNode.activeConnections)
        assertEquals(62_000L, missingNode.cooldownUntilEpochMilliseconds)
    }

    @Test
    fun missingNodeIsRemovedAfterCooldownExpires() {
        var now = 1_000L
        val state = createState(now = { now })
        val nodeA = node("node-a")
        val nodeB = node("node-b")

        state.resolve(nodeA, nodeB)
        now += 1_000L
        state.resolve(nodeB)
        now += 60_000L
        state.resolve(nodeB)

        assertEquals(listOf(nodeB.nodeId), state.diagnostics.value.availableNodes.map { it.nodeId })
    }

    @Test
    fun returningNodeStopsBeingRetainedAsCooldown() {
        var now = 1_000L
        val state = createState(now = { now })
        val nodeA = node("node-a")
        val nodeB = node("node-b")

        state.resolve(nodeA, nodeB)
        now += 1_000L
        state.resolve(nodeB)
        now += 1_000L
        state.resolve(nodeA, nodeB)

        val node = state.diagnostics.value.availableNodes.single { it.nodeId == nodeA.nodeId }

        assertEquals(TransportNodeDiagnosticState.AVAILABLE, node.state)
    }

    private fun createState(now: () -> Long): TransportDiagnosticsState =
        TransportDiagnosticsState(
            registryUrl = "https://plane.example",
            missingNodeCooldownMilliseconds = 60_000L,
            now = now
        )

    private fun TransportDiagnosticsState.resolve(vararg endpoints: NodeEndpoint) {
        resolved(
            endpoints = endpoints.toList(),
            cooldownUntilEpochMillisecondsByNodeId = emptyMap(),
            registryAuthorityVerified = true,
            registryUrl = "https://plane.example"
        )
    }

    private fun node(
        nodeId: String,
        activeConnections: Int = 0
    ): NodeEndpoint =
        NodeEndpoint(
            nodeId = nodeId,
            websocketUrl = "wss://$nodeId.example/v1/gateway",
            activeConnections = activeConnections
        )
}
