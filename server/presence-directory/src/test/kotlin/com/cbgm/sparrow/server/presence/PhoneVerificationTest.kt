package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.UnsignedAliasClaim
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
            val claims = verification { error("Must not call Firebase") }.claims(claim(null))
            assertEquals(AliasClaims(provenAlias = null, unprovenClaimsAllowed = false, aliases = listOf(alias)), claims)
            assertEquals(
                AliasClaims(provenAlias = null, unprovenClaimsAllowed = false),
                verification { error("Must not call Firebase") }.claims(null)
            )
        }

    @Test
    fun proofCoversOnlyTheVerifiedPhoneAlias() =
        runTest {
            val otherAlias = ClientRoutingIds.fromPhoneNumber("+491709999999")
            val claims = assertNotNull(verification { phone }.claims(claim("valid", listOf(alias, otherAlias))))
            assertTrue(claims.isProven(alias))
            assertFalse(claims.isProven(otherAlias))
        }

    @Test
    fun invalidProofIsRejected() =
        runTest {
            val verification = verification { error("Expired or invalid JWT") }
            assertNull(verification.claims(claim("invalid")))
            assertNull(verification.claims(claim("")))
        }

    @Test
    fun withoutPhoneVerificationTheFirstClaimWins() =
        runTest {
            val expected = AliasClaims(provenAlias = null, unprovenClaimsAllowed = true, aliases = listOf(alias))
            assertEquals(expected, PhoneVerification(verifier = null).claims(claim("token")))
            assertEquals(expected, createPhoneVerification { null }.claims(claim("token")))
        }

    @Test
    fun malformedVerifiedNumberIsRejected() =
        runTest {
            assertNull(verification { "not-a-phone" }.claims(claim("valid")))
        }

    @Test
    fun keyOnlyRoutingDoesNotClaimAPhoneNumber() =
        runTest {
            assertNotNull(
                verification { error("No phone claimed") }
                    .claims(claim("valid", emptyList()))
            )
        }

    @Test
    fun cancellationIsNotConvertedToAuthorizationFailure() =
        runTest {
            assertFailsWith<CancellationException> {
                verification { throw CancellationException() }.claims(claim("valid"))
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
    fun testNumbersMoveToTheDeviceThatRegistersThemLast() =
        runTest {
            val otherAlias = ClientRoutingIds.fromPhoneNumber("+491709999999")
            val verification =
                createPhoneVerification { name ->
                    when (name) {
                        "GOOGLE_APPLICATION_CREDENTIALS" -> null
                        "PRESENCE_TEST_PHONE_ALIASES" -> " $alias , "
                        else -> null
                    }
                }
            val claims = assertNotNull(verification.claims(claim(null)))

            assertTrue(claims.allows(alias, deviceRoutingId = "device-b", currentOwner = "device-a"))
            assertFalse(claims.allows(otherAlias, deviceRoutingId = "device-b", currentOwner = "device-a"))
        }

    @Test
    fun testNumbersMustBePhoneAliases() {
        // Same vector as server/installer/app/test_phone_numbers.py derives for the installer.
        assertEquals("scphone1_tCZ6IjTiBy-k0ujYn75yfFD8QUcx1uyswu09APBQONE", alias)
        assertEquals(emptySet(), testPhoneAliases(null))
        assertFailsWith<IllegalArgumentException> { testPhoneAliases(phone) }
    }

    @Test
    fun tokensAreRedactedFromClaimStrings() {
        assertFalse(claim("sensitive-token").toString().contains("sensitive-token"))
    }

    private fun verification(phoneNumber: suspend (String) -> String) =
        PhoneVerification(verifier = { token -> VerifiedPhone(phoneNumber(token), Long.MAX_VALUE) })

    private fun claim(token: String?, aliases: List<String> = listOf(alias)) =
        UnsignedAliasClaim(
            routingId = "scrouting1_key",
            clientSigningPublicKey = byteArrayOf(1),
            aliases = aliases,
            phoneVerificationToken = token,
            issuedAtEpochMilliseconds = 1_000L
        )
}
