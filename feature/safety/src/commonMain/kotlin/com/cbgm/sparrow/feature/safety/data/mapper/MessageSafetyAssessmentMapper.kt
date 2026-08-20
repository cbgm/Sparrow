package com.cbgm.sparrow.feature.safety.data.mapper

import com.cbgm.sparrow.data.database.entity.MessageSafetyAssessmentEntity
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyAssessment
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyRisk

internal fun MessageSafetyAssessmentEntity.toDomain(): MessageSafetyAssessment =
    MessageSafetyAssessment(
        risk = MessageSafetyRisk.entries.firstOrNull { it.name == risk } ?: MessageSafetyRisk.NONE,
        reasons =
            reasons
                .split(REASON_SEPARATOR)
                .asSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .mapNotNull(MessageSafetyReason::fromId)
                .toCollection(linkedSetOf())
    )

internal fun MessageSafetyAssessment.toEntity(
    messageId: String,
    analyzerVersion: Int
): MessageSafetyAssessmentEntity =
    MessageSafetyAssessmentEntity(
        messageId = messageId,
        analyzerVersion = analyzerVersion,
        risk = risk.name,
        reasons = reasons.joinToString(separator = REASON_SEPARATOR) { it.id }
    )

private const val REASON_SEPARATOR = ","
