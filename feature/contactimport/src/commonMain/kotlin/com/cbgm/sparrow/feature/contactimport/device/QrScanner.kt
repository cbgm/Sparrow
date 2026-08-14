package com.cbgm.sparrow.feature.contactimport.device

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Displays a platform QR scanner.
 *
 * The scanner returns only the decoded string. It does not know
 * anything about Sparrow payloads or contact importing.
 */
@Composable
expect fun QrScanner(
    onQrCodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier
)
