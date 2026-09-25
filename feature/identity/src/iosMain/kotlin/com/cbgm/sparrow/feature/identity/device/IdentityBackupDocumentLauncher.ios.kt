package com.cbgm.sparrow.feature.identity.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun IdentityBackupDocumentLauncher(
    exportRequest: IdentityExportRequest?,
    importRequestId: Int,
    onExportResult: (Boolean, String?) -> Unit,
    onImportResult: (ByteArray?, String?) -> Unit
) {
    LaunchedEffect(exportRequest?.id) {
        if (exportRequest != null) onExportResult(false, "Identity backup is not yet supported on iOS")
    }
    LaunchedEffect(importRequestId) {
        if (importRequestId > 0) onImportResult(null, "Identity restoration is not yet supported on iOS")
    }
}
