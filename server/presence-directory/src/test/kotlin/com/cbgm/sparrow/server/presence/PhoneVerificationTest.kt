package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.UnsignedAliasClaim
import com.cbgm.sparrow.server.security.ClientRoutingIds
import com.cbgm.sparrow.server.security.NodeIdentity
import com.cbgm.sparrow.server.security.Signatures
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhoneVerificationTest {
    private val alias = ClientRoutingIds.fromPhoneNumber("+491701234567")
    private val directory = NodeIdentity.generate()
    private val verifier = NodeIdentity.generate()

    @Test
    fun anAttestationFromThePhoneVerifierProvesItsAlias() {
        // Signed by server/control-plane-directory/phone_verifier.py with fixed keys.
        val attestations = PhoneAttestations(directoryPublicKey(VECTOR_DIRECTORY_KEY), now = { VECTOR_NOW })

        assertEquals(alias, attestations.provenAlias(VECTOR_ATTESTATION, "scrouting1_device"))
        assertNull(attestations.provenAlias(VECTOR_ATTESTATION, "scrouting1_other"))
        assertNull(PhoneAttestations(directoryPublicKey(VECTOR_DIRECTORY_KEY), now = { VECTOR_NOW + 61L * 60L * 1_000L }).provenAlias(VECTOR_ATTESTATION, "scrouting1_device"))
        assertNull(PhoneAttestations(directory.publicKey, now = { VECTOR_NOW }).provenAlias(VECTOR_ATTESTATION, "scrouting1_device"))
    }

    @Test
    fun theProofCoversOnlyTheAttestedAliasOfThisDevice() {
        val otherAlias = ClientRoutingIds.fromPhoneNumber("+491709999999")
        val claims = assertNotNull(verification().claims(claim(attestation(), listOf(alias, otherAlias))))

        assertTrue(claims.isProven(alias))
        assertFalse(claims.isProven(otherAlias))
        assertNull(verification().claims(claim(attestation(routingId = "scrouting1_other"))))
    }

    @Test
    fun attestationsNotCertifiedByThePinnedDirectoryKeyAreRefused() {
        val selfCertified = attestation(certifier = verifier)

        assertNull(verification().claims(claim(selfCertified)))
        assertNull(PhoneVerification(PhoneAttestations(directoryPublicKey = null)).claims(claim(attestation())))
        assertNull(verification().claims(claim("not an attestation")))
    }

    @Test
    fun claimsWithoutAnAttestationProveNothing() {
        assertEquals(AliasClaims(provenAlias = null, aliases = listOf(alias)), verification().claims(claim(null)))
        assertEquals(AliasClaims(provenAlias = null), verification().claims(null))
        assertNotNull(verification().claims(claim(attestation(), emptyList())))
    }

    @Test
    fun boundAliasesStayWithTheirDeviceUnlessProven() {
        val unproven = AliasClaims(provenAlias = null)
        val proven = AliasClaims(provenAlias = alias)

        assertFalse(unproven.allows(alias, deviceRoutingId = "device-a", currentOwner = null))
        assertTrue(unproven.allows(alias, deviceRoutingId = "device-a", currentOwner = "device-a"))
        assertFalse(unproven.allows(alias, deviceRoutingId = "device-b", currentOwner = "device-a"))
        assertTrue(proven.allows(alias, deviceRoutingId = "device-a", currentOwner = null))
        assertTrue(proven.allows(alias, deviceRoutingId = "device-b", currentOwner = "device-a"))
    }

    @Test
    fun testNumbersMoveToTheDeviceThatRegistersThemLast() {
        val otherAlias = ClientRoutingIds.fromPhoneNumber("+491709999999")
        val verification =
            createPhoneVerification { name ->
                when (name) {
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
        assertFailsWith<IllegalArgumentException> { testPhoneAliases("+491701234567") }
    }

    @Test
    fun attestationsAreRedactedFromClaimStrings() {
        assertFalse(claim("sensitive-attestation").toString().contains("sensitive-attestation"))
    }

    private fun verification() = PhoneVerification(PhoneAttestations(directory.publicKey, now = { NOW }))

    /** What phone_verifier.py answers, signed here with test keys. */
    private fun attestation(
        routingId: String = ROUTING_ID,
        certifier: NodeIdentity = directory
    ): String {
        val keys = """{"attestationKey":"${encode(verifier.encodedPublicKey)}","protocol":"sparrow-phone-keys-v1","requestKey":"${encode(ByteArray(32))}"}"""
        val payload =
            """{"alias":"$alias","expiresAtEpochMilliseconds":${NOW + 60_000L},"issuedAtEpochMilliseconds":$NOW,""" +
                """"protocol":"sparrow-phone-attestation-v1","routingId":"$routingId"}"""
        return """{"keys":${signed(keys, "sparrow-phone-keys-v1\n", certifier)},""" +
            signed(payload, "sparrow-phone-attestation-v1\n", verifier).removePrefix("{")
    }

    private fun signed(
        json: String,
        domain: String,
        signer: NodeIdentity
    ): String {
        val signature = Signatures.sign((domain + json).encodeToByteArray(), signer.privateKey)
        return """{"payload":"${encode(json.encodeToByteArray())}","signature":"${encode(signature)}"}"""
    }

    private fun encode(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun claim(
        attestation: String?,
        aliases: List<String> = listOf(alias)
    ) = UnsignedAliasClaim(
        routingId = ROUTING_ID,
        clientSigningPublicKey = byteArrayOf(1),
        aliases = aliases,
        phoneAttestation = attestation,
        issuedAtEpochMilliseconds = 1_000L
    )

    private companion object {
        const val ROUTING_ID = "scrouting1_key"
        const val NOW = 1_800_000_000_000L
        const val VECTOR_NOW = 1_800_000_000_000L
        const val VECTOR_DIRECTORY_KEY = "MCowBQYDK2VwAyEAiojj3XQJ8ZX9UtstPLpdcspnCb8dlBIb83SIAbQPb1w"
        const val VECTOR_ATTESTATION =
            """{"keys":{"payload":"eyJhdHRlc3RhdGlvbktleSI6Ik1Db3dCUVlESzJWd0F5RUFnVGwzRHFoOUYxOVdvMVJtdzB4LXpNdU5pcEcwN2plaVhmWVBXNF9KczVRIiwicHJvdG9jb2wiOiJzcGFycm93LXBob25lLWtleXMtdjEiLCJyZXF1ZXN0S2V5IjoiWGY3ZE8ydlVmMi1panVGZGxwMWJzT3BUZDAxSWk5cjUzeHh1QVNTejd5SSJ9","signature":"sbifl_sr71SPyoMoED_UmZSxbnHiwb6pxdFmqBpgF-5XJ2vTUKmkZrO9m7UyBbCjP9F_jQyDK2GyEgvfT7YEBA"},""" +
                """"payload":"eyJhbGlhcyI6InNjcGhvbmUxX3RDWjZJalRpQnktazB1alluNzV5ZkZEOFFVY3gxdXlzd3UwOUFQQlFPTkUiLCJleHBpcmVzQXRFcG9jaE1pbGxpc2Vjb25kcyI6MTgwMDAwMzYwMDAwMCwiaXNzdWVkQXRFcG9jaE1pbGxpc2Vjb25kcyI6MTgwMDAwMDAwMDAwMCwicHJvdG9jb2wiOiJzcGFycm93LXBob25lLWF0dGVzdGF0aW9uLXYxIiwicm91dGluZ0lkIjoic2Nyb3V0aW5nMV9kZXZpY2UifQ",""" +
                """"signature":"7LiNnZXQ-hOVax1Zse4PmE8UHIbDP0LoBsxMsLlzhEvHVZvLGBoWNMHjLprs1v7xntTW4ljlo0ab9Fi5-eOxDA"}"""
    }
}
