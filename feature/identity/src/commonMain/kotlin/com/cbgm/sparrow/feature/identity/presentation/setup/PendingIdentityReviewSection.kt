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
import com.cbgm.sparrow.resources.feature_identity_recovery_dismiss
import com.cbgm.sparrow.resources.feature_identity_recovery_dismiss_warning
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

/** A request is deliberately read-only: replacement is not wired until old trust and routing are handled safely. */
@Composable
internal fun PendingIdentityReviewSection(
    state: PendingIdentityReviewUiState,
    onDismiss: (String, String) -> Unit
) {
    var selected by remember { mutableStateOf<PendingIdentityReviewUi?>(null) }
    if (state.requests.isEmpty() && state.errorMessage == null) return

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
                    TextButton(onClick = { selected = candidate }) {
                        Text(stringResource(Res.string.feature_identity_recovery_review_action))
                    }
                }
            }
        }
    }

    val candidate = selected?.takeIf { chosen ->
        state.requests.any { it.peerId == chosen.peerId && it.invitationId == chosen.invitationId }
    }
    if (candidate != null) {
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(stringResource(Res.string.feature_identity_recovery_review_title)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(Res.string.feature_identity_recovery_dismiss_warning))
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
                }
            },
            confirmButton = {
                Button(
                    enabled = state.dismissingInvitationId == null,
                    onClick = {
                        onDismiss(candidate.peerId, candidate.invitationId)
                        selected = null
                    }
                ) { Text(stringResource(Res.string.feature_identity_recovery_dismiss)) }
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
