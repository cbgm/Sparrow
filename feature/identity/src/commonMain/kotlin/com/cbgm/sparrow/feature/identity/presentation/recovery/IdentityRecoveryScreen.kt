package com.cbgm.sparrow.feature.identity.presentation.recovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.cbgm.sparrow.core.ui.theme.spacing
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
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(Res.string.feature_identity_recovery_screen_title)) },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            if (state.loading || state.busy) CircularProgressIndicator()
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            when {
                state.approved -> {
                    Text(stringResource(Res.string.feature_identity_recovery_screen_approved), style = MaterialTheme.typography.titleMedium)
                    if (state.invitationQueued) {
                        Text(stringResource(Res.string.feature_identity_recovery_screen_queued))
                    } else {
                        Text(stringResource(Res.string.feature_identity_recovery_screen_starting))
                        if (!state.busy) Button(onClick = onRetry) { Text(stringResource(Res.string.feature_identity_recovery_screen_retry)) }
                    }
                    Button(onClick = onClose) { Text(stringResource(Res.string.feature_identity_recovery_screen_return)) }
                }
                state.request == null && !state.loading ->
                    Text(stringResource(Res.string.feature_identity_recovery_screen_missing))
                state.request != null -> {
                    val request = state.request
                    Text(stringResource(Res.string.feature_identity_recovery_screen_warning))
                    KeyRow(stringResource(Res.string.feature_identity_recovery_screen_previous_signing), request.previousSigningKey)
                    KeyRow(stringResource(Res.string.feature_identity_recovery_screen_proposed_signing), request.proposedSigningKey)
                    KeyRow(stringResource(Res.string.feature_identity_recovery_screen_previous_encryption), request.previousEncryptionKey)
                    KeyRow(stringResource(Res.string.feature_identity_recovery_screen_proposed_encryption), request.proposedEncryptionKey)
                    if (!request.fingerprintConfirmed) {
                        Text(stringResource(Res.string.feature_identity_recovery_screen_compare))
                        Text(stringResource(Res.string.feature_identity_recovery_screen_scan_instructions))
                        Button(enabled = !state.busy, onClick = onScan) {
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
                            onClick = { onConfirm(fingerprint) }
                        ) { Text(stringResource(Res.string.feature_identity_recovery_screen_confirm)) }
                    } else {
                        Text(stringResource(Res.string.feature_identity_recovery_screen_confirmed))
                        Button(enabled = !state.busy, onClick = onApprove) {
                            Text(stringResource(Res.string.feature_identity_recovery_screen_approve))
                        }
                    }
                    Spacer(Modifier.height(MaterialTheme.spacing.small))
                    TextButton(enabled = !state.busy, onClick = onDismiss) {
                        Text(stringResource(Res.string.feature_identity_recovery_screen_dismiss))
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyRow(label: String, value: String?) {
    Text(label, style = MaterialTheme.typography.labelMedium)
    Text(value ?: stringResource(Res.string.feature_identity_recovery_screen_unavailable), style = MaterialTheme.typography.bodySmall)
}
