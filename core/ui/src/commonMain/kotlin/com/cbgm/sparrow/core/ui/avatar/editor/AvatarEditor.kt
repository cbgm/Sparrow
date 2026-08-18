package com.cbgm.sparrow.core.ui.avatar.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.cbgm.sparrow.core.ui.avatar.editor.platform.cropAndEncodeProfilePicture
import com.cbgm.sparrow.core.ui.avatar.editor.platform.rememberProfilePictureSourceLauncher
import com.cbgm.sparrow.core.ui.avatar.editor.screen.ProfilePictureCropScreen
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowDialogListItem
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton

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

    val launcher =
        rememberProfilePictureSourceLauncher(
            onPictureSelected = { bytes ->
                if (bytes.isNotEmpty()) {
                    sourceChooserVisible = false
                    sourceBytes = bytes
                } else {
                    sourceChooserVisible = true
                }
            },
            onSourceDismissed = { sourceChooserVisible = true }
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
        SparrowAlertDialog(
            onDismissRequest = onDismiss,
            title = strings.sourceTitle,
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SparrowDialogListItem(
                        text = strings.takePhoto,
                        onClick = {
                            sourceChooserVisible = false
                            launcher.launchCamera()
                        }
                    )
                    SparrowDialogListItem(
                        text = strings.chooseFromGallery,
                        onClick = {
                            sourceChooserVisible = false
                            launcher.launchGallery()
                        }
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
}
