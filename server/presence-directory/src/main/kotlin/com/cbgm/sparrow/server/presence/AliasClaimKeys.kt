package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.ALIAS_CLAIM_CONTEXT
import com.cbgm.sparrow.server.protocol.AliasClaim
import com.cbgm.sparrow.server.protocol.UnsignedAliasClaim
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.protocol.unsigned
import com.cbgm.sparrow.server.security.ClientRoutingIds
import com.cbgm.sparrow.server.security.Signatures
import com.cbgm.sparrow.server.security.X25519KeyPair
import kotlin.math.absoluteValue

/**
 * The presence directory's key for alias claims ([AliasClaim]). It lives only in memory: a restart replaces it, and
 * apps pick up the new one with the next signed node directory ([com.cbgm.sparrow.server.protocol.NodeDirectory]).
 */
internal class AliasClaimKeys(
    private val keys: X25519KeyPair = X25519KeyPair.generate(),
    private val now: () -> Long = System::currentTimeMillis
) {
    val publicKey: ByteArray get() = keys.publicKey

    /**
     * The claim when it was sealed for this key, signed by the key its routing ID derives from and issued within
     * [MAXIMUM_CLAIM_AGE_MILLISECONDS]; otherwise null. A replayed claim only renews what its device claimed.
     */
    fun open(sealedClaim: ByteArray): UnsignedAliasClaim? {
        val claim =
            keys.open(sealedClaim, ALIAS_CLAIM_CONTEXT)
                ?.let { plaintext -> runCatching { serverJson.decodeFromString<AliasClaim>(plaintext.decodeToString()) } }
                ?.getOrNull()
                ?: return null
        val ownKey = ClientRoutingIds.matchesSigningPublicKey(claim.routingId, claim.clientSigningPublicKey)
        val fresh = (now() - claim.issuedAtEpochMilliseconds).absoluteValue <= MAXIMUM_CLAIM_AGE_MILLISECONDS
        val signed =
            runCatching {
                Signatures.verify(
                    content = serverJson.encodeToString(claim.unsigned()).encodeToByteArray(),
                    signature = claim.signature,
                    publicKey = Signatures.decodePublicKey(claim.clientSigningPublicKey)
                )
            }.getOrDefault(false)
        val validAliases =
            claim.aliases.all(ClientRoutingIds::isBootstrapRoutingId) && claim.aliases.distinct() == claim.aliases
        return claim.unsigned().takeIf { ownKey && fresh && signed && validAliases }
    }

    private companion object {
        const val MAXIMUM_CLAIM_AGE_MILLISECONDS = 5L * 60L * 1_000L
    }
}
