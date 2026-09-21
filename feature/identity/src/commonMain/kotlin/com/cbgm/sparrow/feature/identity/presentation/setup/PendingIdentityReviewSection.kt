package com.cbgm.sparrow.feature.identity.presentation.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.cbgm.sparrow.core.ui.component.SparrowCard
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.identity.presentation.setup.model.PendingIdentityReviewUi
import com.cbgm.sparrow.feature.identity.presentation.setup.model.PendingIdentityReviewUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_close
import com.cbgm.sparrow.resources.feature_identity_recovery_approve
import com.cbgm.sparrow.resources.feature_identity_recovery_approve_warning
import com.cbgm.sparrow.resources.feature_identity_recovery_completed
import com.cbgm.sparrow.resources.feature_identity_recovery_confirm_fingerprint
import com.cbgm.sparrow.resources.feature_identity_recovery_dismiss
import com.cbgm.sparrow.resources.feature_identity_recovery_dismiss_warning
import com.cbgm.sparrow.resources.feature_identity_recovery_fingerprint_hint
import com.cbgm.sparrow.resources.feature_identity_recovery_fingerprint_label
import com.cbgm.sparrow.resources.feature_identity_recovery_fingerprint_recorded
import com.cbgm.sparrow.resources.feature_identity_recovery_new_encryption
import com.cbgm.sparrow.resources.feature_identity_recovery_new_signing
import com.cbgm.sparrow.resources.feature_identity_recovery_old_encryption
import com.cbgm.sparrow.resources.feature_identity_recovery_old_signing
import com.cbgm.sparrow.resources.feature_identity_recovery_peer
import com.cbgm.sparrow.resources.feature_identity_recovery_review_action
import com.cbgm.sparrow.resources.feature_identity_recovery_review_title
import com.cbgm.sparrow.resources.feature_identity_recovery_review_warning
import com.cbgm.sparrow.resources.feature_identity_recovery_unavailable
import org.jetbrains.compose.resources.stringResource

/** Explicit manual approval only: stale packets are quarantined; normal invitation is still required. */
@Composable
internal fun PendingIdentityReviewSection(
    state: PendingIdentityReviewUiState,
    onDismiss: (String, String) -> Unit,
    onConfirmFingerprint: (String, String, String) -> Unit,
    onApprove: (String, String) -> Unit
) {
    var selected by remember { mutableStateOf<PendingIdentityReviewUi?>(null) }
    var enteredFingerprint by remember { mutableStateOf("") }
    if (state.requests.isEmpty() && state.errorMessage == null && !state.replacementCompleted) return

    Spacer(Modifier.height(MaterialTheme.spacing.large))
    SparrowCard {
        Column(Modifier.padding(MaterialTheme.spacing.medium)) {
            Text(
                stringResource(Res.string.feature_identity_recovery_review_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(Res.string.feature_identity_recovery_review_warning),
                style = MaterialTheme.typography.bodyMedium
            )
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.replacementCompleted) {
                Text(
                    stringResource(Res.string.feature_identity_recovery_completed),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            state.requests.forEach { candidate ->
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        candidate.peerId,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = {
                        selected = candidate
                        enteredFingerprint = ""
                    }) {
                        Text(stringResource(Res.string.feature_identity_recovery_review_action))
                    }
                }
            }
        }
    }

    val candidate = selected?.takeIf { chosen ->
        state.requests.any {
            it.peerId == chosen.peerId && it.invitationId == chosen.invitationId &&
                it.proposedSigningKey == chosen.proposedSigningKey &&
                it.proposedEncryptionKey == chosen.proposedEncryptionKey &&
                it.fingerprintConfirmed == chosen.fingerprintConfirmed
        }
    }
    if (candidate != null) {
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(stringResource(Res.string.feature_identity_recovery_review_title)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(Res.string.feature_identity_recovery_dismiss_warning))
                    if (candidate.fingerprintConfirmed) {
                        Text(stringResource(Res.string.feature_identity_recovery_fingerprint_recorded))
                    } else {
                        Text(stringResource(Res.string.feature_identity_recovery_fingerprint_hint))
                    }
                    KeyReviewText(stringResource(Res.string.feature_identity_recovery_peer), candidate.peerId)
                    KeyReviewText(
                        stringResource(Res.string.feature_identity_recovery_old_signing),
                        candidate.previousSigningKey ?: stringResource(Res.string.feature_identity_recovery_unavailable)
                    )
                    KeyReviewText(stringResource(Res.string.feature_identity_recovery_new_signing), candidate.proposedSigningKey)
                    KeyReviewText(
                        stringResource(Res.string.feature_identity_recovery_old_encryption),
                        candidate.previousEncryptionKey ?: stringResource(Res.string.feature_identity_recovery_unavailable)
                    )
                    KeyReviewText(stringResource(Res.string.feature_identity_recovery_new_encryption), candidate.proposedEncryptionKey)
                    if (candidate.fingerprintConfirmed) {
                        Spacer(Modifier.height(MaterialTheme.spacing.medium))
                        Text(stringResource(Res.string.feature_identity_recovery_approve_warning))
                    } else {
                        OutlinedTextField(
                            value = enteredFingerprint,
                            onValueChange = { enteredFingerprint = it.take(90) },
                            label = { Text(stringResource(Res.string.feature_identity_recovery_fingerprint_label)) },
                            singleLine = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Column {
                    if (!candidate.fingerprintConfirmed) {
                        Button(
                            enabled = state.confirmingInvitationId == null &&
                                state.dismissingInvitationId == null &&
                                enteredFingerprint.trim().isNotEmpty(),
                            onClick = {
                                onConfirmFingerprint(candidate.peerId, candidate.invitationId, enteredFingerprint)
                                enteredFingerprint = ""
                                selected = null
                            }
                        ) { Text(stringResource(Res.string.feature_identity_recovery_confirm_fingerprint)) }
                    }
                    if (candidate.fingerprintConfirmed) {
                        Button(
                            enabled = state.approvingInvitationId == null &&
                                state.dismissingInvitationId == null && state.confirmingInvitationId == null,
                            onClick = {
                                onApprove(candidate.peerId, candidate.invitationId)
                                selected = null
                            }
                        ) { Text(stringResource(Res.string.feature_identity_recovery_approve)) }
                    }
                    TextButton(
                        enabled = state.dismissingInvitationId == null &&
                            state.confirmingInvitationId == null && state.approvingInvitationId == null,
                        onClick = {
                            onDismiss(candidate.peerId, candidate.invitationId)
                            selected = null
                        }
                    ) { Text(stringResource(Res.string.feature_identity_recovery_dismiss)) }
                }
            },
            dismissButton = {
                TextButton(onClick = { selected = null }) {
                    Text(stringResource(Res.string.base_close))
                }
            }
        )
    }
}

@Composable
private fun KeyReviewText(label: String, value: String) {
    Spacer(Modifier.height(MaterialTheme.spacing.small))
    Text(label, style = MaterialTheme.typography.labelMedium)
    Text(value, style = MaterialTheme.typography.bodySmall)
}
