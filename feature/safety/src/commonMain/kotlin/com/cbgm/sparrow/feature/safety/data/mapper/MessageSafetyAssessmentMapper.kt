package com.cbgm.sparrow.feature.safety.data.mapper

import com.cbgm.sparrow.data.database.entity.MessageSafetyAssessmentEntity
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyAssessment
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyReason

internal fun MessageSafetyAssessmentEntity.toDomain(): MessageSafetyAssessment =
    MessageSafetyAssessment(
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
        legacyRisk = if (reasons.isEmpty()) LEGACY_SAFE_RISK else LEGACY_VISIBLE_RISK,
        reasons = reasons.joinToString(separator = REASON_SEPARATOR) { it.id }
    )

private const val REASON_SEPARATOR = ","
private const val LEGACY_SAFE_RISK = "NONE"
private const val LEGACY_VISIBLE_RISK = "SUSPICIOUS"
