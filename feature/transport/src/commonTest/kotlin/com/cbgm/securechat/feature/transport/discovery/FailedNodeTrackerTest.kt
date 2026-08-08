package com.cbgm.securechat.feature.transport.discovery

import kotlin.test.Test
import kotlin.test.assertEquals

class FailedNodeTrackerTest {
    @Test
    fun failedNodeIsSkippedUntilItsCooldownExpires() {
        var now = 1_000L
        val tracker = FailedNodeTracker(cooldownMilliseconds = 30_000L, now = { now })
        val nodeA = NodeEndpoint("node-a", "wss://a.example/relay")
        val nodeB = NodeEndpoint("node-b", "wss://b.example/relay")

        tracker.recordFailure(nodeA.nodeId)

        assertEquals(listOf(nodeB), tracker.available(listOf(nodeA, nodeB)))
        assertEquals(setOf(nodeA.nodeId), tracker.unavailableNodeIds(listOf(nodeA, nodeB)))

        now += 30_000L

        assertEquals(listOf(nodeA, nodeB), tracker.available(listOf(nodeA, nodeB)))
        assertEquals(emptySet(), tracker.unavailableNodeIds(listOf(nodeA, nodeB)))
    }

    @Test
    fun successfulConnectionRemovesPreviousFailure() {
        val tracker = FailedNodeTracker(cooldownMilliseconds = 30_000L, now = { 1_000L })
        val node = NodeEndpoint("node-a", "wss://a.example/relay")

        tracker.recordFailure(node.nodeId)
        tracker.recordSuccess(node.nodeId)

        assertEquals(listOf(node), tracker.available(listOf(node)))
        assertEquals(emptySet(), tracker.unavailableNodeIds(listOf(node)))
    }
}
