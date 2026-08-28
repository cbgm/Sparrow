package com.cbgm.sparrow.feature.settings.presentation.overview

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.embedding.domain.model.LocalEmbeddingModelState
import com.cbgm.sparrow.core.embedding.domain.model.LocalEmbeddingState
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.locale.AppLanguage
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyState
import com.cbgm.sparrow.feature.search.domain.model.SemanticSearchState
import com.cbgm.sparrow.feature.settings.domain.model.BuildInfo
import com.cbgm.sparrow.feature.settings.presentation.overview.components.LanguagePickerDialog
import com.cbgm.sparrow.feature.settings.presentation.overview.model.SettingsUiEvent
import com.cbgm.sparrow.feature.settings.presentation.overview.model.SettingsUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_developer
import com.cbgm.sparrow.resources.base_language
import com.cbgm.sparrow.resources.base_version
import com.cbgm.sparrow.resources.feature_attachments_storage
import com.cbgm.sparrow.resources.feature_attachments_storage_subtitle
import com.cbgm.sparrow.resources.feature_settings_about
import com.cbgm.sparrow.resources.feature_settings_automatic_secure_setup
import com.cbgm.sparrow.resources.feature_settings_automatic_secure_setup_disabled_subtitle
import com.cbgm.sparrow.resources.feature_settings_automatic_secure_setup_enabled_subtitle
import com.cbgm.sparrow.resources.feature_settings_block_unknown_invites
import com.cbgm.sparrow.resources.feature_settings_block_unknown_invites_subtitle
import com.cbgm.sparrow.resources.feature_settings_blocked_contacts
import com.cbgm.sparrow.resources.feature_settings_blocked_contacts_count
import com.cbgm.sparrow.resources.feature_settings_control_planes
import com.cbgm.sparrow.resources.feature_settings_control_planes_settings_subtitle
import com.cbgm.sparrow.resources.feature_settings_data_disclaimer
import com.cbgm.sparrow.resources.feature_settings_data_disclaimer_subtitle
import com.cbgm.sparrow.resources.feature_settings_developer_menu
import com.cbgm.sparrow.resources.feature_settings_developer_menu_subtitle
import com.cbgm.sparrow.resources.feature_settings_general
import com.cbgm.sparrow.resources.feature_settings_licenses_subtitle
import com.cbgm.sparrow.resources.feature_settings_local_intelligence
import com.cbgm.sparrow.resources.feature_settings_message_safety
import com.cbgm.sparrow.resources.feature_settings_message_safety_analyzing
import com.cbgm.sparrow.resources.feature_settings_message_safety_downloading
import com.cbgm.sparrow.resources.feature_settings_message_safety_failed
import com.cbgm.sparrow.resources.feature_settings_message_safety_ready
import com.cbgm.sparrow.resources.feature_settings_message_safety_subtitle
import com.cbgm.sparrow.resources.feature_settings_network
import com.cbgm.sparrow.resources.feature_settings_open_source_licenses
import com.cbgm.sparrow.resources.feature_settings_privacy_and_data
import com.cbgm.sparrow.resources.feature_settings_privacy_policy
import com.cbgm.sparrow.resources.feature_settings_privacy_policy_subtitle
import com.cbgm.sparrow.resources.feature_settings_profile
import com.cbgm.sparrow.resources.feature_settings_profile_picture
import com.cbgm.sparrow.resources.feature_settings_profile_subtitle
import com.cbgm.sparrow.resources.feature_settings_security
import com.cbgm.sparrow.resources.feature_settings_semantic_search
import com.cbgm.sparrow.resources.feature_settings_semantic_search_building
import com.cbgm.sparrow.resources.feature_settings_semantic_search_downloading
import com.cbgm.sparrow.resources.feature_settings_semantic_search_failed
import com.cbgm.sparrow.resources.feature_settings_semantic_search_ready
import com.cbgm.sparrow.resources.feature_settings_semantic_search_subtitle
import com.cbgm.sparrow.resources.feature_settings_storage
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    onUiEvent: (SettingsUiEvent) -> Unit,
    scrollState: ScrollState,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding(),
                    start = MaterialTheme.spacing.screenPadding,
                    end = MaterialTheme.spacing.screenPadding
                ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        SettingsSection(title = stringResource(Res.string.feature_settings_general)) {
            SettingsRow(
                icon = Icons.Default.Language,
                title = stringResource(Res.string.base_language),
                subtitle = uiState.currentLanguage.nativeName,
                onClick = { onUiEvent(SettingsUiEvent.LanguagePickerOpened) },
                showChevron = false
            )
        }

        SettingsSection(title = stringResource(Res.string.feature_settings_profile)) {
            SettingsRow(
                icon = Icons.Default.Person,
                title = stringResource(Res.string.feature_settings_profile_picture),
                subtitle = stringResource(Res.string.feature_settings_profile_subtitle),
                onClick = { onUiEvent(SettingsUiEvent.ProfileClicked) }
            )
        }

        SettingsSection(title = stringResource(Res.string.feature_settings_network)) {
            SettingsRow(
                icon = Icons.Default.Cloud,
                title = stringResource(Res.string.feature_settings_control_planes),
                subtitle = stringResource(Res.string.feature_settings_control_planes_settings_subtitle),
                onClick = { onUiEvent(SettingsUiEvent.ControlPlanesClicked) }
            )
        }

        SettingsSection(title = stringResource(Res.string.feature_settings_security)) {
            SettingsSwitchRow(
                icon = Icons.Default.Lock,
                title = stringResource(Res.string.feature_settings_automatic_secure_setup),
                subtitle =
                    if (uiState.directIdentitySetupMode == DirectIdentitySetupMode.AUTOMATIC_INVITATION) {
                        stringResource(Res.string.feature_settings_automatic_secure_setup_enabled_subtitle)
                    } else {
                        stringResource(Res.string.feature_settings_automatic_secure_setup_disabled_subtitle)
                    },
                checked = uiState.directIdentitySetupMode == DirectIdentitySetupMode.AUTOMATIC_INVITATION,
                onCheckedChange = { enabled ->
                    onUiEvent(
                        SettingsUiEvent.DirectIdentitySetupModeChanged(
                            if (enabled) {
                                DirectIdentitySetupMode.AUTOMATIC_INVITATION
                            } else {
                                DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING
                            }
                        )
                    )
                }
            )

            SettingsDivider()

            SettingsSwitchRow(
                icon = Icons.Default.PersonOff,
                title = stringResource(Res.string.feature_settings_block_unknown_invites),
                subtitle = stringResource(Res.string.feature_settings_block_unknown_invites_subtitle),
                checked = uiState.blockUnknownContactInvites,
                onCheckedChange = { enabled ->
                    onUiEvent(SettingsUiEvent.BlockUnknownContactInvitesChanged(enabled))
                }
            )

            SettingsDivider()

            SettingsRow(
                icon = Icons.Default.Block,
                title = stringResource(Res.string.feature_settings_blocked_contacts),
                subtitle =
                    stringResource(
                        Res.string.feature_settings_blocked_contacts_count,
                        uiState.blockedContactCount
                    ),
                onClick = { onUiEvent(SettingsUiEvent.BlockedContactsClicked) }
            )
        }

        SettingsSection(title = stringResource(Res.string.feature_settings_local_intelligence)) {
            SettingsSwitchRow(
                icon = Icons.Default.Search,
                title = stringResource(Res.string.feature_settings_semantic_search),
                subtitle = semanticSearchSubtitle(uiState.semanticSearchState),
                checked = uiState.localEmbeddingState.semanticSearchEnabled,
                onCheckedChange = { enabled ->
                    onUiEvent(SettingsUiEvent.SemanticSearchEnabledChanged(enabled))
                }
            )

            SettingsDivider()

            SettingsSwitchRow(
                icon = Icons.Default.Security,
                title = stringResource(Res.string.feature_settings_message_safety),
                subtitle = messageSafetySubtitle(uiState.localEmbeddingState, uiState.messageSafetyState),
                checked = uiState.localEmbeddingState.messageSafetyEnabled,
                onCheckedChange = { enabled ->
                    onUiEvent(SettingsUiEvent.MessageSafetyEnabledChanged(enabled))
                }
            )
        }

        SettingsSection(title = stringResource(Res.string.feature_settings_storage)) {
            SettingsRow(
                icon = Icons.Default.Folder,
                title = stringResource(Res.string.feature_attachments_storage),
                subtitle = stringResource(Res.string.feature_attachments_storage_subtitle),
                onClick = { onUiEvent(SettingsUiEvent.AttachmentStorageClicked) }
            )
        }

        SettingsSection(title = stringResource(Res.string.feature_settings_privacy_and_data)) {
            SettingsRow(
                icon = Icons.Default.PrivacyTip,
                title = stringResource(Res.string.feature_settings_privacy_policy),
                subtitle = stringResource(Res.string.feature_settings_privacy_policy_subtitle),
                onClick = { onUiEvent(SettingsUiEvent.PrivacyPolicyClicked) }
            )

            SettingsDivider()

            SettingsRow(
                icon = Icons.Default.Lock,
                title = stringResource(Res.string.feature_settings_data_disclaimer),
                subtitle = stringResource(Res.string.feature_settings_data_disclaimer_subtitle),
                onClick = { onUiEvent(SettingsUiEvent.DataDisclaimerClicked) }
            )
        }

        SettingsSection(title = stringResource(Res.string.feature_settings_about)) {
            SettingsRow(
                icon = Icons.Default.Code,
                title = stringResource(Res.string.feature_settings_open_source_licenses),
                subtitle = stringResource(Res.string.feature_settings_licenses_subtitle),
                onClick = { onUiEvent(SettingsUiEvent.LicensesClicked) }
            )

            SettingsDivider()

            SettingsRow(
                icon = Icons.Default.Description,
                title = stringResource(Res.string.base_version),
                subtitle = "${uiState.buildInfo.versionName} (${uiState.buildInfo.versionCode})",
                showChevron = false,
                onClick = { onUiEvent(SettingsUiEvent.VersionRowTapped) }
            )
        }

        if (uiState.isDeveloperModeEnabled) {
            SettingsSection(title = stringResource(Res.string.base_developer)) {
                SettingsRow(
                    icon = Icons.Default.BugReport,
                    title = stringResource(Res.string.feature_settings_developer_menu),
                    subtitle = stringResource(Res.string.feature_settings_developer_menu_subtitle),
                    onClick = { onUiEvent(SettingsUiEvent.DeveloperMenuClicked) },
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))
    }

    if (uiState.showLanguagePicker) {
        LanguagePickerDialog(
            currentLanguage = uiState.currentLanguage,
            onLanguageSelected = { language ->
                onUiEvent(SettingsUiEvent.LanguageSelected(language))
            },
            onDismiss = { onUiEvent(SettingsUiEvent.LanguagePickerDismissed) }
        )
    }
}

