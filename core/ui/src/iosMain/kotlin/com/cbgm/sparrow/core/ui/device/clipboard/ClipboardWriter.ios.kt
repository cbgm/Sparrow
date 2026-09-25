package com.cbgm.sparrow.core.ui.device.clipboard

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun String.toClipEntry(): ClipEntry = ClipEntry.withPlainText(this)
