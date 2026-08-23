package com.cbgm.sparrow.feature.safety.presentation.mapper

import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyAssessment
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason
import com.cbgm.sparrow.feature.safety.presentation.model.MessageSafetyWarningLevel
import com.cbgm.sparrow.feature.safety.presentation.model.MessageSafetyWarningReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MessageSafetyUiMapperTest {
    @Test
    fun safeAssessmentHasNoWarning() {
        assertNull(MessageSafetyAssessment.Safe.toWarningUiModel())
    }

    @Test
    fun urgentActionRequestIsVisible() {
        val warning =
            MessageSafetyAssessment(
                reasons = setOf(MessageSafetyReason.URGENT_ACTION_REQUEST)
            ).toWarningUiModel()

        assertEquals(MessageSafetyWarningLevel.SUSPICIOUS, warning?.level)
        assertEquals(listOf(MessageSafetyWarningReason.URGENT_ACTION_REQUEST), warning?.reasons)
    }

    @Test
    fun privateKeyRequestUsesHighVisualSeverity() {
        val warning =
            MessageSafetyAssessment(
                reasons = setOf(MessageSafetyReason.PRIVATE_KEY_REQUEST)
            ).toWarningUiModel()

        assertEquals(MessageSafetyWarningLevel.HIGH, warning?.level)
    }
}
