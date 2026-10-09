package com.cbgm.sparrow.feature.expenses.presentation.create.mapper

import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseParticipantUi

internal fun ExpenseParticipantUi.toAvatarTarget(): AvatarTarget? =
    when {
        isLocal -> AvatarTarget.LocalUser
        !avatarContactId.isNullOrBlank() -> AvatarTarget.User(avatarContactId)
        else -> null
    }
