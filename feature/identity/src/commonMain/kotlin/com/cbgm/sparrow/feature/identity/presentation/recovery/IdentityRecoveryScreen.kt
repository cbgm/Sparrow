package com.cbgm.sparrow.feature.identity.presentation.recovery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.identity.presentation.setup.model.PendingIdentityReviewUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_identity_recovery_screen_approve
import com.cbgm.sparrow.resources.feature_identity_recovery_screen_approved
import com.cbgm.sparrow.resources.feature_identity_recovery_screen_compare
import com.cbgm.sparrow.resources.feature_identity_recovery_screen_confirm
import com.cbgm.sparrow.resources.feature_identity_recovery_screen_confirmed
import com.cbgm.sparrow.resources.feature_identity_recovery_screen_dismiss
import com.cbgm.sparrow.resources.feature_identity_recovery_screen_fingerprint
import com.cbgm.sparrow.resources.feature_identity_recovery_screen_missing
import com.cbgm.sparrow.resources.feature_identity_recovery_screen_previous_encryption
import com.cbgm.sparrow.resources.feature_identity_recovery_screen_previous_signing
import com.cbgm.sparrow.resources.feature_identity_recovery_screen_proposed_encryption
import com.cbgm.sparrow.resources.feature_identity_recovery_screen_proposed_signing
import com.cbgm.sparrow.resources.feature_identity_recovery_screen_queued
import com.cbgm.sparrow.resources.feature_identity_recovery_screen_retry
import com.cbgm.sparrow.resources.feature_identity_recovery_screen_return
import com.cbgm.sparrow.resources.feature_identity_recovery_screen_scan
import com.cbgm.sparrow.resources.feature_identity_recovery_screen_scan_instructions
import com.cbgm.sparrow.resources.feature_identity_recovery_screen_starting
import com.cbgm.sparrow.resources.feature_identity_recovery_screen_title
import com.cbgm.sparrow.resources.feature_identity_recovery_screen_unavailable
import com.cbgm.sparrow.resources.feature_identity_recovery_screen_warning
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityRecoveryScreen(
    state: IdentityRecoveryUiState,
    onClose: () -> Unit,
    onConfirm: (String) -> Unit,
    onScan: () -> Unit,
    onApprove: () -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    var fingerprint by remember { mutableStateOf("") }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(
            title = {
                Text(
                    stringResource(Res.string.feature_identity_recovery_screen_title),
                    style = MaterialTheme.typography.titleSmall
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            ),
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(MaterialTheme.spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            if (state.loading || state.busy) CircularProgressIndicator()
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            when {
                state.approved -> {
                    SparrowCardNoAnimation {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
                        ) {
                            Text(
                                stringResource(Res.string.feature_identity_recovery_screen_approved),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (state.invitationQueued) {
                                Text(stringResource(Res.string.feature_identity_recovery_screen_queued))
                            } else {
                                Text(stringResource(Res.string.feature_identity_recovery_screen_starting))
                                if (!state.busy) {
                                    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                                        Text(stringResource(Res.string.feature_identity_recovery_screen_retry))
                                    }
                                }
                            }
                        }
                    }
                    Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.feature_identity_recovery_screen_return))
                    }
                }
                state.request == null && !state.loading -> {
                    SparrowCardNoAnimation {
                        Text(
                            stringResource(Res.string.feature_identity_recovery_screen_missing),
                            modifier = Modifier.padding(MaterialTheme.spacing.medium)
                        )
                    }
                }
                state.request != null -> {
                    val request = state.request
                    // Identity changes are security-sensitive: keep this warning prominent.
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Text(
                            stringResource(Res.string.feature_identity_recovery_screen_warning),
                            modifier = Modifier.padding(MaterialTheme.spacing.medium),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    SparrowCardNoAnimation {
                        Column {
                            KeyRow(stringResource(Res.string.feature_identity_recovery_screen_previous_signing), request.previousSigningKey)
                            RecoveryDivider()
                            KeyRow(stringResource(Res.string.feature_identity_recovery_screen_proposed_signing), request.proposedSigningKey)
                            RecoveryDivider()
                            KeyRow(stringResource(Res.string.feature_identity_recovery_screen_previous_encryption), request.previousEncryptionKey)
                            RecoveryDivider()
                            KeyRow(stringResource(Res.string.feature_identity_recovery_screen_proposed_encryption), request.proposedEncryptionKey)
                        }
                    }
                    if (!request.fingerprintConfirmed) {
                        SparrowCardNoAnimation {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
                            ) {
                                Text(
                                    stringResource(Res.string.feature_identity_recovery_screen_compare),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    stringResource(Res.string.feature_identity_recovery_screen_scan_instructions),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    enabled = !state.busy,
                                    onClick = onScan,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(Res.string.feature_identity_recovery_screen_scan))
                                }
                                OutlinedTextField(
                                    value = fingerprint,
                                    onValueChange = { fingerprint = it.take(90) },
                                    label = { Text(stringResource(Res.string.feature_identity_recovery_screen_fingerprint)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    enabled = !state.busy && fingerprint.isNotBlank(),
                                    onClick = { onConfirm(fingerprint) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(Res.string.feature_identity_recovery_screen_confirm))
                                }
                            }
                        }
                    } else {
                        SparrowCardNoAnimation {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
                            ) {
                                Text(
                                    stringResource(Res.string.feature_identity_recovery_screen_confirmed),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Button(
                                    enabled = !state.busy,
                                    onClick = onApprove,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(Res.string.feature_identity_recovery_screen_approve))
                                }
                            }
                        }
                    }
                    TextButton(enabled = !state.busy, onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.feature_identity_recovery_screen_dismiss))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecoveryDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = MaterialTheme.spacing.medium),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun KeyRow(label: String, value: String?) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value ?: stringResource(Res.string.feature_identity_recovery_screen_unavailable),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview
@Composable
private fun IdentityRecoveryScreenPreview() {
    SparrowTheme {
        IdentityRecoveryScreen(
            state = IdentityRecoveryUiState(
                request = PendingIdentityReviewUi(
                    peerId = "peer_id",
                    invitationId = "invitation_id",
                    previousSigningKey = "prev_signing_key",
                    proposedSigningKey = "prop_signing_key",
                    previousEncryptionKey = "prev_encryption_key",
                    proposedEncryptionKey = "prop_encryption_key",
                    fingerprintConfirmed = false
                )
            ),
            onClose = {},
            onConfirm = {},
            onScan = {},
            onApprove = {},
            onDismiss = {},
            onRetry = {}
        )
    }
}
