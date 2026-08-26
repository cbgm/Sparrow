package com.cbgm.sparrow.feature.media.device

import androidx.compose.runtime.Composable

interface FileAccessLauncher {
    fun launch()
}

@Composable
expect fun rememberFileAccessLauncher(
    onReturned: (String?) -> Unit,
    onError: (String) -> Unit
): FileAccessLauncher
