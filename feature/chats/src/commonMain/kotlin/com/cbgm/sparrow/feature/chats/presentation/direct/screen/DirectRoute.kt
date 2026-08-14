package com.cbgm.sparrow.feature.chats.presentation.direct.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.component.SparrowSecondaryButton
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectUiEvent
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_chats_import_contact_identity
import com.cbgm.sparrow.resources.feature_chats_manual_identity_setup_description
import com.cbgm.sparrow.resources.feature_chats_manual_identity_setup_title
import com.cbgm.sparrow.resources.feature_identity_share_my_identity
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun DirectRoute(
    conversationId: String,
    contactId: String,
    contactName: String,
    modifier: Modifier = Modifier,
    viewModel: DirectViewModel =
        koinViewModel {
            parametersOf(
                conversationId,
                contactId,
                contactName
            )
        }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showIdentitySetupDialog by remember(contactId) { mutableStateOf(false) }

    LaunchedEffect(contactId) {
        viewModel.markConversationRead()
    }

    DisposableEffect(contactId) {
        onDispose(viewModel::stopTyping)
    }

    val incomingMessageIds =
        uiState.messages
            .asSequence()
            .filterNot { message -> message.isMine }
            .map { message -> message.id }
            .toList()

    LaunchedEffect(incomingMessageIds) {
        if (incomingMessageIds.isNotEmpty()) {
            viewModel.markConversationRead()
        }
    }

    DirectScreen(
        uiState = uiState,
        onUiEvent = { event ->
            if (event == DirectUiEvent.ManualIdentitySetupClicked) {
                showIdentitySetupDialog = true
            } else {
                viewModel.onUiEvent(event)
            }
        },
        modifier = modifier
    )

    if (showIdentitySetupDialog) {
        IdentitySetupDialog(
            onShareIdentity = {
                showIdentitySetupDialog = false
                viewModel.onUiEvent(DirectUiEvent.ShareIdentityClicked)
            },
            onImportIdentity = {
                showIdentitySetupDialog = false
                viewModel.onUiEvent(DirectUiEvent.ImportIdentityClicked)
            },
            onDismiss = { showIdentitySetupDialog = false }
        )
    }
}

@Composable
private fun IdentitySetupDialog(
    onShareIdentity: () -> Unit,
    onImportIdentity: () -> Unit,
    onDismiss: () -> Unit
) {
    SparrowAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.feature_chats_manual_identity_setup_title),
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(Res.string.feature_chats_manual_identity_setup_description))
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                SparrowOutlinedButton(
                    onClick = onShareIdentity,
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.feature_identity_share_my_identity)
                )
                SparrowOutlinedButton(
                    onClick = onImportIdentity,
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.feature_chats_import_contact_identity)
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            SparrowSecondaryButton(
                onClick = onDismiss,
                text = stringResource(Res.string.base_cancel),
                fillMaxWidth = false
            )
        }
    )
}

@Preview
@Composable
private fun IdentitySetupDialogPreview() {
    SparrowTheme {
        IdentitySetupDialog(
            onShareIdentity = {},
            onImportIdentity = {},
            onDismiss = {}
        )
    }
}
