package com.cbgm.sparrow.feature.identity.presentation.platform

import androidx.compose.runtime.Composable

/**
 * Returns a callback that opens the platform share UI with the
 * supplied Sparrow identity payload.
 */
@Composable
expect fun rememberIdentityShareLauncher(
    encodedIdentity: String,
    shareTitle: String = "Share Sparrow identity"
): () -> Unit
