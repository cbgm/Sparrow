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

    @Test
    fun registrationsWithoutProofAreAccepted() =
        runTest {
            assertTrue(verification { error("Must not call Firebase") }.accepts(registration(null)))
        }

    @Test
    fun proofMustMatchEveryAdvertisedPhoneAlias() =
        runTest {
            val verification = verification { phone }
            val otherAlias = ClientRoutingIds.fromPhoneNumber("+491709999999")
            assertTrue(verification.accepts(registration("valid")))
            assertFalse(verification.accepts(registration("valid", listOf(otherAlias))))
            assertFalse(
                verification.accepts(registration("valid", listOf(alias, "scphone1_other")))
            )
        }

    @Test
    fun invalidProofIsRejected() =
        runTest {
            val verification = verification { error("Expired or invalid JWT") }
            assertFalse(verification.accepts(registration("invalid")))
            assertFalse(verification.accepts(registration("")))
        }

    @Test
    fun proofIsIgnoredWhenTheServerCannotVerifyPhones() =
        runTest {
            assertTrue(PhoneVerification(verifier = null).accepts(registration("token")))
            assertTrue(createPhoneVerification { null }.accepts(registration("token")))
        }

    @Test
    fun malformedVerifiedNumberIsRejected() =
        runTest {
            assertFalse(verification { "not-a-phone" }.accepts(registration("valid")))
        }

    @Test
    fun keyOnlyRoutingDoesNotClaimAPhoneNumber() =
        runTest {
            assertTrue(
                verification { error("No phone claimed") }
                    .accepts(registration(null, emptyList()))
            )
        }

    @Test
    fun cancellationIsNotConvertedToAuthorizationFailure() =
        runTest {
            assertFailsWith<CancellationException> {
                verification { throw CancellationException() }.accepts(registration("valid"))
            }
        }

    @Test
    fun tokensAreRedactedFromRegistrationStrings() {
        assertFalse(registration("sensitive-token").toString().contains("sensitive-token"))
    }

    private fun verification(phoneNumber: suspend (String) -> String) =
        PhoneVerification { token -> VerifiedPhone(phoneNumber(token), Long.MAX_VALUE) }

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
