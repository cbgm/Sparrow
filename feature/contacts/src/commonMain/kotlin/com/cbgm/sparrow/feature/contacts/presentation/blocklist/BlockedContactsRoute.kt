package com.cbgm.sparrow.feature.contacts.presentation.blocklist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cbgm.sparrow.feature.contacts.presentation.blocklist.model.BlockedContactsEffect
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BlockedContactsRoute(
    modifier: Modifier = Modifier,
    viewModel: BlockedContactsViewModel = koinViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { event ->
            when (event) {
                is BlockedContactsEffect.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        BlockedContactsScreen(
            blockedContacts = viewModel.blockedContacts,
            processingContactId = viewModel.processingContactId,
            addDialogVisible = viewModel.addDialogVisible,
            dialogState = viewModel.dialogState,
            onUiEvent = viewModel::onUiEvent
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
