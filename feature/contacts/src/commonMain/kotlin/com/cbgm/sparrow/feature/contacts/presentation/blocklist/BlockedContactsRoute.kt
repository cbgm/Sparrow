package com.cbgm.sparrow.feature.contacts.presentation.blocklist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BlockedContactsRoute(
    modifier: Modifier = Modifier,
    viewModel: BlockedContactsViewModel = koinViewModel()
) {
    BlockedContactsScreen(
        modifier = modifier,
        blockedContacts = viewModel.blockedContacts,
        processingContactId = viewModel.processingContactId,
        addDialogVisible = viewModel.addDialogVisible,
        dialogState = viewModel.dialogState,
        onUiEvent = viewModel::onUiEvent
    )
}
