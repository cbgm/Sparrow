package com.cbgm.sparrow.feature.chats.presentation.common.history.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.chats.domain.model.group.ChatMessageType
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_group_change_dropped_message
import com.cbgm.sparrow.resources.feature_chats_group_member_added_message
import com.cbgm.sparrow.resources.feature_chats_group_member_left_message
import com.cbgm.sparrow.resources.feature_chats_group_member_removed_message
import com.cbgm.sparrow.resources.feature_chats_group_unknown_member
import com.cbgm.sparrow.resources.feature_chats_group_you_left_message
import com.cbgm.sparrow.resources.feature_chats_group_you_were_removed_message
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MembershipSystemMessage(
    type: ChatMessageType,
    memberName: String?,
    modifier: Modifier = Modifier
) {
    val text = getSystemMessage(type, memberName)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = MaterialTheme.spacing.small),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun resolvedMemberName(memberName: String?): String =
    memberName?.takeIf(String::isNotBlank) ?: stringResource(Res.string.feature_chats_group_unknown_member)

@Composable
private fun getSystemMessage(
    type: ChatMessageType,
    memberName: String?
) = when (type) {
    ChatMessageType.GROUP_MEMBER_ADDED ->
        stringResource(Res.string.feature_chats_group_member_added_message, resolvedMemberName(memberName))

    ChatMessageType.GROUP_MEMBER_REMOVED ->
        stringResource(Res.string.feature_chats_group_member_removed_message, resolvedMemberName(memberName))

    ChatMessageType.LOCAL_GROUP_MEMBERSHIP_REMOVED ->
        stringResource(Res.string.feature_chats_group_you_were_removed_message)

    ChatMessageType.GROUP_MEMBER_LEFT ->
        stringResource(Res.string.feature_chats_group_member_left_message, resolvedMemberName(memberName))

    ChatMessageType.LOCAL_GROUP_MEMBERSHIP_LEFT ->
        stringResource(Res.string.feature_chats_group_you_left_message)

    ChatMessageType.LOCAL_GROUP_CHANGE_DROPPED ->
        stringResource(Res.string.feature_chats_group_change_dropped_message)

    ChatMessageType.USER -> ""
}

@Preview
@Composable
private fun MembershipSystemMessageAddedPreview() {
    SparrowTheme {
        MembershipSystemMessage(
            type = ChatMessageType.GROUP_MEMBER_ADDED,
            memberName = "Alex"
        )
    }
}

@Preview
@Composable
private fun MembershipSystemMessagePreview() {
    SparrowTheme {
        MembershipSystemMessage(
            type = ChatMessageType.GROUP_MEMBER_REMOVED,
            memberName = "Alex"
        )
    }
}

@Preview
@Composable
private fun MembershipSystemMessageLeftPreview() {
    SparrowTheme {
        MembershipSystemMessage(
            type = ChatMessageType.GROUP_MEMBER_LEFT,
            memberName = "Alex"
        )
    }
}
