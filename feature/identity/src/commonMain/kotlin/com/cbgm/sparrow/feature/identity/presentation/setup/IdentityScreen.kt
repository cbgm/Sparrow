package com.cbgm.sparrow.feature.identity.presentation.setup

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.component.FeedbackBubble
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowCard
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.component.SparrowDetailRow
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.component.SparrowSecondaryButton
import com.cbgm.sparrow.core.ui.component.rememberDelayedVisibility
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity
import com.cbgm.sparrow.feature.identity.presentation.setup.components.IconBadge
import com.cbgm.sparrow.feature.identity.presentation.setup.components.KeyHexGrid
import com.cbgm.sparrow.feature.identity.presentation.setup.components.PublicKeySection
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityBackupUiState
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityBackupUiStatus
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityUiEvent
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityUiState
import com.cbgm.sparrow.feature.identity.presentation.setup.profile.IdentityProfilePictureSection
import com.cbgm.sparrow.feature.identity.presentation.setup.profile.IdentityProfilePictureUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_back
import com.cbgm.sparrow.resources.base_retry
import com.cbgm.sparrow.resources.base_sparrow
import com.cbgm.sparrow.resources.common_copied
import com.cbgm.sparrow.resources.feature_identity_approve_number_create_identity
import com.cbgm.sparrow.resources.feature_identity_backup_exported
import com.cbgm.sparrow.resources.feature_identity_backup_imported
import com.cbgm.sparrow.resources.feature_identity_backup_restore_action
import com.cbgm.sparrow.resources.feature_identity_backup_title
import com.cbgm.sparrow.resources.feature_identity_backup_warning
import com.cbgm.sparrow.resources.feature_identity_check_again
import com.cbgm.sparrow.resources.feature_identity_checking_secure_identity
import com.cbgm.sparrow.resources.feature_identity_choose_phone_number_from_device
import com.cbgm.sparrow.resources.feature_identity_encryption_public_key
import com.cbgm.sparrow.resources.feature_identity_encryption_public_key_description
import com.cbgm.sparrow.resources.feature_identity_identity_enter_phone_description
import com.cbgm.sparrow.resources.feature_identity_incomplete_identity
import com.cbgm.sparrow.resources.feature_identity_incomplete_identity_description
import com.cbgm.sparrow.resources.feature_identity_own_fingerprint_description
import com.cbgm.sparrow.resources.feature_identity_own_fingerprint_title
import com.cbgm.sparrow.resources.feature_identity_private_keys_protected
import com.cbgm.sparrow.resources.feature_identity_share_my_identity
import com.cbgm.sparrow.resources.feature_identity_signing_public_key
import com.cbgm.sparrow.resources.feature_identity_signing_public_key_description
import com.cbgm.sparrow.resources.feature_identity_something_went_wrong
import com.cbgm.sparrow.resources.feature_identity_stable_routing_address_description
import com.cbgm.sparrow.resources.feature_identity_your_phone_number
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

/** Public identity screen contract. */

@Composable
fun IdentityScreen(
    uiState: IdentityUiState,
    modifier: Modifier = Modifier,
    onUiEvent: (IdentityUiEvent) -> Unit,
    scrollState: ScrollState,
    innerPadding: PaddingValues,
    profilePictureState: IdentityProfilePictureUiState? = null,
    onEditProfilePicture: () -> Unit = {},
    backupState: IdentityBackupUiState = IdentityBackupUiState(),
    onExportIdentity: () -> Unit = {},
    onRestoreIdentity: () -> Unit = {},
    page: MeDetailPage = MeDetailPage.Overview
) {
    var copiedEvent by remember { mutableIntStateOf(0) }
    val showLoading = rememberDelayedVisibility(uiState is IdentityUiState.Loading)
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding(),
                    start = MaterialTheme.spacing.screenPadding,
                    end = MaterialTheme.spacing.screenPadding
                )
        ) {
            when (uiState) {
                IdentityUiState.Loading -> {
                    if (showLoading) LoadingContent()
                }

                is IdentityUiState.NoIdentity -> {
                    NoIdentityContent(
                        phoneNumber = uiState.phoneNumber,
                        phoneNumberError = uiState.phoneNumberError,
                        onRequestPhoneNumberHint = { onUiEvent(IdentityUiEvent.RequestPhoneNumberHint) },
                        onPhoneNumberChanged = { onUiEvent(IdentityUiEvent.PhoneNumberChanged(it)) },
                        onCreateIdentity = { onUiEvent(IdentityUiEvent.CreateIdentityClicked) },
                        onRestoreIdentity = onRestoreIdentity,
                        restoreError = backupState.message?.takeIf { backupState.error }
                    )
                }

                is IdentityUiState.Ready -> {
                    ReadyIdentityContent(
                        publicIdentity = uiState.publicIdentity,
                        localPhoneNumber = uiState.localPhoneNumber,
                        scrollState = scrollState,
                        profilePictureState = profilePictureState,
                        onShareIdentity = { onUiEvent(IdentityUiEvent.ShareIdentityClicked) },
                        onEditProfilePicture = onEditProfilePicture,
                        backupState = backupState,
                        onExportIdentity = onExportIdentity,
                        page = page,
                        onOpenKeys = { onUiEvent(IdentityUiEvent.OpenKeys) },
                        onOpenBackup = { onUiEvent(IdentityUiEvent.OpenBackup) },
                        onBack = { onUiEvent(IdentityUiEvent.BackClicked) },
                        onCopied = { copiedEvent++ }
                    )
                }

                IdentityUiState.IncompleteIdentity -> {
                    IncompleteIdentityContent(onRetry = {
                        onUiEvent(IdentityUiEvent.RetryClicked)
                    })
                }

                is IdentityUiState.Error -> {
                    ErrorContent(message = uiState.message, onRetry = {
                        onUiEvent(IdentityUiEvent.RetryClicked)
                    })
                }
            }
        }
        if (copiedEvent > 0) {
            LaunchedEffect(copiedEvent) {
                delay(1000.milliseconds)
                copiedEvent = 0
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = innerPadding.calculateBottomPadding() + MaterialTheme.spacing.large)
            ) {
                FeedbackBubble(
                    text = stringResource(Res.string.common_copied),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = MaterialTheme.spacing.identityScreen.contentTopPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Text(
            text = stringResource(Res.string.feature_identity_checking_secure_identity),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText)
        )
    }
}

