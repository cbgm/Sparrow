package com.cbgm.sparrow.feature.transport.connection

import com.cbgm.sparrow.feature.transport.TransportNodeDiagnosticState
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpoint
import kotlin.test.Test
import kotlin.test.assertEquals

class TransportDiagnosticsStateTest {
    @Test
    fun missingNodeRemainsVisibleAsCooldown() {
        var now = 1_000L
        val state = createState(now = { now })
        val nodeA = node("node-a")
        val nodeB = node("node-b")

        state.resolve(nodeA, nodeB)
        now += 1_000L
        state.resolve(nodeB)

        val missingNode = state.diagnostics.value.availableNodes.single { it.nodeId == nodeA.nodeId }

        assertEquals(TransportNodeDiagnosticState.COOLDOWN, missingNode.state)
        assertEquals(62_000L, missingNode.cooldownUntilEpochMilliseconds)
    }

    @Test
    fun aNodeInItsCooldownIsShownAsSuch() {
        val state = createState(now = { 1_000L })
        val nodeA = node("node-a")
        val nodeB = node("node-b")

        state.resolved(
            endpoints = listOf(nodeA, nodeB),
            cooldownUntilEpochMillisecondsByNodeId = mapOf(nodeA.nodeId to 61_000L),
            registryAuthorityVerified = true,
            registryUrl = "https://plane.example"
        )

        val cooldownNode = state.diagnostics.value.availableNodes.single { it.nodeId == nodeA.nodeId }
        val availableNode = state.diagnostics.value.availableNodes.single { it.nodeId == nodeB.nodeId }

        assertEquals(TransportNodeDiagnosticState.COOLDOWN, cooldownNode.state)
        assertEquals(TransportNodeDiagnosticState.AVAILABLE, availableNode.state)
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

    private fun node(nodeId: String): NodeEndpoint =
        NodeEndpoint(
            nodeId = nodeId,
            websocketUrl = "wss://$nodeId.example/v1/gateway"
        )
}
