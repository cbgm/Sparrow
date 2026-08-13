package com.cbgm.securechat.feature.identity.presentation.setup

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.identity.presentation.platform.PhoneNumberHintLauncher
import com.cbgm.securechat.feature.identity.presentation.platform.PhoneNumberHintResult
import com.cbgm.securechat.feature.identity.presentation.setup.IdentityScreen
import com.cbgm.securechat.feature.identity.presentation.setup.IdentityViewModel
import com.cbgm.securechat.feature.identity.presentation.setup.model.IdentityUiEvent
import com.cbgm.securechat.feature.identity.presentation.setup.model.IdentityUiState
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
        modifier = modifier
    )
}
