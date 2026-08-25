package com.cbgm.sparrow.feature.safety.util

import com.cbgm.sparrow.feature.safety.data.model.GeneratedMessageSafetyMlpModel
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason
import kotlin.math.exp

class MessageSafetyClassifier {
    init {
        GeneratedMessageSafetyMlpModel.byReason.values.forEach(::validateHead)
    }

    fun classify(embedding: FloatArray): Set<MessageSafetyReason> {
        require(embedding.size == GeneratedMessageSafetyMlpModel.EMBEDDING_DIMENSIONS) {
            "Embedding has ${embedding.size} dimensions, expected ${GeneratedMessageSafetyMlpModel.EMBEDDING_DIMENSIONS}"
        }

        val reasons = linkedSetOf<MessageSafetyReason>()
        GeneratedMessageSafetyMlpModel.byReason.forEach { (reason, head) ->
            if (probability(embedding, head) >= head.threshold) {
                reasons += reason
            }
        }
        return reasons
    }

    private fun probability(
        embedding: FloatArray,
        head: GeneratedMessageSafetyMlpModel.Head
    ): Float {
        var outputLogit = head.outputBias.toDouble()
        for (hiddenIndex in 0 until head.hiddenSize) {
            var hiddenLogit = head.hiddenBias[hiddenIndex].toDouble()
            for (inputIndex in embedding.indices) {
                hiddenLogit +=
                    embedding[inputIndex].toDouble() *
                    head.hiddenWeights[inputIndex * head.hiddenSize + hiddenIndex].toDouble()
            }
            if (hiddenLogit > 0.0) {
                outputLogit += hiddenLogit * head.outputWeights[hiddenIndex].toDouble()
            }
        }
        return sigmoid(outputLogit).toFloat()
    }

    private fun validateHead(head: GeneratedMessageSafetyMlpModel.Head) {
        check(head.hiddenSize > 0) { "Safety classifier hidden size must be positive" }
        check(head.hiddenWeights.size == GeneratedMessageSafetyMlpModel.EMBEDDING_DIMENSIONS * head.hiddenSize) {
            "Safety classifier hidden weight count does not match embedding/hidden dimensions"
        }
        check(head.hiddenBias.size == head.hiddenSize) {
            "Safety classifier hidden bias count does not match hidden size"
        }
        check(head.outputWeights.size == head.hiddenSize) {
            "Safety classifier output weight count does not match hidden size"
        }
    }

    private fun sigmoid(value: Double): Double =
        if (value >= 0.0) {
            1.0 / (1.0 + exp(-value))
        } else {
            val exponential = exp(value)
            exponential / (1.0 + exponential)
        }
}
