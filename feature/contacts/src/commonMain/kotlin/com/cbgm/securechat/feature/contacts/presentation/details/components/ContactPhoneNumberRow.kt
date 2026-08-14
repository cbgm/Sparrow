package com.cbgm.securechat.feature.contacts.presentation.details.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumber
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_custom
import com.cbgm.securechat.resources.base_home
import com.cbgm.securechat.resources.base_main
import com.cbgm.securechat.resources.base_mobile
import com.cbgm.securechat.resources.base_other
import com.cbgm.securechat.resources.base_preferred
import com.cbgm.securechat.resources.base_work
import com.cbgm.securechat.resources.base_work_mobile
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ContactPhoneNumberRow(
    phoneNumber: ContactPhoneNumber,
    isPreferred: Boolean
) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        },
        headlineContent = {
            Text(text = phoneNumber.value, style = MaterialTheme.typography.bodyMedium)
        },
        supportingContent = {
            Text(
                text = phoneNumber.displayLabel(),
                style = MaterialTheme.typography.labelMedium
            )
        },
        trailingContent = {
            if (isPreferred) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = stringResource(Res.string.base_preferred),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        colors =
            ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = MaterialTheme.colorScheme.onBackground,
                supportingColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
    )
}

@Composable
private fun ContactPhoneNumber.displayLabel(): String =
    label?.takeIf(String::isNotBlank)
        ?: stringResource(
            when (type) {
                ContactPhoneNumberType.MOBILE -> Res.string.base_mobile
                ContactPhoneNumberType.WORK_MOBILE -> Res.string.base_work_mobile
                ContactPhoneNumberType.HOME -> Res.string.base_home
                ContactPhoneNumberType.WORK -> Res.string.base_work
                ContactPhoneNumberType.MAIN -> Res.string.base_main
                ContactPhoneNumberType.CUSTOM -> Res.string.base_custom
                ContactPhoneNumberType.OTHER -> Res.string.base_other
            }
        )

@Preview
@Composable
private fun ContactPhoneNumberRowPreview() {
    SecureChatTheme {
        ContactPhoneNumberRow(
            phoneNumber = ContactDetailsPreviewData.phoneNumber,
            isPreferred = true
        )
    }
}
