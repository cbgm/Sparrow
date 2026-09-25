package com.cbgm.sparrow.core.ui.device.clipboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import kotlinx.coroutines.launch

interface ClipboardWriter {
    fun copyText(text: String)
}

@Composable
fun rememberClipboardWriter(): ClipboardWriter {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    return remember(clipboard, coroutineScope) {
        object : ClipboardWriter {
            override fun copyText(text: String) {
                coroutineScope.launch {
                    clipboard.setClipEntry(text.toClipEntry())
                }
            }
        }
    }
}

internal expect fun String.toClipEntry(): ClipEntry
