package com.cbgm.sparrow.core.ui.device.clipboard

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.toClipEntry

internal actual fun String.toClipEntry(): ClipEntry =
    ClipData.newPlainText(null, this).toClipEntry()
