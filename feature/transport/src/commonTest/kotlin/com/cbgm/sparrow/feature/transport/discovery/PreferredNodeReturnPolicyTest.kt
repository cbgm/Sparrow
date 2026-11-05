package com.cbgm.sparrow.feature.transport.discovery

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PreferredNodeReturnPolicyTest {
    private val endpoints = listOf(NodeEndpoint("favourite", "wss://a"), NodeEndpoint("fallback", "wss://b"))

    @Test
    fun returnsToTheFavouriteOnceItIsListedAgain() {
        assertTrue(shouldReturn())
    }

    @Test
    fun staysWhileTheFavouriteIsMissingOrCoolingDown() {
        assertFalse(shouldReturn(endpoints = endpoints.drop(1)))
        assertFalse(shouldReturn(coolingDown = setOf("favourite")))
    }

    @Test
    fun staysWithoutAFavouriteOrWhenAlreadyOnIt() {
        assertFalse(shouldReturn(preferred = null))
        assertFalse(shouldReturn(connected = "favourite"))
    }

    private fun shouldReturn(
        preferred: String? = "favourite",
        connected: String = "fallback",
        endpoints: List<NodeEndpoint> = this.endpoints,
        coolingDown: Set<String> = emptySet()
    ) = PreferredNodeReturnPolicy.shouldReturn(preferred, connected, endpoints, coolingDown)
}
