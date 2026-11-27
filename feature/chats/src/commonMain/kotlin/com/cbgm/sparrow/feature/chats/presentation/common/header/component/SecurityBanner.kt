package com.cbgm.sparrow.feature.chats.presentation.common.header.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.cbgm.sparrow.core.ui.component.SparrowBanner
import com.cbgm.sparrow.core.ui.component.SparrowBannerAction
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
import com.cbgm.sparrow.resources.feature_chats_chat_verified_keys_description
import com.cbgm.sparrow.resources.feature_chats_identity_not_fully_mutual_title
import com.cbgm.sparrow.resources.feature_chats_manual_identity_incomplete_description
import com.cbgm.sparrow.resources.feature_chats_manual_identity_required_description
import com.cbgm.sparrow.resources.feature_chats_manual_identity_required_title
import com.cbgm.sparrow.resources.feature_chats_manual_identity_setup_action
import com.cbgm.sparrow.resources.feature_chats_reconnect
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
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (securityState == ContactSecurityState.MUTUAL_KEYS_VERIFIED && isChatAuthorized) {
        return
    }

    val state = securityState(securityState) ?: return

    SparrowBanner(
        icon = state.icon,
        accent = state.accent,
        title = state.title,
        description = state.description,
        actions = securityActions(
            securityState = securityState,
            onVerifyIdentity = onVerifyIdentity,
            onManualIdentitySetup = onManualIdentitySetup,
            onReconnect = onReconnect
        ),
        modifier = modifier.padding(
            horizontal = MaterialTheme.spacing.small,
            vertical = MaterialTheme.spacing.micro
        )
    )
}

@Composable
private fun securityActions(
    securityState: ContactSecurityState,
    onVerifyIdentity: () -> Unit,
    onManualIdentitySetup: () -> Unit,
    onReconnect: () -> Unit
): List<SparrowBannerAction> {
    val setUp = SparrowBannerAction(
        text = stringResource(Res.string.feature_chats_manual_identity_setup_action),
        onClick = onManualIdentitySetup
    )
    val reconnect = SparrowBannerAction(
        text = stringResource(Res.string.feature_chats_reconnect),
        onClick = onReconnect
    )
    val verify = SparrowBannerAction(
        text = stringResource(Res.string.base_verify),
        onClick = onVerifyIdentity
    )
    // Identity import is available even when automatic sharing is enabled:
    // the other person may have chosen manual identity sharing.
    return when (securityState) {
        ContactSecurityState.NO_REMOTE_PUBLIC_KEYS,
        ContactSecurityState.ONE_WAY_KEYS -> listOf(setUp, reconnect)
        ContactSecurityState.LOCAL_IDENTITY_SHARED -> listOf(setUp)
        ContactSecurityState.MUTUAL_KEYS_UNVERIFIED,
        ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_CONTACT -> listOf(verify)
        ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_ME,
        ContactSecurityState.MUTUAL_KEYS_VERIFIED -> emptyList()
    }
}

@Composable
private fun securityState(securityState: ContactSecurityState): SecurityBannerState? =
    when (securityState) {
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
            SecurityBannerState(
                icon = Icons.Default.Security,
                title = stringResource(Res.string.feature_chats_chat_unverified_title),
                description = stringResource(Res.string.feature_chats_chat_unverified_description),
                accent = MaterialTheme.colorScheme.primary
            )

        ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_ME ->
            SecurityBannerState(
                icon = Icons.Default.Schedule,
                title = stringResource(Res.string.feature_chats_chat_verified_by_me_title),
                description = stringResource(Res.string.feature_chats_chat_verified_by_me_description),
                accent = MaterialTheme.colorScheme.tertiary
            )

        ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_CONTACT ->
            SecurityBannerState(
                icon = Icons.Default.Security,
                title = stringResource(Res.string.feature_chats_chat_verified_by_contact_title),
                description = stringResource(Res.string.feature_chats_chat_verified_by_contact_description),
                accent = MaterialTheme.colorScheme.tertiary
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
        accent = MaterialTheme.colorScheme.error
    )
