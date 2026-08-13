package com.cbgm.securechat.feature.contacts.presentation.screen.details.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_securechat
import com.cbgm.securechat.resources.feature_contacts_securechat_keys_attach_later
import com.cbgm.securechat.resources.feature_contacts_securechat_not_enabled
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NoSecureChatIdentityContent() {
    SectionTitle(
        icon = Icons.Default.Security,
        title = stringResource(Res.string.base_securechat)
    )
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
    Text(
        text = stringResource(Res.string.feature_contacts_securechat_not_enabled),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.error
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(Res.string.feature_contacts_securechat_keys_attach_later),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    )
}

@Preview
@Composable
private fun NoSecureChatIdentityContentPreview() {
    SecureChatTheme {
        NoSecureChatIdentityContent()
    }
}
