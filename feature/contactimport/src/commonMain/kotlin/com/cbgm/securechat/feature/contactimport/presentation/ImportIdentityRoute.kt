package com.cbgm.securechat.feature.contactimport.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.core.extensions.toFingerprint
import com.cbgm.securechat.core.ui.navigation.AppRoute
import com.cbgm.securechat.feature.contactimport.presentation.model.ImportIdentityUiEvent
import com.cbgm.securechat.feature.contactimport.presentation.model.ScannedIdentityPreview
import com.cbgm.securechat.feature.contactimport.presentation.screen.ImportIdentityScreen
import com.cbgm.securechat.feature.contactimport.presentation.screen.ImportIdentityViewModel
import com.cbgm.securechat.feature.contactimport.presentation.screen.components.ScannedIdentityConfirmationDialog
import com.cbgm.securechat.feature.contacts.domain.model.IdentityImportTrust
import com.cbgm.securechat.feature.identity.domain.service.IdentityShareCodec
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_contactimport_trust_and_import
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ImportIdentityRoute(
    route: AppRoute.ImportContact,
    viewModel: ImportIdentityViewModel =
        koinViewModel(
            parameters = {
                parametersOf(route)
            }
        ),
    identityShareCodec: IdentityShareCodec = koinInject()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingScannedIdentity by remember(route.scannedIdentity) {
        mutableStateOf(route.scannedIdentity)
    }
    var scannedIdentityPreview by remember { mutableStateOf<ScannedIdentityPreview?>(null) }

    LaunchedEffect(pendingScannedIdentity) {
        val encodedIdentity =
            pendingScannedIdentity
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: return@LaunchedEffect

        identityShareCodec
            .decode(encodedValue = encodedIdentity)
            .onSuccess { payload ->
                scannedIdentityPreview =
                    ScannedIdentityPreview(
                        encodedIdentity = encodedIdentity,
                        displayName = payload.contactDetails.displayName,
                        phoneNumber = payload.contactDetails.phoneNumber,
                        signingKeyFingerprint = payload.signingPublicKey.toFingerprint(),
                        encryptionKeyFingerprint = payload.encryptionPublicKey.toFingerprint()
                    )
            }.onFailure {
                viewModel.onUiEvent(
                    ImportIdentityUiEvent.EncodedIdentityChanged(encodedIdentity)
                )
            }

        pendingScannedIdentity = null
    }

    ImportIdentityScreen(
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent,
        importContactId = route.contactId
    )

    scannedIdentityPreview?.let { preview ->
        ScannedIdentityConfirmationDialog(
            preview = preview,
            confirmButtonText = stringResource(Res.string.feature_contactimport_trust_and_import),
            onConfirm = {
                scannedIdentityPreview = null
                viewModel.onUiEvent(
                    ImportIdentityUiEvent.EncodedIdentityChanged(preview.encodedIdentity)
                )
                viewModel.onUiEvent(
                    ImportIdentityUiEvent.ImportClicked(
                        contactId = route.contactId,
                        identityImportTrust = IdentityImportTrust.VERIFIED_IN_PERSON
                    )
                )
            },
            onDismiss = {
                scannedIdentityPreview = null
            }
        )
    }
}
