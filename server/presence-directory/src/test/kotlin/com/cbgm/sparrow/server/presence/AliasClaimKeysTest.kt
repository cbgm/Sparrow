package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.ALIAS_CLAIM_CONTEXT
import com.cbgm.sparrow.server.protocol.AliasClaim
import com.cbgm.sparrow.server.protocol.UnsignedAliasClaim
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.security.ClientRoutingIds
import com.cbgm.sparrow.server.security.NodeIdentity
import com.cbgm.sparrow.server.security.Signatures
import com.cbgm.sparrow.server.security.X25519Sealing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AliasClaimKeysTest {
    private val keys = AliasClaimKeys(now = { NOW })

    @Test
    fun aSignedFreshClaimOpens() {
        val device = NodeIdentity.generate()

        assertEquals(listOf(ALIAS), keys.open(sealedClaim(device))?.aliases)
    }

    @Test
    fun claimsSignedByAnotherKeyForAnotherDeviceOrStaleDoNotOpen() {
        val device = NodeIdentity.generate()

        assertNull(keys.open(sealedClaim(device, signer = NodeIdentity.generate())))
        assertNull(keys.open(sealedClaim(device, routingId = ClientRoutingIds.fromSigningPublicKey(ByteArray(32) { 1 }))))
        assertNull(keys.open(sealedClaim(device, issuedAt = NOW - 6L * 60L * 1_000L)))
        assertNull(keys.open(sealedClaim(device, aliases = listOf("scrouting1_not_an_alias"))))
        assertNull(AliasClaimKeys(now = { NOW }).open(sealedClaim(device)))
    }

    private fun sealedClaim(
        device: NodeIdentity,
        signer: NodeIdentity = device,
        routingId: String = ClientRoutingIds.fromSigningPublicKey(device.encodedPublicKey),
        issuedAt: Long = NOW,
        aliases: List<String> = listOf(ALIAS)
    ): ByteArray = sealClaim(device, keys.publicKey, signer, routingId, issuedAt, aliases)

    companion object {
        const val NOW = 1_000_000L
        val ALIAS = ClientRoutingIds.fromPhoneNumber("+491701234567")

        fun sealClaim(
            device: NodeIdentity,
            presenceKey: ByteArray,
            signer: NodeIdentity = device,
            routingId: String = ClientRoutingIds.fromSigningPublicKey(device.encodedPublicKey),
            issuedAt: Long = NOW,
            aliases: List<String> = listOf(ALIAS)
        ): ByteArray {
            val unsigned =
                UnsignedAliasClaim(
                    routingId = routingId,
                    clientSigningPublicKey = device.encodedPublicKey,
                    aliases = aliases,
                    issuedAtEpochMilliseconds = issuedAt
                )
            val claim =
                AliasClaim(
                    routingId = unsigned.routingId,
                    clientSigningPublicKey = unsigned.clientSigningPublicKey,
                    aliases = unsigned.aliases,
                    issuedAtEpochMilliseconds = unsigned.issuedAtEpochMilliseconds,
                    signature = Signatures.sign(serverJson.encodeToString(unsigned).encodeToByteArray(), signer.privateKey)
                )
            return X25519Sealing.seal(serverJson.encodeToString(claim).encodeToByteArray(), presenceKey, ALIAS_CLAIM_CONTEXT)
        }
    }
}
