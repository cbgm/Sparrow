package com.cbgm.sparrow.feature.safety.data.classifier

import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageSafetyClassifierPolicyTest {
    @Test
    fun thresholdRequiresBothSimilarityAndContrast() {
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
