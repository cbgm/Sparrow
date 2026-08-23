package com.cbgm.sparrow.feature.safety.domain.usecase

import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyAssessment
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason
import com.cbgm.sparrow.feature.safety.domain.repository.MessageSafetyAnalysisRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyzeMessageSafetyUseCaseTest {
    @Test
    fun returnsAssessmentFromAnalysisRepository() = runTest {
        val expectedReasons =
            linkedSetOf(
                MessageSafetyReason.CREDENTIAL_REQUEST,
                MessageSafetyReason.URL_SHORTENER,
                MessageSafetyReason.SUSPICIOUS_LINK
            )
        val useCase = AnalyzeMessageSafetyUseCase(FakeAnalysisRepository(expectedReasons))

        val result = useCase("Open https://bit.ly/account-check")

        assertEquals(MessageSafetyAssessment(expectedReasons), result)
    }

    @Test
    fun returnsSafeAssessmentWhenRepositoryFindsNoReasons() = runTest {
        val useCase = AnalyzeMessageSafetyUseCase(FakeAnalysisRepository(emptySet()))

        assertEquals(MessageSafetyAssessment.Safe, useCase("ordinary message"))
    }
}

private class FakeAnalysisRepository(
    private val reasons: Set<MessageSafetyReason>
) : MessageSafetyAnalysisRepository {
    override suspend fun analyze(text: String): Set<MessageSafetyReason> = reasons
}
