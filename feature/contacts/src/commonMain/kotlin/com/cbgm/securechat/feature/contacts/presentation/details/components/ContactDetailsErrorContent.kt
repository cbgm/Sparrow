package com.cbgm.securechat.feature.contacts.presentation.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_retry
import com.cbgm.securechat.resources.feature_contacts_could_not_load_contact
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ContactDetailsErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text(
                text = stringResource(Res.string.feature_contacts_could_not_load_contact),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            SecureChatApprovalButton(
                onClick = onRetry,
                text = stringResource(Res.string.base_retry)
            )
        }
    }
}

@Preview
@Composable
private fun ContactDetailsErrorContentPreview() {
    SecureChatTheme {
        ContactDetailsErrorContent(
            message = "Contact could not be loaded",
            onRetry = {},
            modifier = Modifier.padding(24.dp)
        )
    }
}
