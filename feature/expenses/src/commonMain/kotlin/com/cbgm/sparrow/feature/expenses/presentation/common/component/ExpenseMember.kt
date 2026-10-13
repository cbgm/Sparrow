package com.cbgm.sparrow.feature.expenses.presentation.common.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.feature.avatar.presentation.component.SparrowAvatar
import com.cbgm.sparrow.feature.expenses.presentation.create.mapper.toAvatarTarget
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseParticipantUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_group_unknown_member
import com.cbgm.sparrow.resources.feature_expenses_you
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ExpenseMemberAvatar(
    member: ExpenseParticipantUi,
    size: Dp = Dimens.GroupConversationScreen.avatarSize
) {
    SparrowAvatar(
        name = member.label(),
        target = if (LocalInspectionMode.current) null else member.toAvatarTarget(),
        size = size
    )
}

@Composable
internal fun ExpenseParticipantUi.label(): String =
    when {
        isLocal -> stringResource(Res.string.feature_expenses_you)
        displayName.isNotBlank() -> displayName
        else -> stringResource(Res.string.feature_chats_group_unknown_member)
    }
