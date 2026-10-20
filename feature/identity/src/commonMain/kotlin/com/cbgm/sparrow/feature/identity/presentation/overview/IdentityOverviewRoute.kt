package com.cbgm.sparrow.feature.identity.presentation.overview

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.avatar.presentation.editor.AvatarEditor
import com.cbgm.sparrow.feature.avatar.presentation.editor.AvatarEditorStrings
import com.cbgm.sparrow.feature.identity.presentation.overview.model.IdentityOverviewUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_settings_profile_picture
import com.cbgm.sparrow.resources.feature_settings_profile_picture_choose_gallery
import com.cbgm.sparrow.resources.feature_settings_profile_picture_crop
import com.cbgm.sparrow.resources.feature_settings_profile_picture_remove
import com.cbgm.sparrow.resources.feature_settings_profile_picture_take_photo
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IdentityOverviewRoute(
    scrollState: ScrollState,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: IdentityOverviewViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshBackupStatus()
        onPauseOrDispose { }
    }
    val pictureState = (uiState as? IdentityOverviewUiState.Ready)?.profilePicture
    var showAvatarEditor by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IdentityOverviewScreen(
            uiState = uiState,
            profilePictureState = pictureState,
            onUiEvent = viewModel::onUiEvent,
            scrollState = scrollState,
            innerPadding = innerPadding,
            onEditProfilePicture = { showAvatarEditor = true },
            modifier = Modifier
        )

        if (showAvatarEditor && uiState is IdentityOverviewUiState.Ready) {
            AvatarEditor(
                strings = AvatarEditorStrings(
                    sourceTitle = stringResource(Res.string.feature_settings_profile_picture),
                    cropTitle = stringResource(Res.string.feature_settings_profile_picture_crop),
                    takePhoto = stringResource(Res.string.feature_settings_profile_picture_take_photo),
                    chooseFromGallery = stringResource(Res.string.feature_settings_profile_picture_choose_gallery),
                    remove = stringResource(Res.string.feature_settings_profile_picture_remove)
                        .takeIf { pictureState?.hasPicture == true },
                    cancel = stringResource(Res.string.base_cancel)
                ),
                onAvatarSelected = { result ->
                    showAvatarEditor = false
                    viewModel.savePicture(result)
                },
                onRemoveAvatar = {
                    showAvatarEditor = false
                    viewModel.removePicture()
                },
                onDismiss = { showAvatarEditor = false },
                cropInFullScreenDialog = true
            )
        }
    }
}
