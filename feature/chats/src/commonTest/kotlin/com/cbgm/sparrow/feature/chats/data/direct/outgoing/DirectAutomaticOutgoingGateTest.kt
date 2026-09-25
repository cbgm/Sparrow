package com.cbgm.sparrow.feature.chats.data.direct.outgoing

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DirectAutomaticOutgoingGateTest {
    @Test
    fun startupWithNoQueuedMessagesOrReceiptsDoesNotConsultAuthorization() = runTest {
        var authorizationChecks = 0
        var outgoingOperations = 0
        val gate = DirectAutomaticOutgoingGate {
            authorizationChecks++
            "Mutual keys are not available"
        }

        assertFalse(gate.run("contact", hasPendingWork = false) { outgoingOperations++ })
        assertEquals(0, authorizationChecks)
        assertEquals(0, outgoingOperations)
    }

    @Test
    fun pendingOnboardingAuthorizationKeepsQueuedWorkUntouched() = runTest {
        var outgoingOperations = 0
        val gate = DirectAutomaticOutgoingGate { "Mutual keys are not available" }

        assertFalse(gate.run("contact", hasPendingWork = true) { outgoingOperations++ })
        assertEquals(0, outgoingOperations)
    }

    @Test
    fun establishedAuthorizationReleasesPreviouslyDeferredWorkOnRetry() = runTest {
        var authorized = false
        var outgoingOperations = 0
        val gate = DirectAutomaticOutgoingGate { if (authorized) null else "Missing remote key" }

        assertFalse(gate.run("contact", hasPendingWork = true) { outgoingOperations++ })
        authorized = true // subsequent mutual-identity result or startup reconciliation
        assertTrue(gate.run("contact", hasPendingWork = true) { outgoingOperations++ })
        assertEquals(1, outgoingOperations)
    }

    @Test
    fun pendingIdentityReplacementCannotReleaseOldKeyPackets() = runTest {
        var outgoingOperations = 0
        val gate = DirectAutomaticOutgoingGate { "Identity change pending verification" }

        assertFalse(gate.run("contact", hasPendingWork = true) { outgoingOperations++ })
        assertEquals(0, outgoingOperations)
    }

    @Test
    fun unexpectedAuthorizationFailureIsNotSilenced() = runTest {
        val gate = DirectAutomaticOutgoingGate { throw IllegalStateException("Database unavailable") }

        val failure = assertFailsWith<IllegalStateException> {
            gate.run("contact", hasPendingWork = true) { error("Must not execute") }
        }
        assertEquals("Database unavailable", failure.message)
    }

    @Test
    fun unexpectedOutgoingFailureIsNotSilenced() = runTest {
        val gate = DirectAutomaticOutgoingGate { null }

        val failure = assertFailsWith<IllegalStateException> {
            gate.run("contact", hasPendingWork = true) { error("Outbox unavailable") }
        }
        assertEquals("Outbox unavailable", failure.message)
    }
}
