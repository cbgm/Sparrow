package com.cbgm.sparrow.feature.safety.domain.usecase

import com.cbgm.sparrow.core.embedding.data.platform.EmbeddingInputType
import com.cbgm.sparrow.core.embedding.data.platform.LocalTextEmbedder
import com.cbgm.sparrow.feature.safety.domain.analyzer.MessageSafetyStructuralAnalyzer
import com.cbgm.sparrow.feature.safety.domain.classifier.EmbeddingMessageSafetyClassifier
import com.cbgm.sparrow.feature.safety.domain.classifier.MessageSafetyClassifierPolicy
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyRisk
import com.cbgm.sparrow.feature.safety.domain.resolver.MessageSafetyRiskResolver
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnalyzeMessageSafetyUseCaseTest {
    private val structuralAnalyzer = MessageSafetyStructuralAnalyzer()
    private val riskResolver = MessageSafetyRiskResolver()

    @Test
    fun normalHttpsLinkIsNotFlaggedStructurally() {
        val reasons = structuralAnalyzer("The documentation is at https://example.com/docs")
        assertTrue(reasons.isEmpty())
    }

    @Test
    fun shortenedLinkIsSuspicious() {
        val result = riskResolver(structuralAnalyzer("Open https://bit.ly/account-check"))

        assertEquals(MessageSafetyRisk.SUSPICIOUS, result.risk)
        assertTrue(MessageSafetyReason.URL_SHORTENER in result.reasons)
        assertTrue(MessageSafetyReason.SUSPICIOUS_LINK in result.reasons)
    }

    @Test
    fun mixedScriptDomainIsFlagged() {
        val reasons = structuralAnalyzer("Open https://pаypal.com now")

        assertTrue(MessageSafetyReason.MIXED_SCRIPT_DOMAIN in reasons)
        assertTrue(MessageSafetyReason.SUSPICIOUS_LINK in reasons)
    }

    @Test
    fun punycodeDomainIsFlaggedAsLookalike() {
        val reasons = structuralAnalyzer("Open https://xn--paypa-4ve.com now")

        assertTrue(MessageSafetyReason.LOOKALIKE_DOMAIN in reasons)
        assertTrue(MessageSafetyReason.SUSPICIOUS_LINK in reasons)
    }

    @Test
    fun credentialPlusSuspiciousLinkIsHighRisk() {
        val result =
            riskResolver(
                setOf(
                    MessageSafetyReason.CREDENTIAL_REQUEST,
                    MessageSafetyReason.SUSPICIOUS_LINK,
                    MessageSafetyReason.IP_ADDRESS_LINK
                )
            )

        assertEquals(MessageSafetyRisk.HIGH, result.risk)
    }

    @Test
    fun urgentPaymentRequestIsHighRisk() {
        val result =
            riskResolver(
                setOf(
                    MessageSafetyReason.PAYMENT_REQUEST,
                    MessageSafetyReason.URGENT_ACTION_REQUEST
                )
            )

        assertEquals(MessageSafetyRisk.HIGH, result.risk)
    }

    @Test
    fun prototypeClassifierUsesSemanticSimilarityInsteadOfLiteralPhrases() = runTest {
        val classifier = EmbeddingMessageSafetyClassifier(FakeSemanticEmbedder())

        val credentialReasons = classifier.classify("Could you confirm the secret used to access your account?")
        val paymentReasons = classifier.classify("I need you to settle an unusual financial request.")

        assertTrue(MessageSafetyReason.CREDENTIAL_REQUEST in credentialReasons)
        assertTrue(MessageSafetyReason.PAYMENT_REQUEST in paymentReasons)
    }

    @Test
    fun securityAdviceDoesNotBecomeCredentialOrPrivateKeyRequest() = runTest {
        val classifier = EmbeddingMessageSafetyClassifier(FakeSemanticEmbedder())

        val credentialAdvice = classifier.classify("Never share your password or verification code with anyone.")
        val walletAdvice = classifier.classify("Keep your recovery phrase private and never send it in a chat.")

        assertFalse(MessageSafetyReason.CREDENTIAL_REQUEST in credentialAdvice)
        assertFalse(MessageSafetyReason.PRIVATE_KEY_REQUEST in walletAdvice)
    }

    @Test
    fun normalPaymentAndDeadlineDiscussionDoesNotBecomeFraudIntent() = runTest {
        val classifier = EmbeddingMessageSafetyClassifier(FakeSemanticEmbedder())

        val paymentDiscussion = classifier.classify("I already paid the invoice yesterday and kept the receipt.")
        val deadlineReminder = classifier.classify("Reminder: our appointment is tomorrow at ten.")

        assertFalse(MessageSafetyReason.PAYMENT_REQUEST in paymentDiscussion)
        assertFalse(MessageSafetyReason.URGENT_ACTION_REQUEST in deadlineReminder)
    }

    @Test
    fun reasonSpecificThresholdRequiresBothSimilarityAndContrast() {
        val credentialThreshold = MessageSafetyClassifierPolicy.threshold(MessageSafetyReason.CREDENTIAL_REQUEST)
        val paymentThreshold = MessageSafetyClassifierPolicy.threshold(MessageSafetyReason.PAYMENT_REQUEST)

        assertTrue(
            credentialThreshold.matches(
                positiveSimilarity = credentialThreshold.minimumSimilarity + 0.02f,
                negativeSimilarity = credentialThreshold.minimumSimilarity - 0.04f
            )
        )
        assertFalse(
            credentialThreshold.matches(
                positiveSimilarity = credentialThreshold.minimumSimilarity + 0.02f,
                negativeSimilarity = credentialThreshold.minimumSimilarity - 0.01f
            )
        )
        assertFalse(
            paymentThreshold.matches(
                positiveSimilarity = paymentThreshold.minimumSimilarity - 0.01f,
                negativeSimilarity = 0f
            )
        )
    }
}

private class FakeSemanticEmbedder : LocalTextEmbedder {
    override suspend fun embed(
        text: String,
        inputType: EmbeddingInputType
    ): FloatArray {
        val normalized = text.lowercase()
        return when {
            "security or account conversation" in normalized ||
                "sicherheits- oder kontounterhaltung" in normalized -> vector(4)
            "security or educational conversation" in normalized ||
                "sicherheits- oder lernunterhaltung" in normalized -> vector(6)
            "normal conversation about a price" in normalized ||
                "normale unterhaltung über preis" in normalized -> vector(5)
            "normal reminder or conversation" in normalized ||
                "normale erinnerung oder unterhaltung" in normalized -> vector(7)
            "never share your password" in normalized || "verification code with anyone" in normalized -> vector(4)
            "recovery phrase private" in normalized || "never send it in a chat" in normalized -> vector(6)
            "already paid the invoice" in normalized || "kept the receipt" in normalized -> vector(5)
            "appointment is tomorrow" in normalized || "reminder:" in normalized -> vector(7)
            "password" in normalized || "pin" in normalized || "account credential" in normalized ||
                "secret used to access your account" in normalized -> vector(0)
            "send or transfer money" in normalized || "gift cards" in normalized ||
                "financial request" in normalized || "unusual financial transaction" in normalized -> vector(1)
            "private key" in normalized || "recovery phrase" in normalized ||
                "cryptographic secret" in normalized || "wallet seed" in normalized -> vector(2)
            "act immediately" in normalized || "severe deadline" in normalized ||
                "under pressure" in normalized || "something bad will happen" in normalized -> vector(3)
            else -> vector(8)
        }
    }

    override fun close() = Unit

    private fun vector(index: Int): FloatArray =
        FloatArray(128).also { values -> values[index] = 1f }
}
