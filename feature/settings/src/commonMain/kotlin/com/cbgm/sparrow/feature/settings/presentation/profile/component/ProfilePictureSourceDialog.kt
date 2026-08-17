package com.cbgm.sparrow.feature.settings.presentation.profile.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowDialogListItem
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_settings_profile_picture
import com.cbgm.sparrow.resources.feature_settings_profile_picture_choose_gallery
import com.cbgm.sparrow.resources.feature_settings_profile_picture_take_photo
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProfilePictureSourceDialog(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onDismiss: () -> Unit
) {
    SparrowAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.feature_settings_profile_picture),
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                SparrowDialogListItem(
                    text = stringResource(Res.string.feature_settings_profile_picture_take_photo),
                    onClick = onCamera
                )
                SparrowDialogListItem(
                    text = stringResource(Res.string.feature_settings_profile_picture_choose_gallery),
                    onClick = onGallery
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            SparrowOutlinedButton(
                onClick = onDismiss,
                fillMaxWidth = false,
                text = stringResource(Res.string.base_cancel)
            )
        }
    )
}
