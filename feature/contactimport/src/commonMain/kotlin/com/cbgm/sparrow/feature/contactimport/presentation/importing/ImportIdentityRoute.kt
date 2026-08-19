package com.cbgm.sparrow.feature.contactimport.presentation.importing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.core.extensions.toFingerprint
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.feature.contactimport.presentation.component.ScannedIdentityConfirmationDialog
import com.cbgm.sparrow.feature.contactimport.presentation.importing.model.ImportIdentityUiEvent
import com.cbgm.sparrow.feature.contactimport.presentation.scan.model.ScannedIdentityPreview
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityImportTrust
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityShareRepository
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_contactimport_trust_and_import
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ImportIdentityRoute(
    route: AppRoute.ImportContact,
    viewModel: ImportIdentityViewModel = koinViewModel(),
    identityShareRepository: IdentityShareRepository = koinInject()
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

        identityShareRepository
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
