package com.cbgm.sparrow.feature.identity.presentation.backup

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.identity.device.IdentityBackupDocumentLauncher
import com.cbgm.sparrow.feature.identity.device.IdentityExportRequest
import com.cbgm.sparrow.feature.identity.presentation.backup.model.IdentityBackupUiEvent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IdentityBackupRoute(
    modifier: Modifier = Modifier,
    viewModel: IdentityBackupViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val exportDocument by viewModel.exportDocument.collectAsStateWithLifecycle()
    var showExportDialog by remember { mutableStateOf(false) }
    var importDocument by remember { mutableStateOf<ByteArray?>(null) }
    var password by remember { mutableStateOf("") }
    var importRequestId by remember { mutableIntStateOf(0) }
    var exportRequestId by remember { mutableIntStateOf(0) }
    var exportRequest by remember { mutableStateOf<IdentityExportRequest?>(null) }

    LaunchedEffect(exportDocument) {
        if (exportDocument != null) {
            exportRequestId += 1
            exportRequest = IdentityExportRequest(exportRequestId, exportDocument!!)
        }
    }

    IdentityBackupDocumentLauncher(
        exportRequest = exportRequest,
        importRequestId = importRequestId,
        onExportResult = { success, error ->
            exportRequest = null
            viewModel.onUiEvent(IdentityBackupUiEvent.BackupFileWritten(success, error))
        },
        onImportResult = { document, error ->
            if (document != null) {
                importDocument = document
                password = ""
            } else if (error != null && error != "Backup selection cancelled") {
                viewModel.showBackupError(error)
            }
        }
    )

    Box(modifier = modifier) {
        IdentityBackupScreen(
            uiState = uiState,
            onUiEvent = { event ->
                viewModel.onUiEvent(event)
            },
            onExportIdentity = {
                showExportDialog = true
                password = ""
            },
            modifier = Modifier
        )

        if (showExportDialog || importDocument != null) {
            val isImport = importDocument != null
            IdentityBackupPasswordDialog(
                isImport = isImport,
                password = password,
                onPasswordChange = { password = it },
                busy = uiState.busy,
                onDismiss = {
                    showExportDialog = false
                    importDocument = null
                    password = ""
                },
                onConfirm = {
                    val pass = password.toCharArray()
                    password = ""
                    if (isImport) {
                        viewModel.onUiEvent(IdentityBackupUiEvent.RestoreBackup(importDocument!!, pass))
                        importDocument = null
                    } else {
                        showExportDialog = false
                        viewModel.onUiEvent(IdentityBackupUiEvent.PrepareBackup(pass))
                    }
                }
            )
        }
    }
}
