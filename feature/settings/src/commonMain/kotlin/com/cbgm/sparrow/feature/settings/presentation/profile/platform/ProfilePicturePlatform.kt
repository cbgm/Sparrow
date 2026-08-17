package com.cbgm.sparrow.feature.settings.presentation.profile.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun rememberProfilePictureEditorLauncher(
    onPictureSelected: (ByteArray) -> Unit
): () -> Unit

@Composable
expect fun ProfilePictureImage(
    bytes: ByteArray,
    contentDescription: String?,
    modifier: Modifier = Modifier
)
