package com.cbgm.sparrow.feature.chats.presentation.common.header.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.chats.domain.model.direct.ContactSecurityState
import com.cbgm.sparrow.feature.chats.presentation.common.header.model.SecurityBannerState
import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentitySetupMode
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_verify
import com.cbgm.sparrow.resources.feature_chats_chat_no_keys_description
import com.cbgm.sparrow.resources.feature_chats_chat_one_way_keys_description
import com.cbgm.sparrow.resources.feature_chats_chat_unverified_description
import com.cbgm.sparrow.resources.feature_chats_chat_unverified_keys_description
import com.cbgm.sparrow.resources.feature_chats_chat_unverified_title
import com.cbgm.sparrow.resources.feature_chats_chat_verified_by_contact_description
import com.cbgm.sparrow.resources.feature_chats_chat_verified_by_contact_keys_description
import com.cbgm.sparrow.resources.feature_chats_chat_verified_by_contact_title
import com.cbgm.sparrow.resources.feature_chats_chat_verified_by_me_description
import com.cbgm.sparrow.resources.feature_chats_chat_verified_by_me_keys_description
import com.cbgm.sparrow.resources.feature_chats_chat_verified_by_me_title
import com.cbgm.sparrow.resources.feature_chats_chat_verified_e2ee
import com.cbgm.sparrow.resources.feature_chats_chat_verified_keys_description
import com.cbgm.sparrow.resources.feature_chats_identity_not_fully_mutual_title
import com.cbgm.sparrow.resources.feature_chats_manual_identity_incomplete_description
import com.cbgm.sparrow.resources.feature_chats_manual_identity_required_description
import com.cbgm.sparrow.resources.feature_chats_manual_identity_required_title
import com.cbgm.sparrow.resources.feature_chats_manual_identity_setup_action
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun securityDescription(securityState: ContactSecurityState): String =
    when (securityState) {
        ContactSecurityState.NO_REMOTE_PUBLIC_KEYS -> stringResource(Res.string.feature_chats_chat_no_keys_description)
        ContactSecurityState.LOCAL_IDENTITY_SHARED -> stringResource(Res.string.feature_chats_manual_identity_incomplete_description)
        ContactSecurityState.ONE_WAY_KEYS -> stringResource(Res.string.feature_chats_chat_one_way_keys_description)
        ContactSecurityState.MUTUAL_KEYS_UNVERIFIED -> stringResource(Res.string.feature_chats_chat_unverified_keys_description)
        ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_ME -> stringResource(Res.string.feature_chats_chat_verified_by_me_keys_description)
        ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_CONTACT -> stringResource(Res.string.feature_chats_chat_verified_by_contact_keys_description)
        ContactSecurityState.MUTUAL_KEYS_VERIFIED -> stringResource(Res.string.feature_chats_chat_verified_keys_description)
    }

@Composable
internal fun SecurityBanner(
    securityState: ContactSecurityState,
    @Suppress("UNUSED_PARAMETER") identitySetupMode: DirectIdentitySetupMode,
    isChatAuthorized: Boolean,
    onVerifyIdentity: () -> Unit,
    onManualIdentitySetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (securityState == ContactSecurityState.MUTUAL_KEYS_VERIFIED && isChatAuthorized) {
        VerifiedSecurityIndicator(modifier = modifier)
        return
    }

    val state =
        securityState(securityState) ?: return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = state.containerColor,
        contentColor = state.contentColor
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.small,
                vertical = MaterialTheme.spacing.directConversationScreen.securityBannerVerticalPadding
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = state.icon,
                contentDescription = null,
                modifier = Modifier.size(Dimens.DirectConversationScreen.authorizationIconSize)
            )
            Column(
                modifier = Modifier.padding(start = MaterialTheme.spacing.small).weight(1f)
            ) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = state.description,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.directConversationScreen.securityDescriptionTopPadding),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            SecurityAction(
                securityState = securityState,
                contentColor = state.contentColor,
                onVerifyIdentity = onVerifyIdentity,
                onManualIdentitySetup = onManualIdentitySetup
            )
        }
    }
}

@Composable
private fun SecurityAction(
    securityState: ContactSecurityState,
    contentColor: Color,
    onVerifyIdentity: () -> Unit,
    onManualIdentitySetup: () -> Unit
) {
    when {
        // Identity import is available even when automatic sharing is enabled:
        // the other person may have chosen manual identity sharing.
        securityState in setOf(
            ContactSecurityState.NO_REMOTE_PUBLIC_KEYS,
            ContactSecurityState.LOCAL_IDENTITY_SHARED,
            ContactSecurityState.ONE_WAY_KEYS
        ) -> {
            TextButton(onClick = onManualIdentitySetup) {
                Text(
                    text = stringResource(Res.string.feature_chats_manual_identity_setup_action),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        securityState == ContactSecurityState.MUTUAL_KEYS_UNVERIFIED ||
            securityState == ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_CONTACT -> {
            TextButton(onClick = onVerifyIdentity) {
                Text(
                    text = stringResource(Res.string.base_verify),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun securityState(securityState: ContactSecurityState): SecurityBannerState? = when (securityState) {
    ContactSecurityState.NO_REMOTE_PUBLIC_KEYS ->
        errorBanner(
            icon = Icons.Default.LockOpen,
            title = stringResource(Res.string.feature_chats_manual_identity_required_title),
            description = stringResource(Res.string.feature_chats_manual_identity_required_description)
        )

    ContactSecurityState.LOCAL_IDENTITY_SHARED,
    ContactSecurityState.ONE_WAY_KEYS ->
        errorBanner(
            icon = Icons.Default.LockOpen,
            title = stringResource(Res.string.feature_chats_identity_not_fully_mutual_title),
            description = stringResource(Res.string.feature_chats_manual_identity_incomplete_description)
        )

    ContactSecurityState.MUTUAL_KEYS_UNVERIFIED ->
        errorBanner(
            title = stringResource(Res.string.feature_chats_chat_unverified_title),
            description = stringResource(Res.string.feature_chats_chat_unverified_description)
        )

    ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_ME ->
        SecurityBannerState(
            icon = Icons.Default.Schedule,
            title = stringResource(Res.string.feature_chats_chat_verified_by_me_title),
            description = stringResource(Res.string.feature_chats_chat_verified_by_me_description),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )

    ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_CONTACT ->
        SecurityBannerState(
            icon = Icons.Default.Security,
            title = stringResource(Res.string.feature_chats_chat_verified_by_contact_title),
            description = stringResource(Res.string.feature_chats_chat_verified_by_contact_description),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )

    ContactSecurityState.MUTUAL_KEYS_VERIFIED -> null
}

@Composable
private fun errorBanner(
    title: String,
    description: String,
    icon: ImageVector = Icons.Default.Warning
) =
    SecurityBannerState(
        icon = icon,
        title = title,
        description = description,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    )

@Composable
private fun VerifiedSecurityIndicator(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = Alpha.DirectScreen.securityBanner),
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.small,
                vertical = MaterialTheme.spacing.directConversationScreen.verifiedBannerVerticalPadding
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(Dimens.DirectConversationScreen.statusIconSize),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.directConversationScreen.verifiedContentGap))
            Text(
                text = stringResource(Res.string.feature_chats_chat_verified_e2ee),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}
