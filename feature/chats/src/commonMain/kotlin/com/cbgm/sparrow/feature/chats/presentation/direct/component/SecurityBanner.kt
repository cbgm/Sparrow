package com.cbgm.sparrow.feature.chats.presentation.direct.component

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
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.chats.domain.model.direct.ContactSecurityState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_verify
import com.cbgm.sparrow.resources.feature_chats_chat_key_exchange_incomplete_description
import com.cbgm.sparrow.resources.feature_chats_chat_key_exchange_incomplete_title
import com.cbgm.sparrow.resources.feature_chats_chat_no_keys_description
import com.cbgm.sparrow.resources.feature_chats_chat_one_way_keys_description
import com.cbgm.sparrow.resources.feature_chats_chat_unencrypted_description
import com.cbgm.sparrow.resources.feature_chats_chat_unencrypted_title
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
import com.cbgm.sparrow.resources.feature_chats_manual_identity_incomplete_description
import com.cbgm.sparrow.resources.feature_chats_manual_identity_incomplete_title
import com.cbgm.sparrow.resources.feature_chats_manual_identity_required_description
import com.cbgm.sparrow.resources.feature_chats_manual_identity_required_title
import com.cbgm.sparrow.resources.feature_chats_manual_identity_setup_action
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun securityDescription(securityState: ContactSecurityState): String =
    when (securityState) {
        ContactSecurityState.NO_REMOTE_PUBLIC_KEYS -> stringResource(Res.string.feature_chats_chat_no_keys_description)
        ContactSecurityState.ONE_WAY_KEYS -> stringResource(Res.string.feature_chats_chat_one_way_keys_description)
        ContactSecurityState.MUTUAL_KEYS_UNVERIFIED -> stringResource(Res.string.feature_chats_chat_unverified_keys_description)
        ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_ME -> stringResource(Res.string.feature_chats_chat_verified_by_me_keys_description)
        ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_CONTACT -> stringResource(Res.string.feature_chats_chat_verified_by_contact_keys_description)
        ContactSecurityState.MUTUAL_KEYS_VERIFIED -> stringResource(Res.string.feature_chats_chat_verified_keys_description)
    }

@Composable
internal fun SecurityBanner(
    securityState: ContactSecurityState,
    identitySetupMode: DirectIdentitySetupMode,
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
        securityState(
            securityState = securityState,
            identitySetupMode = identitySetupMode
        ) ?: return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = state.containerColor,
        contentColor = state.contentColor
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.small,
                vertical = MaterialTheme.spacing.directScreen.securityBannerVerticalPadding
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = state.icon,
                contentDescription = null,
                modifier = Modifier.size(Dimens.DirectScreen.invitationIconSize)
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
                    modifier = Modifier.padding(top = MaterialTheme.spacing.directScreen.securityDescriptionTopPadding),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            SecurityAction(
                securityState = securityState,
                identitySetupMode = identitySetupMode,
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
    identitySetupMode: DirectIdentitySetupMode,
    contentColor: Color,
    onVerifyIdentity: () -> Unit,
    onManualIdentitySetup: () -> Unit
) {
    when {
        identitySetupMode == DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING &&
            securityState in setOf(
                ContactSecurityState.NO_REMOTE_PUBLIC_KEYS,
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
private fun securityState(
    securityState: ContactSecurityState,
    identitySetupMode: DirectIdentitySetupMode
): SecurityBannerState? {
    val isManualSetup = identitySetupMode == DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING

    return when (securityState) {
        ContactSecurityState.NO_REMOTE_PUBLIC_KEYS ->
            errorBanner(
                icon = Icons.Default.LockOpen,
                title = stringResource(
                    if (isManualSetup) {
                        Res.string.feature_chats_manual_identity_required_title
                    } else {
                        Res.string.feature_chats_chat_unencrypted_title
                    }
                ),
                description = stringResource(
                    if (isManualSetup) {
                        Res.string.feature_chats_manual_identity_required_description
                    } else {
                        Res.string.feature_chats_chat_unencrypted_description
                    }
                )
            )

        ContactSecurityState.ONE_WAY_KEYS ->
            errorBanner(
                icon = Icons.Default.LockOpen,
                title = stringResource(
                    if (isManualSetup) {
                        Res.string.feature_chats_manual_identity_incomplete_title
                    } else {
                        Res.string.feature_chats_chat_key_exchange_incomplete_title
                    }
                ),
                description = stringResource(
                    if (isManualSetup) {
                        Res.string.feature_chats_manual_identity_incomplete_description
                    } else {
                        Res.string.feature_chats_chat_key_exchange_incomplete_description
                    }
                )
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
                vertical = MaterialTheme.spacing.directScreen.verifiedBannerVerticalPadding
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(Dimens.DirectScreen.statusIconSize),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.directScreen.verifiedContentGap))
            Text(
                text = stringResource(Res.string.feature_chats_chat_verified_e2ee),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

private data class SecurityBannerState(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val containerColor: Color,
    val contentColor: Color
)
