package com.cbgm.securechat.feature.onboarding.presentation.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.onboarding.presentation.pages.component.ListingRow
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_contacts
import com.cbgm.securechat.resources.base_notifications
import com.cbgm.securechat.resources.base_permissions
import com.cbgm.securechat.resources.base_phone_number
import com.cbgm.securechat.resources.feature_onboarding_allow_and_continue
import com.cbgm.securechat.resources.feature_onboarding_camera
import com.cbgm.securechat.resources.feature_onboarding_camera_description
import com.cbgm.securechat.resources.feature_onboarding_contacts_permission_description
import com.cbgm.securechat.resources.feature_onboarding_notifications_description
import com.cbgm.securechat.resources.feature_onboarding_permissions_description
import com.cbgm.securechat.resources.feature_onboarding_permissions_settings_hint
import com.cbgm.securechat.resources.feature_onboarding_phone_number_permission_description
import org.jetbrains.compose.resources.stringResource

@Composable
fun PermissionsPage(onRequestPermissions: () -> Unit) {
    Column(
        Modifier.padding(MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.base_permissions),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(MaterialTheme.spacing.base))
        Text(
            text = stringResource(Res.string.feature_onboarding_permissions_description),
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .74f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        ListingRow(
            index = "01",
            title = stringResource(Res.string.base_notifications),
            description = stringResource(Res.string.feature_onboarding_notifications_description)
        )
        ListingRow(
            index = "02",
            title = stringResource(Res.string.base_contacts),
            description = stringResource(Res.string.feature_onboarding_contacts_permission_description)
        )
        ListingRow(
            index = "03",
            title = stringResource(Res.string.feature_onboarding_camera),
            description = stringResource(Res.string.feature_onboarding_camera_description)
        )
        ListingRow(
            index = "04",
            title = stringResource(Res.string.base_phone_number),
            description = stringResource(Res.string.feature_onboarding_phone_number_permission_description)
        )
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        SecureChatApprovalButton(
            onClick = onRequestPermissions,
            text = stringResource(Res.string.feature_onboarding_allow_and_continue)
        )
        Spacer(Modifier.height(MaterialTheme.spacing.base))
        Text(
            text = stringResource(Res.string.feature_onboarding_permissions_settings_hint),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = .58f),
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
private fun PermissionsPagePreview() {
    SecureChatTheme {
        PermissionsPage { }
    }
}
