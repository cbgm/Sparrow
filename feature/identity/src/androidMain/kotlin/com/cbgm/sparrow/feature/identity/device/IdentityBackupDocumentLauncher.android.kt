package com.cbgm.sparrow.feature.identity.device

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.cbgm.sparrow.core.logging.SparrowLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Android SAF: exports survive uninstall; no storage permission and no plaintext temp file. */
@Composable
actual fun IdentityBackupDocumentLauncher(
    exportRequest: IdentityExportRequest?,
    importRequestId: Int,
    onExportResult: (Boolean, String?) -> Unit,
    onImportResult: (ByteArray?, String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportCallback = rememberUpdatedState(onExportResult)
    val importCallback = rememberUpdatedState(onImportResult)
    val request = rememberUpdatedState(exportRequest)
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) {
            exportCallback.value(false, "Backup export cancelled")
        } else {
            val bytes = request.value?.document
            if (bytes == null) {
                exportCallback.value(false, "Backup export request expired")
            } else {
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        runCatching {
                            val stream = context.contentResolver.openOutputStream(uri, "wt")
                                ?: error("Cannot open the selected backup destination")
                            stream.use {
                                it.write(bytes)
                                it.flush()
                            }
                        }
                    }
                    result.exceptionOrNull()?.let { failure ->
                        SparrowLog.error("IdentityBackupDocumentLauncher", "Identity backup export failed", failure)
                    }
                    exportCallback.value(result.isSuccess, result.exceptionOrNull()?.message)
                }
            }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            importCallback.value(null, "Backup selection cancelled")
        } else {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val stream = context.contentResolver.openInputStream(uri)
                            ?: error("Cannot open the selected identity backup")
                        stream.use { input ->
                            // Read at most 16 KiB plus one byte before rejecting a maliciously large document.
                            val output = java.io.ByteArrayOutputStream()
                            val chunk = ByteArray(2048)
                            var count = 0
                            while (true) {
                                val n = input.read(chunk)
                                if (n < 0) break
                                count += n
                                require(count <= 16_384) { "Identity backup is too large" }
                                output.write(chunk, 0, n)
                            }
                            output.toByteArray()
                        }
                    }
                }
                result.exceptionOrNull()?.let { failure ->
                    SparrowLog.error("IdentityBackupDocumentLauncher", "Identity backup import failed", failure)
                }
                importCallback.value(result.getOrNull(), result.exceptionOrNull()?.message)
            }
        }
    }
    LaunchedEffect(exportRequest?.id) {
        if (exportRequest != null) create.launch("Sparrow-Identity-Backup.json")
    }
    LaunchedEffect(importRequestId) {
        if (importRequestId > 0) open.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
    }
}
