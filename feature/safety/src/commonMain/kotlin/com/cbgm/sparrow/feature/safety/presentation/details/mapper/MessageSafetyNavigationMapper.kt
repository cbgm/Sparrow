package com.cbgm.sparrow.feature.safety.presentation.details.mapper

import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUiModel

fun MessageSafetyWarningUiModel.toDetailsRoute(
    messageId: String,
    contactId: String?
): AppRoute.MessageSafetyDetails =
    AppRoute.MessageSafetyDetails(
        messageId = messageId,
        levelId = level.id,
        reasonIds = reasons.joinToString(separator = ",") { reason -> reason.id },
        focusReasonId = reasons.firstOrNull()?.id,
        contactId = contactId
    )
