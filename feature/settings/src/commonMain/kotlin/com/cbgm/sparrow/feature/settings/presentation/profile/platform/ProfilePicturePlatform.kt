package com.cbgm.sparrow.feature.settings.presentation.profile.platform

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.cbgm.sparrow.feature.settings.presentation.profile.crop.ProfilePictureCropRegion
import org.jetbrains.compose.resources.decodeToImageBitmap

internal data class ProfilePictureSourceLauncher(
    val launchCamera: () -> Unit,
    val launchGallery: () -> Unit
)

@Composable
internal expect fun rememberProfilePictureSourceLauncher(
    onPictureSelected: (ByteArray) -> Unit
): ProfilePictureSourceLauncher

internal expect fun cropAndEncodeProfilePicture(
    sourceBytes: ByteArray,
    cropRegion: ProfilePictureCropRegion
): ByteArray?

@Composable
fun ProfilePictureImage(
    bytes: ByteArray,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val imageBitmap =
        remember(bytes) {
            runCatching { bytes.decodeToImageBitmap() }.getOrNull()
        }

    imageBitmap?.let { image ->
        Image(
            bitmap = image,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}
