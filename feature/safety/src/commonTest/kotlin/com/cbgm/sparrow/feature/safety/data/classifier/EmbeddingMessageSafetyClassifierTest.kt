package com.cbgm.sparrow.feature.safety.data.classifier

import com.cbgm.sparrow.core.embedding.data.platform.EmbeddingInputType
import com.cbgm.sparrow.core.embedding.data.platform.LocalTextEmbedder
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmbeddingMessageSafetyClassifierTest {
    @Test
    fun usesSemanticSimilarityInsteadOfLiteralPhrases() = runTest {
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
