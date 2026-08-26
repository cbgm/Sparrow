package com.cbgm.sparrow.feature.media.presentation.avatar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowDialogListItem
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.feature.media.device.cropAndEncodeProfilePicture
import com.cbgm.sparrow.feature.media.device.rememberCameraCaptureLauncher
import com.cbgm.sparrow.feature.media.device.rememberImagePickerLauncher
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureConfig
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureType
import com.cbgm.sparrow.feature.media.domain.model.CameraLens

data class AvatarEditorStrings(
    val sourceTitle: String,
    val cropTitle: String,
    val takePhoto: String,
    val chooseFromGallery: String,
    val cancel: String
)

@Composable
fun AvatarEditor(
    strings: AvatarEditorStrings,
    onAvatarSelected: (ByteArray) -> Unit,
    onDismiss: () -> Unit
) {
    var sourceBytes by remember { mutableStateOf<ByteArray?>(null) }
    var sourceChooserVisible by remember { mutableStateOf(true) }

    val onSourceSelected: (ByteArray) -> Unit = { bytes ->
        if (bytes.isNotEmpty()) {
            sourceChooserVisible = false
            sourceBytes = bytes
        } else {
            sourceChooserVisible = true
        }
    }

    val cameraLauncher =
        rememberCameraCaptureLauncher(
            config =
                CameraCaptureConfig(
                    allowedTypes = setOf(CameraCaptureType.PHOTO),
                    initialLens = CameraLens.FRONT,
                    initialType = CameraCaptureType.PHOTO
                ),
            onCaptured = { captured -> onSourceSelected(captured.bytes) },
            onDismissed = { sourceChooserVisible = true },
            onError = { sourceChooserVisible = true }
        )
    val galleryLauncher =
        rememberImagePickerLauncher(
            onImageSelected = onSourceSelected,
            onDismissed = { sourceChooserVisible = true },
            onError = { sourceChooserVisible = true }
        )

    sourceBytes?.let { bytes ->
        ProfilePictureCropScreen(
            sourceBytes = bytes,
            title = strings.cropTitle,
            onConfirm = { cropRegion ->
                cropAndEncodeProfilePicture(
                    sourceBytes = bytes,
                    cropRegion = cropRegion
                )?.let { cropped ->
                    sourceBytes = null
                    onAvatarSelected(cropped)
                }
            },
            onDismiss = onDismiss
        )
        return
    }

    if (sourceChooserVisible) {
        AvatarSourceDialog(
            strings = strings,
            onTakePhoto = {
                sourceChooserVisible = false
                cameraLauncher.launch()
            },
            onChooseFromGallery = {
                sourceChooserVisible = false
                galleryLauncher.launch()
            },
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun AvatarSourceDialog(
    strings: AvatarEditorStrings,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit,
    onDismiss: () -> Unit
) {
    SparrowAlertDialog(
        onDismissRequest = onDismiss,
        title = strings.sourceTitle,
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                SparrowDialogListItem(
                    text = strings.takePhoto,
                    onClick = onTakePhoto
                )
                SparrowDialogListItem(
                    text = strings.chooseFromGallery,
                    onClick = onChooseFromGallery
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            SparrowOutlinedButton(
                onClick = onDismiss,
                fillMaxWidth = false,
                text = strings.cancel
            )
        }
    )
}

@Preview
@Composable
private fun AvatarSourceDialogPreview() {
    SparrowTheme {
        AvatarSourceDialog(
            strings =
                AvatarEditorStrings(
                    sourceTitle = "Profile picture",
                    cropTitle = "Crop picture",
                    takePhoto = "Take photo",
                    chooseFromGallery = "Choose from gallery",
                    cancel = "Cancel"
                ),
            onTakePhoto = {},
            onChooseFromGallery = {},
            onDismiss = {}
        )
    }
}
