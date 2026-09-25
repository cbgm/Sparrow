package com.cbgm.sparrow.feature.identity.domain.model

import com.cbgm.sparrow.feature.identity.data.model.IdentityExchangeStage
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class IdentityHandshakeInputTest {
    @Test
    fun offerDefensivelyCopiesIncomingIdentityKeysAndChallenge() {
        val challenge = byteArrayOf(1, 2)
        val encryptionKey = byteArrayOf(3, 4)
        val signingKey = byteArrayOf(5, 6)
        val offer = IdentityExchangeOffer(
            exchangeId = "exchange-1",
            createdAtEpochMilliseconds = 10L,
            expiresAtEpochMilliseconds = 20L,
            inviteChallenge = challenge,
            encryptionPublicKey = encryptionKey,
            signingPublicKey = signingKey
        )
        challenge[0] = 9
        encryptionKey[0] = 9
        signingKey[0] = 9
        assertContentEquals(byteArrayOf(1, 2), offer.inviteChallenge)
        assertContentEquals(byteArrayOf(3, 4), offer.encryptionPublicKey)
        assertContentEquals(byteArrayOf(5, 6), offer.signingPublicKey)
    }

    @Test
    fun invalidationKeepsLegacyRoomStageForExistingInstallations() {
        assertEquals("AUTHORIZATION_REVOKED", IdentityExchangeStage.EXCHANGE_INVALIDATED.persistedValue)
    }
}
