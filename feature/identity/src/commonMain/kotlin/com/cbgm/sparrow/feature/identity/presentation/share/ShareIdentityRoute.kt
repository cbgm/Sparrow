package com.cbgm.sparrow.feature.identity.presentation.share

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.identity.device.rememberIdentityShareLauncher
import com.cbgm.sparrow.feature.identity.presentation.share.model.ShareIdentityUiEvent
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_identity_share_identity_text
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ShareIdentityRoute(
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
    viewModel: ShareIdentityViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val shareIdentity =
        rememberIdentityShareLauncher(
            encodedIdentity = uiState.encodedIdentity.orEmpty(),
            shareTitle = stringResource(Res.string.feature_identity_share_identity_text)
        )

    ShareIdentityScreen(
        uiState = uiState,
        onUiEvent = { event ->
            if (event == ShareIdentityUiEvent.ShareClicked) {
                shareIdentity()
            } else {
                viewModel.onUiEvent(event)
            }
        },
        showBackButton = showBackButton,
        modifier = modifier
    )
}
