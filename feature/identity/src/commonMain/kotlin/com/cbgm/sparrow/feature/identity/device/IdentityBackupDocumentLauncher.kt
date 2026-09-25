package com.cbgm.sparrow.feature.identity.device

import androidx.compose.runtime.Composable

/** Request IDs prevent a picker from reopening after a normal Compose recomposition. */
data class IdentityExportRequest(
    val id: Int,
    val document: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IdentityExportRequest

        if (id != other.id) return false
        if (!document.contentEquals(other.document)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + document.contentHashCode()
        return result
    }
}

@Composable
expect fun IdentityBackupDocumentLauncher(
    exportRequest: IdentityExportRequest?,
    importRequestId: Int,
    onExportResult: (Boolean, String?) -> Unit,
    onImportResult: (ByteArray?, String?) -> Unit
)
