package com.cbgm.sparrow.feature.media.device

import androidx.compose.runtime.Composable

interface FileOpener {
    suspend fun open(
        localFilePath: String,
        fileName: String,
        mimeType: String
    ): Result<Unit>
}

@Composable
expect fun rememberFileOpener(): FileOpener
