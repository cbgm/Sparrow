package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.IdentityPeerState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

class IdentityRecoveryAuthorizationHoldTest {
    @Test
    fun pendingReplacementWithholdsPreviousAuthorizationWithoutChangingStoredState() {
        val established = IdentityPeerState(hasEstablishedExchange = true, hasMutualIdentity = true)
        val effective = established.withRecoveryHold(hasPendingIdentityChange = true)
        assertFalse(effective.hasEstablishedExchange)
        assertFalse(effective.hasMutualIdentity)
        assertEquals(IdentityPeerState(true, true), established)
    }

    @Test
    fun noPendingReplacementRetainsTheOriginalIdentityState() {
        val established = IdentityPeerState(hasEstablishedExchange = true, hasMutualIdentity = true)
        assertSame(established, established.withRecoveryHold(hasPendingIdentityChange = false))
    }
}
