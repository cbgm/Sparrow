package com.cbgm.sparrow.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AttachmentColors(
    val gallery: Color,
    val camera: Color,
    val file: Color,
    val contact: Color,
    val location: Color
)

internal val SparrowAttachmentColors =
    AttachmentColors(
        gallery = Color(0xFF569EEC),
        camera = Color(0xFFE85D7F),
        file = Color(0xFF63BB6A),
        contact = Color(0xFFB78CF5),
        location = Color(0xFFBBB268)
    )

internal val LocalAttachmentColors =
    staticCompositionLocalOf {
        SparrowAttachmentColors
    }

val MaterialTheme.attachmentColors: AttachmentColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAttachmentColors.current
