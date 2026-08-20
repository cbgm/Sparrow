package com.cbgm.sparrow.feature.safety.domain.usecase

import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyRisk
import com.cbgm.sparrow.feature.safety.domain.rule.MessageSafetyRuleEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyzeMessageSafetyUseCaseTest {
    private val analyze = AnalyzeMessageSafetyUseCase(MessageSafetyRuleEngine())

    @Test
    fun normalHttpsLinkIsNotFlagged() {
        val result = analyze("The documentation is at https://example.com/docs")

        assertEquals(MessageSafetyRisk.NONE, result.risk)
        assertTrue(result.reasons.isEmpty())
    }

    @Test
    fun shortenedLinkIsSuspicious() {
        val result = analyze("Open https://bit.ly/account-check")

        assertEquals(MessageSafetyRisk.SUSPICIOUS, result.risk)
        assertTrue(MessageSafetyReason.URL_SHORTENER in result.reasons)
        assertTrue(MessageSafetyReason.SUSPICIOUS_LINK in result.reasons)
    }

    @Test
    fun ipAddressCredentialLinkIsHighRisk() {
        val result =
            analyze(
                "Verify immediately: enter your password at http://192.168.1.10/login"
            )

        assertEquals(MessageSafetyRisk.HIGH, result.risk)
        assertTrue(MessageSafetyReason.IP_ADDRESS_LINK in result.reasons)
        assertTrue(MessageSafetyReason.CREDENTIAL_REQUEST in result.reasons)
        assertTrue(MessageSafetyReason.URGENT_ACTION_REQUEST in result.reasons)
        assertTrue(MessageSafetyReason.SUSPICIOUS_LINK in result.reasons)
    }

    @Test
    fun urgentPaymentRequestIsHighRisk() {
        val result = analyze("Bitte überweise das Geld sofort")

        assertEquals(MessageSafetyRisk.HIGH, result.risk)
        assertTrue(MessageSafetyReason.PAYMENT_REQUEST in result.reasons)
        assertTrue(MessageSafetyReason.URGENT_ACTION_REQUEST in result.reasons)
    }

    @Test
    fun privateKeyRequestIsHighRisk() {
        val result = analyze("Please send me your recovery phrase")

        assertEquals(MessageSafetyRisk.HIGH, result.risk)
        assertTrue(MessageSafetyReason.PRIVATE_KEY_REQUEST in result.reasons)
    }

    @Test
    fun safetyAdviceDoesNotFlagRecoveryPhrase() {
        val result = analyze("Never share your recovery phrase with anyone")

        assertEquals(MessageSafetyRisk.NONE, result.risk)
        assertTrue(result.reasons.isEmpty())
    }

    @Test
    fun mixedScriptDomainIsFlagged() {
        val result = analyze("Open https://pаypal.com now")

        assertTrue(MessageSafetyReason.MIXED_SCRIPT_DOMAIN in result.reasons)
        assertTrue(MessageSafetyReason.SUSPICIOUS_LINK in result.reasons)
    }

    @Test
    fun punycodeDomainIsFlaggedAsLookalike() {
        val result = analyze("Open https://xn--paypa-4ve.com now")

        assertTrue(MessageSafetyReason.LOOKALIKE_DOMAIN in result.reasons)
        assertTrue(MessageSafetyReason.SUSPICIOUS_LINK in result.reasons)
    }
}
