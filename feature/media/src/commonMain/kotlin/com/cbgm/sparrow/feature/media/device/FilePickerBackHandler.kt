package com.cbgm.sparrow.feature.media.device

import androidx.compose.runtime.Composable

@Composable
expect fun FilePickerBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit
)
