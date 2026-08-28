package com.cbgm.sparrow.feature.safety.presentation.details.mapper

import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyAssessment
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningLevel
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MessageSafetyUiMapperTest {
    @Test
    fun safeAssessmentHasNoWarning() {
        assertNull(MessageSafetyAssessment.Safe.toMessageSafetyWarningUi())
    }

    @Test
    fun urgentActionRequestIsVisible() {
        val warning =
            MessageSafetyAssessment(
                reasons = setOf(MessageSafetyReason.URGENT_ACTION_REQUEST)
            ).toMessageSafetyWarningUi()

        assertEquals(MessageSafetyWarningLevel.SUSPICIOUS, warning?.level)
        assertEquals(listOf(MessageSafetyWarningReason.URGENT_ACTION_REQUEST), warning?.reasons)
    }

    @Test
    fun privateKeyRequestUsesHighVisualSeverity() {
        val warning =
            MessageSafetyAssessment(
                reasons = setOf(MessageSafetyReason.PRIVATE_KEY_REQUEST)
            ).toMessageSafetyWarningUi()

        assertEquals(MessageSafetyWarningLevel.HIGH, warning?.level)
    }
}
