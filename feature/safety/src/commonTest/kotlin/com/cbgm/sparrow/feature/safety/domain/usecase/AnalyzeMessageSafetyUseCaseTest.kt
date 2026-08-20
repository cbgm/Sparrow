package com.cbgm.sparrow.feature.safety.domain.usecase

import com.cbgm.sparrow.feature.safety.domain.analyzer.MessageSafetyStructuralAnalyzer
import com.cbgm.sparrow.feature.safety.domain.classifier.MessageSafetyClassifier
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyRisk
import com.cbgm.sparrow.feature.safety.domain.resolver.MessageSafetyRiskResolver
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyzeMessageSafetyUseCaseTest {
    @Test
    fun combinesStructuralAndClassifierReasonsBeforeResolvingRisk() = runTest {
        val useCase =
            AnalyzeMessageSafetyUseCase(
                structuralAnalyzer = MessageSafetyStructuralAnalyzer(),
                classifier =
                    object : MessageSafetyClassifier {
                        override suspend fun classify(text: String): Set<MessageSafetyReason> =
                            setOf(MessageSafetyReason.CREDENTIAL_REQUEST)
                    },
                riskResolver = MessageSafetyRiskResolver()
            )

        val result = useCase("Open https://bit.ly/account-check")

        assertEquals(MessageSafetyRisk.HIGH, result.risk)
        assertTrue(MessageSafetyReason.CREDENTIAL_REQUEST in result.reasons)
        assertTrue(MessageSafetyReason.URL_SHORTENER in result.reasons)
        assertTrue(MessageSafetyReason.SUSPICIOUS_LINK in result.reasons)
    }
}
