package com.cbgm.securechat.feature.contacts.presentation.screen.details.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.ui.component.SecureChatCardNoAnimation
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing

@Composable
internal fun ContactDetailsSectionCard(content: @Composable () -> Unit) {
    SecureChatCardNoAnimation {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            content()
        }
    }
}

@Preview
@Composable
private fun ContactDetailsSectionCardPreview() {
    SecureChatTheme {
        ContactDetailsSectionCard {
            Text(text = "Contact details")
        }
    }
}
