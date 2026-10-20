package com.cbgm.sparrow.feature.onboarding.presentation.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.identity.presentation.create.model.IdentityCreateUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_choose_another_number
import com.cbgm.sparrow.resources.base_choose_phone_number
import com.cbgm.sparrow.resources.base_generating_secure_identity
import com.cbgm.sparrow.resources.base_identity_ready_opening
import com.cbgm.sparrow.resources.base_phone_number
import com.cbgm.sparrow.resources.feature_identity_backup_restore_action
import com.cbgm.sparrow.resources.feature_identity_something_went_wrong
import com.cbgm.sparrow.resources.feature_onboarding_approve_create_identity
import com.cbgm.sparrow.resources.feature_onboarding_approve_phone_number
import com.cbgm.sparrow.resources.feature_onboarding_detected_automatically_confirm
import com.cbgm.sparrow.resources.feature_onboarding_input_your_name
import com.cbgm.sparrow.resources.feature_onboarding_local_identity_incomplete
import com.cbgm.sparrow.resources.feature_onboarding_no_automatic_number
import com.cbgm.sparrow.resources.feature_onboarding_phone_routing_description
import com.cbgm.sparrow.resources.feature_onboarding_preparing_phone_setup
import com.cbgm.sparrow.resources.feature_onboarding_try_sim_number_again
import com.cbgm.sparrow.resources.feature_onboarding_your_name
import org.jetbrains.compose.resources.stringResource

@Composable
fun PhonePage(
    identityState: IdentityCreateUiState,
    isCreating: Boolean,
    canRetryAutomatic: Boolean,
    onChooseAnotherNumber: () -> Unit,
    onRetryAutomaticNumber: () -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onApproveAndCreate: () -> Unit,
    onRestoreIdentity: () -> Unit = {},
    onNameChanged: (String) -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (identityState) {
            IdentityCreateUiState.Loading, IdentityCreateUiState.Creating -> {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                Text(
                    text = if (isCreating) {
                        stringResource(Res.string.base_generating_secure_identity)
                    } else {
                        stringResource(
                            Res.string.feature_onboarding_preparing_phone_setup
                        )
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            is IdentityCreateUiState.Form -> {
                Text(
                    text = stringResource(Res.string.feature_onboarding_approve_phone_number),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(MaterialTheme.spacing.base))
                Text(
                    text = stringResource(Res.string.feature_onboarding_phone_routing_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(MaterialTheme.spacing.medium))
                Column(modifier = Modifier.fillMaxWidth()) {
                    SparrowInputField(
                        value = identityState.phoneNumber,
                        onValueChange = onPhoneNumberChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(Res.string.base_phone_number),
                        placeholderText = "+491701234567",
                        errorText = if (identityState.phoneNumber.isBlank()) {
                            stringResource(Res.string.feature_onboarding_no_automatic_number)
                        } else {
                            stringResource(
                                Res.string.feature_onboarding_detected_automatically_confirm
                            )
                        },
                        isSingleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(Modifier.height(MaterialTheme.spacing.small))
                    SparrowInputField(
                        value = identityState.name,
                        onValueChange = onNameChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(Res.string.feature_onboarding_your_name),
                        placeholderText = stringResource(Res.string.feature_onboarding_your_name),
                        errorText = stringResource(Res.string.feature_onboarding_input_your_name),
                        isSingleLine = true
                    )
                }
                Spacer(Modifier.height(MaterialTheme.spacing.base))
                if (canRetryAutomatic) {
                    TextButton(onClick = onRetryAutomaticNumber) {
                        Text(stringResource(Res.string.feature_onboarding_try_sim_number_again))
                    }
                }
                TextButton(onClick = onChooseAnotherNumber) {
                    Text(
                        if (identityState.phoneNumber.isBlank()) {
                            stringResource(Res.string.base_choose_phone_number)
                        } else {
                            stringResource(Res.string.base_choose_another_number)
                        }
                    )
                }
                Spacer(Modifier.height(MaterialTheme.spacing.base))
                SparrowApprovalButton(
                    onClick = onApproveAndCreate,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = identityState.phoneNumber.isNotBlank() && identityState.name.isNotBlank(),
                    text = stringResource(Res.string.feature_onboarding_approve_create_identity)
                )
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                TextButton(
                    onClick = onRestoreIdentity,
                    enabled = identityState.phoneNumber.isNotBlank() && identityState.name.isNotBlank() && !isCreating
                ) {
                    Text(stringResource(Res.string.feature_identity_backup_restore_action))
                }
            }

            IdentityCreateUiState.Completed -> {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(MaterialTheme.spacing.medium))
                Text(
                    text = stringResource(Res.string.base_identity_ready_opening),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            IdentityCreateUiState.IncompleteIdentity -> {
                Text(
                    text = stringResource(Res.string.feature_onboarding_local_identity_incomplete),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            IdentityCreateUiState.Error -> {
                Text(
                    text = stringResource(Res.string.feature_identity_something_went_wrong),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview
@Composable
private fun PhonePagePreview() {
    SparrowTheme {
        PhonePage(
            identityState =
                IdentityCreateUiState.Completed,
            isCreating = false,
            canRetryAutomatic = true,
            onChooseAnotherNumber = {},
            onRetryAutomaticNumber = {},
            onPhoneNumberChanged = {},
            onApproveAndCreate = {},
            onNameChanged = {}
        )
    }
}

@Preview
@Composable
private fun PhonePageNoIdentityPreview() {
    SparrowTheme {
        PhonePage(
            identityState = IdentityCreateUiState.Form(),
            isCreating = false,
            canRetryAutomatic = true,
            onChooseAnotherNumber = {},
            onRetryAutomaticNumber = {},
            onPhoneNumberChanged = {},
            onApproveAndCreate = {},
            onNameChanged = {}
        )
    }
}
