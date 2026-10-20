package com.cbgm.sparrow.feature.identity.presentation.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.FeedbackBubble
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.component.SparrowDetailRow
import com.cbgm.sparrow.core.ui.component.SparrowScrollScaffold
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity
import com.cbgm.sparrow.feature.identity.presentation.component.IconBadge
import com.cbgm.sparrow.feature.identity.presentation.details.component.KeyHexGrid
import com.cbgm.sparrow.feature.identity.presentation.details.component.PublicKeySection
import com.cbgm.sparrow.feature.identity.presentation.details.mapper.toUi
import com.cbgm.sparrow.feature.identity.presentation.details.model.IdentityDetailsUiEvent
import com.cbgm.sparrow.feature.identity.presentation.details.model.IdentityDetailsUiState
import com.cbgm.sparrow.feature.identity.presentation.details.model.PublicHexUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_back
import com.cbgm.sparrow.resources.base_retry
import com.cbgm.sparrow.resources.common_copied
import com.cbgm.sparrow.resources.feature_identity_encryption_public_key
import com.cbgm.sparrow.resources.feature_identity_encryption_public_key_description
import com.cbgm.sparrow.resources.feature_identity_own_fingerprint_description
import com.cbgm.sparrow.resources.feature_identity_own_fingerprint_title
import com.cbgm.sparrow.resources.feature_identity_share_my_identity
import com.cbgm.sparrow.resources.feature_identity_signing_public_key
import com.cbgm.sparrow.resources.feature_identity_signing_public_key_description
import com.cbgm.sparrow.resources.feature_identity_something_went_wrong
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityDetailsScreen(
    uiState: IdentityDetailsUiState,
    onUiEvent: (IdentityDetailsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var copiedEvent by remember { mutableIntStateOf(0) }

    SparrowScrollScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    scrolledContainerColor = containerColor,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Text(
                        text = stringResource(Res.string.feature_identity_own_fingerprint_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onUiEvent(IdentityDetailsUiEvent.BackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.base_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding, scrollState ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaterialTheme.spacing.screenPadding),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                uiState.error.isNotEmpty() -> {
                    ErrorContent(
                        error = uiState.error,
                        onRetry = { onUiEvent(IdentityDetailsUiEvent.RetryClicked) }
                    )
                }

                uiState.error.isEmpty() && uiState.publicIdentity != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(
                                top = innerPadding.calculateTopPadding() + MaterialTheme.spacing.screenPadding,
                                bottom = innerPadding.calculateBottomPadding() + MaterialTheme.spacing.screenPadding
                            )
                    ) {
                        ReadyDetailsContent(
                            publicIdentity = uiState.publicIdentity,
                            onShareIdentity = { onUiEvent(IdentityDetailsUiEvent.ShareIdentityClicked) },
                            onCopied = { copiedEvent++ }
                        )
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
}

@Composable
private fun ErrorContent(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        IconBadge(
            icon = Icons.Default.ErrorOutline,
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        Text(
            text = stringResource(Res.string.feature_identity_something_went_wrong),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        SparrowApprovalButton(
            onClick = onRetry,
            text = stringResource(Res.string.base_retry)
        )
    }
}

@Composable
private fun ReadyDetailsContent(
    publicIdentity: PublicHexUi,
    onShareIdentity: () -> Unit,
    onCopied: () -> Unit
) {
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
    Spacer(Modifier.height(MaterialTheme.spacing.medium))
}

@Preview
@Composable
private fun IdentityDetailsScreenPreview() {
    SparrowTheme {
        IdentityDetailsScreen(
            uiState = IdentityDetailsUiState(
                isLoading = false,
                error = "",
                publicIdentity = PublicIdentity(
                    signingPublicKey = ByteArray(32) { it.toByte() },
                    encryptionPublicKey = ByteArray(32) { it.toByte() }
                ).toUi()
            ),
            onUiEvent = {}
        )
    }
}
