package com.cbgm.sparrow.feature.safety.data.classifier

import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason

internal data class MessageSafetyClassifierThreshold(
    val minimumSimilarity: Float,
    val minimumContrastMargin: Float
) {
    init {
        require(minimumSimilarity in -1f..1f) {
            "minimumSimilarity must be a cosine-similarity value"
        }
        require(minimumContrastMargin in 0f..2f) {
            "minimumContrastMargin must be a valid cosine-similarity margin"
        }
    }

    fun matches(
        positiveSimilarity: Float,
        negativeSimilarity: Float
    ): Boolean =
        positiveSimilarity >= minimumSimilarity &&
            positiveSimilarity - negativeSimilarity >= minimumContrastMargin
}

internal object MessageSafetyClassifierPolicy {
    fun threshold(reason: MessageSafetyReason): MessageSafetyClassifierThreshold =
        when (reason) {
            MessageSafetyReason.URGENT_ACTION_REQUEST ->
                MessageSafetyClassifierThreshold(
                    minimumSimilarity = 0.60f,
                    minimumContrastMargin = 0.05f
                )

            MessageSafetyReason.CREDENTIAL_REQUEST ->
                MessageSafetyClassifierThreshold(
                    minimumSimilarity = 0.60f,
                    minimumContrastMargin = 0.05f
                )

            MessageSafetyReason.PAYMENT_REQUEST ->
                MessageSafetyClassifierThreshold(
                    minimumSimilarity = 0.61f,
                    minimumContrastMargin = 0.05f
                )

            MessageSafetyReason.PRIVATE_KEY_REQUEST ->
                MessageSafetyClassifierThreshold(
                    minimumSimilarity = 0.58f,
                    minimumContrastMargin = 0.05f
                )

            MessageSafetyReason.SUSPICIOUS_LINK,
            MessageSafetyReason.LOOKALIKE_DOMAIN,
            MessageSafetyReason.MIXED_SCRIPT_DOMAIN,
            MessageSafetyReason.IP_ADDRESS_LINK,
            MessageSafetyReason.URL_SHORTENER ->
                error("Structural safety reasons do not use semantic classifier thresholds")
        }
}
