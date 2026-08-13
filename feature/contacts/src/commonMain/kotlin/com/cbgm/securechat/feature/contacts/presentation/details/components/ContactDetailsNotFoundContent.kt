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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_contacts_contact_not_found
import com.cbgm.securechat.resources.feature_contacts_return_to_contacts
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ContactDetailsNotFoundContent(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text(
                text = stringResource(Res.string.feature_contacts_contact_not_found),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            SecureChatApprovalButton(
                onClick = onBack,
                text = stringResource(Res.string.feature_contacts_return_to_contacts)
            )
        }
    }
}

@Preview
@Composable
private fun ContactDetailsNotFoundContentPreview() {
    SecureChatTheme {
        ContactDetailsNotFoundContent(
            onBack = {},
            modifier = Modifier.padding(24.dp)
        )
    }
}
