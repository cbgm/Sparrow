package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.ClientRoute
import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.security.ClientRoutingIds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhoneVerificationTest {
    private val phone = "+491701234567"
    private val alias = ClientRoutingIds.fromPhoneNumber(phone)

    @Test
    fun optionalAllowsSkippingAndMandatoryRejectsSkipping() =
        runTest {
            val unverified = registration(null)
            assertTrue(verification(false) { error("Must not call Firebase") }.accepts(unverified))
            assertFalse(verification(true) { error("Must not call Firebase") }.accepts(unverified))
        }

    @Test
    fun proofMustMatchEveryAdvertisedPhoneAlias() =
        runTest {
            val verification = verification(true) { phone }
            val otherAlias = ClientRoutingIds.fromPhoneNumber("+491709999999")
            assertTrue(verification.accepts(registration("valid")))
            assertFalse(verification.accepts(registration("valid", listOf(otherAlias))))
            assertFalse(
                verification.accepts(registration("valid", listOf(alias, "scphone1_other")))
            )
        }

    @Test
    fun invalidProofIsRejectedEvenWhenOptional() =
        runTest {
            for (required in listOf(false, true)) {
                val verification = verification(required) { error("Expired or invalid JWT") }
                assertFalse(verification.accepts(registration("invalid")))
                assertFalse(verification.accepts(registration("")))
            }
        }

    @Test
    fun proofIsIgnoredWhenTheServerCannotVerifyPhones() =
        runTest {
            assertTrue(PhoneVerification(false, verifier = null).accepts(registration("token")))
            assertFalse(PhoneVerification(true, verifier = null).accepts(registration("token")))
        }

    @Test
    fun policyReportsWhetherVerificationIsAvailable() {
        assertEquals(
            PhoneVerificationPolicy(required = true, available = true),
            verification(true) { phone }.policy
        )
        assertEquals(
            PhoneVerificationPolicy(required = false, available = false),
            PhoneVerification(false, verifier = null).policy
        )
    }

    @Test
    fun environmentEnablesVerificationOnlyWithFirebaseCredentials() {
        val withoutCredentials = mapOf("PHONE_VERIFICATION_REQUIRED" to "true")
        val withCredentials =
            withoutCredentials + ("GOOGLE_APPLICATION_CREDENTIALS" to "/run/firebase.json")

        assertEquals(
            PhoneVerificationPolicy(required = true, available = false),
            createPhoneVerification(withoutCredentials::get).policy
        )
        assertEquals(
            PhoneVerificationPolicy(required = true, available = true),
            createPhoneVerification(withCredentials::get).policy
        )
        assertEquals(
            PhoneVerificationPolicy(required = false, available = false),
            createPhoneVerification { null }.policy
        )
        assertFailsWith<IllegalArgumentException> {
            createPhoneVerification(mapOf("PHONE_VERIFICATION_REQUIRED" to "yes")::get)
        }
    }

    @Test
    fun malformedVerifiedNumberIsRejected() =
        runTest {
            assertFalse(verification(true) { "not-a-phone" }.accepts(registration("valid")))
        }

    @Test
    fun keyOnlyRoutingDoesNotClaimAPhoneNumber() =
        runTest {
            assertTrue(
                verification(true) { error("No phone claimed") }
                    .accepts(registration(null, emptyList()))
            )
        }

    @Test
    fun cancellationIsNotConvertedToAuthorizationFailure() =
        runTest {
            assertFailsWith<CancellationException> {
                verification(true) { throw CancellationException() }.accepts(registration("valid"))
            }
        }

    @Test
    fun tokensAreRedactedFromRegistrationStrings() {
        assertFalse(registration("sensitive-token").toString().contains("sensitive-token"))
    }

    private fun verification(required: Boolean, phoneNumber: suspend (String) -> String) =
        PhoneVerification(required) { token -> VerifiedPhone(phoneNumber(token), Long.MAX_VALUE) }

    private fun registration(token: String?, aliases: List<String> = listOf(alias)) =
        ClientRouteRegistration(
            route =
                ClientRoute(
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
