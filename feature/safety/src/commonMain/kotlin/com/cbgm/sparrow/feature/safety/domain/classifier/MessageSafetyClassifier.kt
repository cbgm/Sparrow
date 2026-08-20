package com.cbgm.sparrow.feature.safety.domain.classifier

import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason

interface MessageSafetyClassifier {
    suspend fun classify(text: String): Set<MessageSafetyReason>
}