@Composable
private fun NoIdentityContent(
    phoneNumber: String,
    phoneNumberError: String?,
    onRequestPhoneNumberHint: () -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onCreateIdentity: () -> Unit,
    onRestoreIdentity: () -> Unit,
    restoreError: String?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        IconBadge(icon = Icons.Default.Shield)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Text(
            text = stringResource(Res.string.base_sparrow),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.micro))

        Text(
            text = stringResource(Res.string.feature_identity_identity_enter_phone_description),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText)
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        SparrowCard {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                SparrowSecondaryButton(
                    onClick = onRequestPhoneNumberHint,
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.feature_identity_choose_phone_number_from_device)
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                SparrowInputField(
                    value = phoneNumber,
                    onValueChange = onPhoneNumberChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(Res.string.feature_identity_your_phone_number),
                    placeholderText = "+491701234567",
                    errorText = phoneNumberError ?: stringResource(Res.string.feature_identity_stable_routing_address_description),
                    isError = phoneNumberError != null,
                    isSingleLine = true
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                SparrowApprovalButton(
                    onClick = onCreateIdentity,
                    enabled = phoneNumber.isNotBlank(),
                    text = stringResource(Res.string.feature_identity_approve_number_create_identity)
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                SparrowSecondaryButton(
                    onClick = onRestoreIdentity,
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.feature_identity_backup_restore_action)
                )
                restoreError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

enum class MeDetailPage { Overview, Keys, Backup }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReadyIdentityContent(
    publicIdentity: PublicIdentity,
    localPhoneNumber: String,
    scrollState: ScrollState,
    profilePictureState: IdentityProfilePictureUiState?,
    onShareIdentity: () -> Unit,
    onEditProfilePicture: () -> Unit,
    backupState: IdentityBackupUiState,
    onExportIdentity: () -> Unit,
    page: MeDetailPage,
    onOpenKeys: () -> Unit,
    onOpenBackup: () -> Unit,
    onBack: () -> Unit,
    onCopied: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when (page) {
            MeDetailPage.Overview -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.spacing.medium),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    profilePictureState?.let { picture ->
                        IdentityProfilePictureSection(state = picture, onEdit = onEditProfilePicture)
                    }
                    Spacer(Modifier.height(MaterialTheme.spacing.medium))
                    val clipboard = LocalClipboardManager.current
                    Text(
                        text = localPhoneNumber,
                        modifier = Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = {
                                clipboard.setText(AnnotatedString(localPhoneNumber))
                                onCopied()
                            }
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(MaterialTheme.spacing.base))
                    Text(
                        text = stringResource(Res.string.feature_identity_private_keys_protected),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(MaterialTheme.spacing.large))
                SparrowCardNoAnimation {
                    Column {
                        SparrowDetailRow(
                            icon = Icons.Default.Share,
                            title = stringResource(Res.string.feature_identity_share_my_identity),
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = onShareIdentity
                        )
                        SparrowDetailRow(
                            icon = Icons.Default.Fingerprint,
                            title = stringResource(Res.string.feature_identity_own_fingerprint_title),
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = onOpenKeys
                        )
                        SparrowDetailRow(
                            icon = Icons.Default.Backup,
                            title = stringResource(Res.string.feature_identity_backup_title),
                            subtitle = stringResource(
                                when (backupState.status) {
                                    IdentityBackupUiStatus.NOT_BACKED_UP -> Res.string.feature_identity_backup_warning
                                    IdentityBackupUiStatus.EXPORTED -> Res.string.feature_identity_backup_exported
                                    IdentityBackupUiStatus.IMPORTED -> Res.string.feature_identity_backup_imported
                                }
                            ).substringBefore('.'),
                            iconTint = MaterialTheme.colorScheme.primary,
                            showDivider = false,
                            onClick = onOpenBackup
                        )
                    }
                }
            }
            MeDetailPage.Keys -> {
                MeSubpageHeader(
                    title = stringResource(Res.string.feature_identity_own_fingerprint_title),
                    onBack = onBack
                )
                SparrowCardNoAnimation {
                    Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                        Text(
                            stringResource(Res.string.feature_identity_own_fingerprint_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(MaterialTheme.spacing.small))
                        Text(
                            stringResource(Res.string.feature_identity_own_fingerprint_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(MaterialTheme.spacing.small))
                        KeyHexGrid(key = publicIdentity.signingPublicKey, onCopied = onCopied)
                        HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.large))
                        PublicKeySection(
                            icon = Icons.Default.Lock,
                            title = stringResource(Res.string.feature_identity_encryption_public_key),
                            description = stringResource(Res.string.feature_identity_encryption_public_key_description),
                            key = publicIdentity.encryptionPublicKey,
                            onCopied = onCopied
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.medium))
                        PublicKeySection(
                            icon = Icons.Default.Key,
                            title = stringResource(Res.string.feature_identity_signing_public_key),
                            description = stringResource(Res.string.feature_identity_signing_public_key_description),
                            key = publicIdentity.signingPublicKey,
                            onCopied = onCopied
                        )
                        Spacer(Modifier.height(MaterialTheme.spacing.medium))
                        SparrowDetailRow(
                            icon = Icons.Default.Share,
                            title = stringResource(Res.string.feature_identity_share_my_identity),
                            onClick = onShareIdentity
                        )
                    }
                }
            }
            MeDetailPage.Backup -> {
                MeSubpageHeader(
                    title = stringResource(Res.string.feature_identity_backup_title),
                    onBack = onBack
                )
                SparrowCardNoAnimation {
                    Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                        IdentityBackupSection(state = backupState, onExport = onExportIdentity)
                    }
                }
            }
        }
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
    }
}

