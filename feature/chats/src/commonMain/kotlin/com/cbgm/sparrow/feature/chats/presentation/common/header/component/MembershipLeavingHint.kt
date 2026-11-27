package com.cbgm.sparrow.feature.chats.presentation.common.header.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowBanner
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.core.ui.theme.warning
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_group_leaving_hint_description
import com.cbgm.sparrow.resources.feature_chats_group_leaving_hint_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MembershipLeavingHint(modifier: Modifier = Modifier) {
    SparrowBanner(
        icon = Icons.AutoMirrored.Filled.Logout,
        accent = MaterialTheme.colorScheme.warning,
        title = stringResource(Res.string.feature_chats_group_leaving_hint_title),
        description = stringResource(Res.string.feature_chats_group_leaving_hint_description),
        modifier = modifier.padding(
            horizontal = MaterialTheme.spacing.small,
            vertical = MaterialTheme.spacing.micro
        )
    )
}

@Preview
@Composable
private fun MembershipLeavingHintPreview() {
    SparrowTheme {
        MembershipLeavingHint()
    }
}
