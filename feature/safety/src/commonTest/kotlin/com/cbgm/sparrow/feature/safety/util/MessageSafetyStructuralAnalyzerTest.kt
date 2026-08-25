package com.cbgm.sparrow.feature.safety.util

import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason
import kotlin.test.Test
import kotlin.test.assertTrue

class MessageSafetyStructuralAnalyzerTest {
    private val analyzer = MessageSafetyStructuralAnalyzer()

    @Test
    fun normalHttpsLinkIsNotFlagged() {
        val reasons = analyzer("The documentation is at https://example.com/docs")

        assertTrue(reasons.isEmpty())
    }

    @Test
    fun shortenedLinkIsFlagged() {
        val reasons = analyzer("Open https://bit.ly/account-check")

        assertTrue(MessageSafetyReason.URL_SHORTENER in reasons)
        assertTrue(MessageSafetyReason.SUSPICIOUS_LINK in reasons)
    }

    @Test
    fun mixedScriptDomainIsFlagged() {
        val reasons = analyzer("Open https://pаypal.com now")

        assertTrue(MessageSafetyReason.MIXED_SCRIPT_DOMAIN in reasons)
        assertTrue(MessageSafetyReason.SUSPICIOUS_LINK in reasons)
    }

    @Test
    fun punycodeDomainIsFlaggedAsLookalike() {
        val reasons = analyzer("Open https://xn--paypa-4ve.com now")

        assertTrue(MessageSafetyReason.LOOKALIKE_DOMAIN in reasons)
        assertTrue(MessageSafetyReason.SUSPICIOUS_LINK in reasons)
    }
}
