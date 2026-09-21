package com.cbgm.sparrow.feature.onboarding.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.identity.device.IdentityBackupDocumentLauncher
import com.cbgm.sparrow.feature.identity.device.PhoneNumberHintLauncher
import com.cbgm.sparrow.feature.identity.device.PhoneNumberHintResult
import com.cbgm.sparrow.feature.identity.presentation.setup.IdentityViewModel
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityUiEvent
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityUiState
import com.cbgm.sparrow.feature.onboarding.device.AutomaticPhoneNumberReader
import com.cbgm.sparrow.feature.onboarding.device.AutomaticPhoneNumberResult
import com.cbgm.sparrow.feature.onboarding.device.OnboardingPermissionRequester
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingPage
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingUiEvent
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_identity_backup_password
import com.cbgm.sparrow.resources.feature_identity_backup_restore_action
import com.cbgm.sparrow.resources.feature_identity_backup_restore_hint
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingRoute(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
    identityViewModel: IdentityViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val identityState by identityViewModel.uiState.collectAsStateWithLifecycle()
    val backupState by identityViewModel.backupState.collectAsStateWithLifecycle()
    var importRequestId by remember { mutableIntStateOf(0) }
    var importDocument by remember { mutableStateOf<ByteArray?>(null) }
    var backupPassword by remember { mutableStateOf("") }
    IdentityBackupDocumentLauncher(
        exportRequest = null,
        importRequestId = importRequestId,
        onExportResult = { _, _ -> },
        onImportResult = { document, error ->
            if (document != null) {
                importDocument = document
                backupPassword = ""
            } else if (error != null && error != "Backup selection cancelled") {
                identityViewModel.showBackupError(error)
            }
        }
    )
    if (importDocument != null && identityState is IdentityUiState.NoIdentity) {
        AlertDialog(
            onDismissRequest = {
                importDocument = null
                backupPassword = ""
            },
            title = { Text(stringResource(Res.string.feature_identity_backup_restore_action)) },
            text = {
                androidx.compose.foundation.layout.Column {
                    Text(stringResource(Res.string.feature_identity_backup_restore_hint))
                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        label = { Text(stringResource(Res.string.feature_identity_backup_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = backupPassword.isNotEmpty() && !backupState.busy,
                    onClick = {
                        val password = backupPassword.toCharArray()
                        backupPassword = ""
                        identityViewModel.restoreBackup(importDocument!!, password)
                        importDocument = null
                    }
                ) { Text(stringResource(Res.string.feature_identity_backup_restore_action)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    importDocument = null
                    backupPassword = ""
                }) {
                    Text(stringResource(Res.string.base_cancel))
                }
            }
        )
    }
    var hintRequestId by remember { mutableIntStateOf(0) }

    OnboardingPermissionRequester(
        requestId = state.permissionRequestId,
        onResult = viewModel::onPermissionsResult
    )

    AutomaticPhoneNumberReader(
        requestId = state.automaticPhoneRequestId,
        enabled = state.page == OnboardingPage.PHONE && state.phonePermissionGranted,
        onResult = { result ->
            handleAutomaticPhoneNumberResult(result, identityViewModel)
        }
    )

    PhoneNumberHintLauncher(
        requestId = hintRequestId,
        enabled = state.page == OnboardingPage.PHONE,
        onResult = { result ->
            handlePhoneNumberHintResult(result, identityViewModel)
        }
    )

    LaunchedEffect(identityState) {
        when (identityState) {
            is IdentityUiState.Ready -> onComplete()
            IdentityUiState.Loading -> viewModel.setCreatingIdentity(true)
            else -> viewModel.setCreatingIdentity(false)
        }
    }

    OnboardingScreen(
        state = state,
        identityState = identityState,
        backupError = backupState.message?.takeIf { backupState.error },
        isRestoring = backupState.busy,
        onRestoreIdentity = { importRequestId += 1 },
        onUiEvent = { event ->
            handleOnboardingUiEvent(
                event = event,
                viewModel = viewModel,
                identityViewModel = identityViewModel,
                onChooseAnotherNumber = { hintRequestId += 1 }
            )
        }
    )
}

private fun handleOnboardingUiEvent(
    event: OnboardingUiEvent,
    viewModel: OnboardingViewModel,
    identityViewModel: IdentityViewModel,
    onChooseAnotherNumber: () -> Unit
) {
    when (event) {
        OnboardingUiEvent.ChooseAnotherNumberClicked -> onChooseAnotherNumber()
        is OnboardingUiEvent.PhoneNumberChanged ->
            identityViewModel.onUiEvent(IdentityUiEvent.PhoneNumberChanged(event.value))
        is OnboardingUiEvent.NameChanged ->
            identityViewModel.onUiEvent(IdentityUiEvent.NameChanged(event.value))
        OnboardingUiEvent.ApproveAndCreateClicked ->
            identityViewModel.onUiEvent(IdentityUiEvent.CreateIdentityClicked)
        else -> viewModel.onUiEvent(event)
    }
}

private fun handleAutomaticPhoneNumberResult(
    result: AutomaticPhoneNumberResult,
    identityViewModel: IdentityViewModel
) {
    when (result) {
        is AutomaticPhoneNumberResult.Found -> identityViewModel.onSuggestedPhoneNumber(result.phoneNumber)
        AutomaticPhoneNumberResult.Unavailable -> Unit
        is AutomaticPhoneNumberResult.Failed -> identityViewModel.onPhoneNumberHintFailed(result.message)
    }
}

private fun handlePhoneNumberHintResult(
    result: PhoneNumberHintResult,
    identityViewModel: IdentityViewModel
) {
    when (result) {
        is PhoneNumberHintResult.Selected -> identityViewModel.onSuggestedPhoneNumber(result.phoneNumber)
        PhoneNumberHintResult.Unavailable -> identityViewModel.onPhoneNumberHintUnavailable()
        PhoneNumberHintResult.Cancelled -> Unit
        is PhoneNumberHintResult.Failed -> identityViewModel.onPhoneNumberHintFailed(result.message)
    }
}
