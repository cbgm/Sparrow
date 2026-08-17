package com.cbgm.sparrow.feature.settings.presentation.profile.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
actual fun rememberProfilePictureEditorLauncher(
    onPictureSelected: (ByteArray) -> Unit
): () -> Unit = remember { {} }

@Composable
actual fun ProfilePictureImage(
    bytes: ByteArray,
    contentDescription: String?,
    modifier: Modifier
) = Unit
