package com.cbgm.sparrow.feature.chats.presentation.group.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_group_deleted_description
import com.cbgm.sparrow.resources.feature_chats_group_deleted_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ConversationDeletedHint(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.screenPadding,
                vertical = MaterialTheme.spacing.small
            )
        ) {
            Text(
                text = stringResource(Res.string.feature_chats_group_deleted_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(Res.string.feature_chats_group_deleted_description),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Preview
@Composable
private fun ConversationDeletedHintPreview() {
    SparrowTheme {
        ConversationDeletedHint()
    }
}
