package com.cbgm.sparrow.feature.identity.presentation.overview

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
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
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.component.SparrowDetailRow
import com.cbgm.sparrow.core.ui.component.rememberDelayedVisibility
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.identity.presentation.backup.model.BackupStateUi
import com.cbgm.sparrow.feature.identity.presentation.component.IconBadge
import com.cbgm.sparrow.feature.identity.presentation.overview.model.IdentityOverviewUiEvent
import com.cbgm.sparrow.feature.identity.presentation.overview.model.IdentityOverviewUiState
import com.cbgm.sparrow.feature.identity.presentation.overview.profile.IdentityProfilePictureUiState
import com.cbgm.sparrow.feature.identity.presentation.setup.profile.IdentityProfilePictureSection
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_retry
import com.cbgm.sparrow.resources.common_copied
import com.cbgm.sparrow.resources.feature_identity_backup_exported
import com.cbgm.sparrow.resources.feature_identity_backup_imported
import com.cbgm.sparrow.resources.feature_identity_backup_title
import com.cbgm.sparrow.resources.feature_identity_backup_warning
import com.cbgm.sparrow.resources.feature_identity_checking_secure_identity
import com.cbgm.sparrow.resources.feature_identity_own_fingerprint_title
import com.cbgm.sparrow.resources.feature_identity_private_keys_protected
import com.cbgm.sparrow.resources.feature_identity_share_my_identity
import com.cbgm.sparrow.resources.feature_identity_something_went_wrong
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun IdentityOverviewScreen(
    uiState: IdentityOverviewUiState,
    profilePictureState: IdentityProfilePictureUiState?,
    modifier: Modifier = Modifier,
    onUiEvent: (IdentityOverviewUiEvent) -> Unit,
    scrollState: ScrollState,
    innerPadding: PaddingValues,
    onEditProfilePicture: () -> Unit = {}
) {
    var copiedEvent by remember { mutableIntStateOf(0) }
    val showLoading = rememberDelayedVisibility(uiState is IdentityOverviewUiState.Loading)

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.spacing.screenPadding)
    ) {
        when {
            uiState is IdentityOverviewUiState.Ready -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(
                            top = innerPadding.calculateTopPadding() + MaterialTheme.spacing.screenPadding,
                            bottom = innerPadding.calculateBottomPadding() + MaterialTheme.spacing.screenPadding
                        )
                ) {
                    ReadyOverviewContent(
                        localPhoneNumber = uiState.localPhoneNumber,
                        profilePictureState = profilePictureState,
                        onShareIdentity = { onUiEvent(IdentityOverviewUiEvent.ShareIdentityClicked) },
                        onEditProfilePicture = onEditProfilePicture,
                        backupStatus = uiState.backupStatus,
                        onOpenKeys = { onUiEvent(IdentityOverviewUiEvent.OpenKeysClicked) },
                        onOpenBackup = { onUiEvent(IdentityOverviewUiEvent.OpenBackupClicked) },
                        onCopied = { copiedEvent++ }
                    )
                }
            }

            uiState is IdentityOverviewUiState.Loading && showLoading -> {
                LoadingContent()
            }

            uiState is IdentityOverviewUiState.Error -> {
                ErrorContent(
                    message = stringResource(Res.string.feature_identity_something_went_wrong),
                    onRetry = {
                        onUiEvent(IdentityOverviewUiEvent.RetryClicked)
                    }
                )
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
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        Text(
            text = stringResource(Res.string.feature_identity_checking_secure_identity),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReadyOverviewContent(
    localPhoneNumber: String,
    profilePictureState: IdentityProfilePictureUiState?,
    onShareIdentity: () -> Unit,
    onEditProfilePicture: () -> Unit,
    backupStatus: BackupStateUi,
    onOpenKeys: () -> Unit,
    onOpenBackup: () -> Unit,
    onCopied: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
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
                    when (backupStatus) {
                        BackupStateUi.NOT_BACKED_UP -> Res.string.feature_identity_backup_warning
                        BackupStateUi.EXPORTED -> Res.string.feature_identity_backup_exported
                        BackupStateUi.IMPORTED -> Res.string.feature_identity_backup_imported
                    }
                ).substringBefore('.'),
                iconTint = MaterialTheme.colorScheme.primary,
                showDivider = false,
                onClick = onOpenBackup
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
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
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Preview
@Composable
private fun IdentityOverviewScreenPreview() {
    SparrowTheme {
        IdentityOverviewScreen(
            uiState = IdentityOverviewUiState.Ready(localPhoneNumber = "+491701234567"),
            onUiEvent = {},
            scrollState = ScrollState(0),
            innerPadding = PaddingValues(0.dp),
            profilePictureState = IdentityProfilePictureUiState(
                hasPicture = false,
                isSaving = false
            )
        )
    }
}

@Preview
@Composable
private fun IdentityOverviewScreenLoadingPreview() {
    SparrowTheme {
        IdentityOverviewScreen(
            uiState = IdentityOverviewUiState.Error,
            onUiEvent = {},
            scrollState = ScrollState(0),
            innerPadding = PaddingValues(0.dp),
            profilePictureState = null
        )
    }
}