@Composable
private fun semanticSearchSubtitle(state: SemanticSearchState): String =
    when (state) {
        SemanticSearchState.Disabled,
        SemanticSearchState.Preparing -> stringResource(Res.string.feature_settings_semantic_search_subtitle)
        is SemanticSearchState.DownloadingModel -> {
            val percent = state.progress?.let { (it * 100).toInt().coerceIn(0, 100) }
            if (percent == null) {
                stringResource(Res.string.feature_settings_semantic_search_downloading)
            } else {
                stringResource(Res.string.feature_settings_semantic_search_downloading) + " $percent%"
            }
        }
        is SemanticSearchState.BuildingIndex ->
            stringResource(
                Res.string.feature_settings_semantic_search_building,
                state.processed,
                state.total
            )
        SemanticSearchState.Ready -> stringResource(Res.string.feature_settings_semantic_search_ready)
        is SemanticSearchState.Failed -> stringResource(Res.string.feature_settings_semantic_search_failed)
    }

@Composable
private fun messageSafetySubtitle(
    localEmbeddingState: LocalEmbeddingState,
    messageSafetyState: MessageSafetyState
): String {
    if (!localEmbeddingState.messageSafetyEnabled) {
        return stringResource(Res.string.feature_settings_message_safety_subtitle)
    }

    return when (val modelState = localEmbeddingState.modelState) {
        LocalEmbeddingModelState.NotNeeded,
        LocalEmbeddingModelState.Preparing -> stringResource(Res.string.feature_settings_message_safety_subtitle)
        is LocalEmbeddingModelState.Downloading -> {
            val percent = modelState.progress?.let { (it * 100).toInt().coerceIn(0, 100) }
            if (percent == null) {
                stringResource(Res.string.feature_settings_message_safety_downloading)
            } else {
                stringResource(Res.string.feature_settings_message_safety_downloading) + " $percent%"
            }
        }
        is LocalEmbeddingModelState.Failed -> stringResource(Res.string.feature_settings_message_safety_failed)
        LocalEmbeddingModelState.Ready ->
            when (messageSafetyState) {
                MessageSafetyState.Disabled,
                MessageSafetyState.Preparing -> stringResource(Res.string.feature_settings_message_safety_subtitle)
                MessageSafetyState.Analyzing -> stringResource(Res.string.feature_settings_message_safety_analyzing)
                MessageSafetyState.Ready -> stringResource(Res.string.feature_settings_message_safety_ready)
                is MessageSafetyState.Failed -> stringResource(Res.string.feature_settings_message_safety_failed)
            }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText),
            fontWeight = FontWeight.SemiBold,
            modifier =
                Modifier.padding(
                    start = MaterialTheme.spacing.base.div(2),
                    bottom = MaterialTheme.spacing.base
                )
        )

        SparrowCardNoAnimation {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showChevron: Boolean = true,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.SettingsScreen.icon)
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    horizontal = MaterialTheme.spacing.small,
                    vertical = MaterialTheme.spacing.small
                ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(Dimens.SettingsScreen.primaryIconSize)
        )

        Spacer(modifier = Modifier.size(MaterialTheme.spacing.small))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.SettingsScreen.disabledIcon),
                modifier = Modifier.size(Dimens.SettingsScreen.secondaryIconSize)
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onCheckedChange(!checked)
                }.padding(
                    horizontal = MaterialTheme.spacing.small,
                    vertical = MaterialTheme.spacing.small
                ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.SettingsScreen.icon),
            modifier = Modifier.size(Dimens.SettingsScreen.primaryIconSize)
        )

        Spacer(modifier = Modifier.size(MaterialTheme.spacing.small))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = null,
            colors =
                SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary
                )
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = MaterialTheme.spacing.times(5)),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Alpha.divider)
    )
}

@Preview
@Composable
fun SettingsScreenPreview() {
    SparrowTheme {
        SettingsScreen(
            uiState =
                SettingsUiState(
                    currentLanguage = AppLanguage.ENGLISH,
                    showLanguagePicker = false,
                    buildInfo =
                        BuildInfo(
                            versionName = "1.0.0",
                            versionCode = 1,
                            buildType = "debug",
                            gitSha = null
                        ),
                    isDeveloperModeEnabled = true
                ),
            snackbarHostState = SnackbarHostState(),
            onUiEvent = {},
            scrollState = ScrollState(0),
            innerPadding = PaddingValues(MaterialTheme.spacing.zero)
        )
    }
}
