package com.cbgm.securechat.feature.onboarding.presentation.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.component.SecureChatSecondaryButton
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.identity.domain.model.PublicIdentity
import com.cbgm.securechat.feature.identity.presentation.setup.model.IdentityUiState
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_choose_another_number
import com.cbgm.securechat.resources.base_choose_phone_number
import com.cbgm.securechat.resources.base_generating_secure_identity
import com.cbgm.securechat.resources.base_identity_ready_opening
import com.cbgm.securechat.resources.base_phone_number
import com.cbgm.securechat.resources.feature_onboarding_approve_create_identity
import com.cbgm.securechat.resources.feature_onboarding_approve_phone_number
import com.cbgm.securechat.resources.feature_onboarding_detected_automatically_confirm
import com.cbgm.securechat.resources.feature_onboarding_input_your_name
import com.cbgm.securechat.resources.feature_onboarding_local_identity_incomplete
import com.cbgm.securechat.resources.feature_onboarding_no_automatic_number
import com.cbgm.securechat.resources.feature_onboarding_phone_routing_description
import com.cbgm.securechat.resources.feature_onboarding_preparing_phone_setup
import com.cbgm.securechat.resources.feature_onboarding_try_sim_number_again
import com.cbgm.securechat.resources.feature_onboarding_your_name
import org.jetbrains.compose.resources.stringResource

private val Field = Color(0xFF102A46)

@Composable
fun PhonePage(
    identityState: IdentityUiState,
    isCreating: Boolean,
    canRetryAutomatic: Boolean,
    onChooseAnotherNumber: () -> Unit,
    onRetryAutomaticNumber: () -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onApproveAndCreate: () -> Unit,
    onNameChanged: (String) -> Unit
) {
    Column(
        Modifier.padding(MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (identityState) {
            IdentityUiState.Loading -> {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                Text(
                    text = if (isCreating) stringResource(Res.string.base_generating_secure_identity) else stringResource(Res.string.feature_onboarding_preparing_phone_setup),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            is IdentityUiState.NoIdentity -> {
                Text(
                    text = stringResource(Res.string.feature_onboarding_approve_phone_number),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(MaterialTheme.spacing.base))
                Text(
                    text = stringResource(Res.string.feature_onboarding_phone_routing_description),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .74f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(MaterialTheme.spacing.medium))
                OutlinedTextField(
                    value = identityState.phoneNumber,
                    onValueChange = onPhoneNumberChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(stringResource(Res.string.base_phone_number))
                    },
                    placeholder = {
                        Text("+491701234567")
                    },
                    supportingText = {
                        Text(
                            text =
                                identityState.phoneNumberError
                                    ?: if (identityState.phoneNumber.isBlank()) stringResource(Res.string.feature_onboarding_no_automatic_number) else stringResource(Res.string.feature_onboarding_detected_automatically_confirm),
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    isError = identityState.phoneNumberError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    textStyle =
                        MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        ),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedContainerColor = Field,
                            unfocusedContainerColor = Field,
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .18f),
                            focusedLabelColor = MaterialTheme.colorScheme.secondary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f),
                            cursorColor = MaterialTheme.colorScheme.secondary
                        )
                )
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                OutlinedTextField(
                    value = identityState.name,
                    onValueChange = onNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(stringResource(Res.string.feature_onboarding_your_name))
                    },
                    placeholder = {
                        Text(stringResource(Res.string.feature_onboarding_your_name))
                    },
                    supportingText = {
                        Text(
                            text = stringResource(Res.string.feature_onboarding_input_your_name),
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    textStyle =
                        MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        ),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedContainerColor = Field,
                            unfocusedContainerColor = Field,
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .18f),
                            focusedLabelColor = MaterialTheme.colorScheme.secondary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f),
                            cursorColor = MaterialTheme.colorScheme.secondary
                        )
                )
                Spacer(Modifier.height(MaterialTheme.spacing.base))
                if (canRetryAutomatic) {
                    SecureChatSecondaryButton(
                        onClick = onRetryAutomaticNumber,
                        text = stringResource(Res.string.feature_onboarding_try_sim_number_again)
                    )
                    Spacer(Modifier.height(MaterialTheme.spacing.base))
                }
                SecureChatSecondaryButton(
                    onClick = onChooseAnotherNumber,
                    text = if (identityState.phoneNumber.isBlank()) stringResource(Res.string.base_choose_phone_number) else stringResource(Res.string.base_choose_another_number)
                )
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                SecureChatApprovalButton(
                    onClick = onApproveAndCreate,
                    enabled = identityState.phoneNumber.isNotBlank() && identityState.name.isNotBlank(),
                    text = stringResource(Res.string.feature_onboarding_approve_create_identity)
                )
            }

            is IdentityUiState.Ready -> {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(MaterialTheme.spacing.medium))
                Text(
                    text = stringResource(Res.string.base_identity_ready_opening),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            IdentityUiState.IncompleteIdentity -> {
                Text(
                    text = stringResource(Res.string.feature_onboarding_local_identity_incomplete),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            is IdentityUiState.Error -> {
                Text(
                    text = identityState.message,
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
    SecureChatTheme {
        PhonePage(
            identityState =
                IdentityUiState.Ready(
                    localPhoneNumber = "445446",
                    publicIdentity =
                        PublicIdentity(
                            ByteArray(size = 0),
                            ByteArray(size = 0)
                        )
                ),
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
    SecureChatTheme {
        PhonePage(
            identityState = IdentityUiState.NoIdentity(),
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
