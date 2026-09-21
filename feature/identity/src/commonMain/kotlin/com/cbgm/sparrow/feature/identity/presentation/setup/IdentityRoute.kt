package com.cbgm.sparrow.feature.identity.presentation.setup

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.avatar.presentation.editor.AvatarEditor
import com.cbgm.sparrow.feature.avatar.presentation.editor.AvatarEditorStrings
import com.cbgm.sparrow.feature.identity.device.PhoneNumberHintLauncher
import com.cbgm.sparrow.feature.identity.device.PhoneNumberHintResult
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityUiEvent
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityUiState
import com.cbgm.sparrow.feature.identity.presentation.setup.profile.IdentityProfilePictureViewModel
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
fun IdentityRoute(
    scrollState: ScrollState,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onIdentityReady: () -> Unit = {},
    viewModel: IdentityViewModel =
        koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pictureViewModel: IdentityProfilePictureViewModel? =
        if (uiState is IdentityUiState.Ready) koinViewModel<IdentityProfilePictureViewModel>() else null
    val pictureState = pictureViewModel?.uiState?.collectAsStateWithLifecycle()
    var showAvatarEditor by remember { mutableStateOf(false) }

    var phoneNumberHintRequestId by remember { mutableIntStateOf(0) }

    val canRequestPhoneNumber = uiState is IdentityUiState.NoIdentity

    PhoneNumberHintLauncher(
        requestId = phoneNumberHintRequestId,
        enabled = canRequestPhoneNumber,
        onResult = { result ->
            when (result) {
                is PhoneNumberHintResult.Selected -> {
                    viewModel.onSuggestedPhoneNumber(phoneNumber = result.phoneNumber)
                }

                PhoneNumberHintResult.Unavailable -> {
                    viewModel.onPhoneNumberHintUnavailable()
                }

                PhoneNumberHintResult.Cancelled -> {
                    /*
                     * Manual entry remains visible. Cancellation is
                     * therefore not treated as an error.
                     */
                }

                is PhoneNumberHintResult.Failed -> {
                    viewModel.onPhoneNumberHintFailed(message = result.message)
                }
            }
        }
    )

    LaunchedEffect(uiState) {
        if (uiState is IdentityUiState.Ready) {
            onIdentityReady()
        }
    }

    Box(modifier = modifier) {
        IdentityScreen(
            uiState = uiState,
            onUiEvent = { event ->
                if (event == IdentityUiEvent.RequestPhoneNumberHint) {
                    phoneNumberHintRequestId += 1
                } else {
                    viewModel.onUiEvent(event)
                }
            },
            scrollState = scrollState,
            innerPadding = innerPadding,
            profilePictureState = pictureState?.value,
            onEditProfilePicture = { showAvatarEditor = true },
            modifier = Modifier
        )

        if (showAvatarEditor && uiState is IdentityUiState.Ready && pictureViewModel != null) {
            AvatarEditor(
                strings = AvatarEditorStrings(
                    sourceTitle = stringResource(Res.string.feature_settings_profile_picture),
                    cropTitle = stringResource(Res.string.feature_settings_profile_picture_crop),
                    takePhoto = stringResource(Res.string.feature_settings_profile_picture_take_photo),
                    chooseFromGallery = stringResource(Res.string.feature_settings_profile_picture_choose_gallery),
                    remove = stringResource(Res.string.feature_settings_profile_picture_remove)
                        .takeIf { pictureState?.value?.hasPicture == true },
                    cancel = stringResource(Res.string.base_cancel)
                ),
                onAvatarSelected = { result ->
                    showAvatarEditor = false
                    pictureViewModel.savePicture(result)
                },
                onRemoveAvatar = {
                    showAvatarEditor = false
                    pictureViewModel.removePicture()
                },
                onDismiss = { showAvatarEditor = false }
            )
        }
    }
}
