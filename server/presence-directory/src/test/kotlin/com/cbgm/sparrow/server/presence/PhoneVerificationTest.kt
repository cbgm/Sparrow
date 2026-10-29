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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhoneVerificationTest {
    private val phone = "+491701234567"
    private val alias = ClientRoutingIds.fromPhoneNumber(phone)

    @Test
    fun registrationsWithoutProofProveNothing() =
        runTest {
            val claims = verification { error("Must not call Firebase") }.claims(registration(null))
            assertEquals(AliasClaims(provenAlias = null, unprovenClaimsAllowed = false), claims)
        }

    @Test
    fun proofCoversOnlyTheVerifiedPhoneAlias() =
        runTest {
            val otherAlias = ClientRoutingIds.fromPhoneNumber("+491709999999")
            val claims = assertNotNull(verification { phone }.claims(registration("valid", listOf(alias, otherAlias))))
            assertTrue(claims.isProven(alias))
            assertFalse(claims.isProven(otherAlias))
        }

    @Test
    fun invalidProofIsRejected() =
        runTest {
            val verification = verification { error("Expired or invalid JWT") }
            assertNull(verification.claims(registration("invalid")))
            assertNull(verification.claims(registration("")))
        }

    @Test
    fun withoutPhoneVerificationTheFirstClaimWins() =
        runTest {
            val expected = AliasClaims(provenAlias = null, unprovenClaimsAllowed = true)
            assertEquals(expected, PhoneVerification(verifier = null).claims(registration("token")))
            assertEquals(expected, createPhoneVerification { null }.claims(registration("token")))
        }

    @Test
    fun malformedVerifiedNumberIsRejected() =
        runTest {
            assertNull(verification { "not-a-phone" }.claims(registration("valid")))
        }

    @Test
    fun keyOnlyRoutingDoesNotClaimAPhoneNumber() =
        runTest {
            assertNotNull(
                verification { error("No phone claimed") }
                    .claims(registration("valid", emptyList()))
            )
        }

    @Test
    fun cancellationIsNotConvertedToAuthorizationFailure() =
        runTest {
            assertFailsWith<CancellationException> {
                verification { throw CancellationException() }.claims(registration("valid"))
            }
        }

    @Test
    fun boundAliasesStayWithTheirDeviceUnlessProven() {
        val unproven = AliasClaims(provenAlias = null, unprovenClaimsAllowed = true)
        val proven = AliasClaims(provenAlias = alias, unprovenClaimsAllowed = false)

        assertTrue(unproven.allows(alias, deviceRoutingId = "device-a", currentOwner = null))
        assertTrue(unproven.allows(alias, deviceRoutingId = "device-a", currentOwner = "device-a"))
        assertFalse(unproven.allows(alias, deviceRoutingId = "device-b", currentOwner = "device-a"))
        assertTrue(proven.allows(alias, deviceRoutingId = "device-b", currentOwner = "device-a"))
        assertFalse(
            AliasClaims(provenAlias = null, unprovenClaimsAllowed = false)
                .allows(alias, deviceRoutingId = "device-a", currentOwner = null)
        )
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
