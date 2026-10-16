package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.ClientRoute
import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.security.ClientRoutingIds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhoneVerificationTest {
    private val phone = "+491701234567"
    private val alias = ClientRoutingIds.fromPhoneNumber(phone)

    @Test fun optionalAllowsSkippingAndMandatoryRejectsSkipping() = runTest {
        val unverified = registration(null)
        assertTrue(PhoneVerification(false) { error("Must not call Firebase") }.accepts(unverified))
        assertFalse(PhoneVerification(true) { error("Must not call Firebase") }.accepts(unverified))
    }

    @Test fun proofMustMatchEveryAdvertisedPhoneAlias() = runTest {
        val verification = PhoneVerification(true) { phone }
        assertTrue(verification.accepts(registration("valid")))
        assertFalse(verification.accepts(registration("valid", listOf(ClientRoutingIds.fromPhoneNumber("+491709999999")))))
        assertFalse(verification.accepts(registration("valid", listOf(alias, "scphone1_other"))))
    }

    @Test fun invalidProofIsRejectedEvenWhenOptional() = runTest {
        for (required in listOf(false, true)) {
            val verification = PhoneVerification(required) { error("Expired or invalid JWT") }
            assertFalse(verification.accepts(registration("invalid")))
            assertFalse(verification.accepts(registration("")))
        }
    }

    @Test fun malformedVerifiedNumberIsRejected() = runTest {
        assertFalse(PhoneVerification(true) { "not-a-phone" }.accepts(registration("valid")))
    }

    @Test fun keyOnlyRoutingDoesNotClaimAPhoneNumber() = runTest {
        assertTrue(PhoneVerification(true) { error("No phone claimed") }.accepts(registration(null, emptyList())))
    }

    @Test fun cancellationIsNotConvertedToAuthorizationFailure() = runTest {
        assertFailsWith<CancellationException> {
            PhoneVerification(true) { throw CancellationException() }.accepts(registration("valid"))
        }
    }

    @Test fun tokensAreRedactedFromRegistrationStrings() {
        assertFalse(registration("sensitive-token").toString().contains("sensitive-token"))
    }

    private fun registration(token: String?, aliases: List<String> = listOf(alias)) = ClientRouteRegistration(
        route = ClientRoute(
            routingId = "scrouting1_key",
            nodeId = "node",
            connectionId = "connection",
            generation = 1L,
            expiresAtEpochMilliseconds = 1000L,
            aliases = aliases,
            clientSignature = byteArrayOf(1)
        ),
        clientSigningPublicKey = byteArrayOf(1),
        phoneVerificationToken = token
    )
}
