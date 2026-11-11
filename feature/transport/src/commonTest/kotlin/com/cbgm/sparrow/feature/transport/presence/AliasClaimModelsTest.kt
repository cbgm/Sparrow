package com.cbgm.sparrow.feature.transport.presence

import com.cbgm.sparrow.feature.transport.gateway.codec.createGatewayJson
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The claim must keep the wire format of `AliasClaim` in `:server:protocol`: the server re-encodes it to check the signature. */
class AliasClaimModelsTest {
    private val json = createGatewayJson()

    @Test
    fun aSignedClaimIsTheUnsignedClaimWithItsSignature() {
        val unsigned =
            UnsignedAliasClaim(
                routingId = "scrouting1_device",
                clientSigningPublicKey = byteArrayOf(1, 2),
                aliases = listOf("scphone1_a"),
                phoneAttestation = "attestation",
                issuedAtEpochMilliseconds = 7L
            )
        val claim =
            AliasClaim(
                routingId = unsigned.routingId,
                clientSigningPublicKey = unsigned.clientSigningPublicKey,
                aliases = unsigned.aliases,
                phoneAttestation = unsigned.phoneAttestation,
                issuedAtEpochMilliseconds = unsigned.issuedAtEpochMilliseconds,
                signature = byteArrayOf(9)
            )

        val unsignedJson = json.encodeToString(unsigned)
        assertEquals(
            """{"routingId":"scrouting1_device","clientSigningPublicKey":[1,2],"aliases":["scphone1_a"],""" +
                """"phoneAttestation":"attestation","issuedAtEpochMilliseconds":7}""",
            unsignedJson
        )
        assertEquals(unsignedJson.dropLast(1) + ""","signature":[9]}""", json.encodeToString(claim))

        val decoded = json.decodeFromString<AliasClaim>(json.encodeToString(claim))
        assertEquals(listOf("scphone1_a"), decoded.aliases)
        assertEquals("attestation", decoded.phoneAttestation)
        assertEquals(7L, decoded.issuedAtEpochMilliseconds)
    }

    @Test
    fun aClaimWithoutAttestationLeavesTheFieldOut() {
        val unsigned = UnsignedAliasClaim("scrouting1_device", byteArrayOf(1), listOf("scphone1_a"), null, 7L)

        assertTrue("phoneAttestation" !in json.encodeToString(unsigned))
    }

    @Test
    fun theRequestCarriesOnlyTheSealedClaim() {
        val request = AliasClaimRequest(byteArrayOf(4, 5))

        assertEquals("""{"sealedClaim":[4,5]}""", json.encodeToString(request))
        assertContentEquals(byteArrayOf(4, 5), json.decodeFromString<AliasClaimRequest>("""{"sealedClaim":[4,5]}""").sealedClaim)
    }
}
