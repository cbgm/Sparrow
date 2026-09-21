package com.cbgm.sparrow.feature.avatar.presentation.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowDialogListItem
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.feature.avatar.device.rememberImagePickerLauncher
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarEditResult
import com.cbgm.sparrow.feature.media.device.rememberCameraCaptureLauncher
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureConfig
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureType
import com.cbgm.sparrow.feature.media.domain.model.CameraLens
import org.koin.compose.viewmodel.koinViewModel

data class AvatarEditorStrings(
    val sourceTitle: String,
    val cropTitle: String,
    val takePhoto: String,
    val chooseFromGallery: String,
    val remove: String? = null,
    val cancel: String
)

@Composable
fun AvatarEditor(
    strings: AvatarEditorStrings,
    onAvatarSelected: (AvatarEditResult) -> Unit,
    onRemoveAvatar: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    cropInFullScreenDialog: Boolean = false
) {
    val viewModel = koinViewModel<AvatarEditorViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var sourceChooserVisible by remember { mutableStateOf(true) }

    val currentOnAvatarSelected by rememberUpdatedState(onAvatarSelected)

    LaunchedEffect(viewModel) {
        viewModel.result.collect { result ->
            currentOnAvatarSelected(result)
        }
    }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
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
            onCaptured = { captured ->
                sourceChooserVisible = false
                viewModel.onSourceSelected(captured.bytes)
            },
            onDismissed = { sourceChooserVisible = true },
            onError = { sourceChooserVisible = true }
        )
    val galleryLauncher =
        rememberImagePickerLauncher(
            onImageSelected = { source ->
                sourceChooserVisible = false
                viewModel.onSourceSelected(source)
            },
            onDismissed = { sourceChooserVisible = true },
            onError = { sourceChooserVisible = true }
        )

    uiState.image?.let { image ->
        val closeCrop = {
            viewModel.clear()
            onDismiss()
        }
        if (cropInFullScreenDialog) {
            // Identity lives inside the main tab pager. An inline crop screen is
            // constrained by that pager and can hide its Close/Confirm controls.
            // Use a separate full-screen window, as the camera already does.
            Dialog(
                onDismissRequest = closeCrop,
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = true
                )
            ) {
                Box(Modifier.fillMaxSize()) {
                    ProfilePictureCropScreen(
                        image = image,
                        title = strings.cropTitle,
                        isCropping = uiState.isCropping,
                        onConfirm = viewModel::onCropConfirmed,
                        onDismiss = closeCrop
                    )
                }
            }
        } else {
            ProfilePictureCropScreen(
                image = image,
                title = strings.cropTitle,
                isCropping = uiState.isCropping,
                onConfirm = viewModel::onCropConfirmed,
                onDismiss = closeCrop
            )
        }
        return
    }

    AvatarSourceDialog(
        isVisible = sourceChooserVisible && !uiState.isPreparing,
        strings = strings,
        onTakePhoto = {
            sourceChooserVisible = false
            cameraLauncher.launch()
        },
        onChooseFromGallery = {
            sourceChooserVisible = false
            galleryLauncher.launch()
        },
        onRemove =
            if (strings.remove != null && onRemoveAvatar != null) {
                {
                    viewModel.clear()
                    sourceChooserVisible = false
                    onRemoveAvatar()
                    onDismiss()
                }
            } else {
                null
            },
        onDismiss = {
            viewModel.clear()
            onDismiss()
        }
    )
}

@Composable
private fun AvatarSourceDialog(
    isVisible: Boolean,
    strings: AvatarEditorStrings,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit,
    onRemove: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    SparrowAlertDialog(
        isVisible = isVisible,
        onDismissRequest = onDismiss,
        title = strings.sourceTitle,
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                SparrowDialogListItem(text = strings.takePhoto, onClick = onTakePhoto)
                SparrowDialogListItem(text = strings.chooseFromGallery, onClick = onChooseFromGallery)
                if (strings.remove != null && onRemove != null) {
                    SparrowDialogListItem(text = strings.remove, onClick = onRemove)
                }
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
            isVisible = true,
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
            onRemove = {},
            onDismiss = {}
        )
    }
}