@Composable
private fun MeSubpageHeader(title: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.base_back),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 48.dp)
        )
    }
    Spacer(Modifier.height(MaterialTheme.spacing.medium))
}

@Composable
private fun IncompleteIdentityContent(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        IconBadge(icon = Icons.Default.ErrorOutline, tint = MaterialTheme.colorScheme.error)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Text(
            text = stringResource(Res.string.feature_identity_incomplete_identity),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Text(
            text = stringResource(Res.string.feature_identity_incomplete_identity_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        SparrowApprovalButton(onClick = onRetry, text = stringResource(Res.string.feature_identity_check_again))
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        IconBadge(icon = Icons.Default.ErrorOutline, tint = MaterialTheme.colorScheme.error)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Text(
            text = stringResource(Res.string.feature_identity_something_went_wrong),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        SparrowApprovalButton(onClick = onRetry, text = stringResource(Res.string.base_retry))
    }
}

@Preview(showBackground = true)
@Composable
private fun NoIdentityPreview() {
    SparrowTheme {
        IdentityScreen(
            uiState = IdentityUiState.NoIdentity(phoneNumber = "+491701111111"),
            onUiEvent = {},
            scrollState = ScrollState(0),
            innerPadding = PaddingValues(MaterialTheme.spacing.zero)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReadyIdentityPreview() {
    SparrowTheme {
        IdentityScreen(
            uiState =
                IdentityUiState.Ready(
                    publicIdentity =
                        PublicIdentity(
                            encryptionPublicKey = byteArrayOf(1, 2, 3),
                            signingPublicKey = byteArrayOf(4, 5, 6)
                        ),
                    localPhoneNumber = "+491701111111"
                ),
            onUiEvent = {},
            scrollState = ScrollState(0),
            innerPadding = PaddingValues(MaterialTheme.spacing.zero)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IncompleteIdentityPreview() {
    SparrowTheme {
        IdentityScreen(
            uiState = IdentityUiState.IncompleteIdentity,
            onUiEvent = {},
            scrollState = ScrollState(0),
            innerPadding = PaddingValues(MaterialTheme.spacing.zero)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingIdentityPreview() {
    SparrowTheme {
        IdentityScreen(
            uiState = IdentityUiState.Error("gdfgdgdg"),
            onUiEvent = {},
            scrollState = ScrollState(0),
            innerPadding = PaddingValues(MaterialTheme.spacing.zero)
        )
    }
}
