package com.cbgm.sparrow.feature.media.device

import androidx.compose.runtime.Composable

interface FileOpener {
    suspend fun open(
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Result<Unit>
}

@Composable
expect fun rememberFileOpener(): FileOpener
