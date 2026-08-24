package com.cbgm.sparrow.feature.identity.device

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Displays a QR code.
 *
 * Platform implementations choose how the QR code is rendered.
 */
@Composable
expect fun QrCode(
    content: String,
    modifier: Modifier = Modifier
)
