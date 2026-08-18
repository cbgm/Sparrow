package com.cbgm.sparrow.startup.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.cbgm.sparrow.core.ui.component.SparrowAnimation
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowCard
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_app_name
import com.cbgm.sparrow.resources.base_choose_another_number
import com.cbgm.sparrow.resources.base_choose_phone_number
import com.cbgm.sparrow.resources.base_continue_action
import com.cbgm.sparrow.resources.base_generating_secure_identity
import com.cbgm.sparrow.resources.base_identity_ready_opening
import com.cbgm.sparrow.resources.base_phone_number
import com.cbgm.sparrow.resources.base_retry
import com.cbgm.sparrow.resources.base_tagline
import com.cbgm.sparrow.resources.feature_startup_choose_number_or_enter
import com.cbgm.sparrow.resources.feature_startup_contacts_find_by_phone
import com.cbgm.sparrow.resources.feature_startup_detected_edit_or_choose
import com.cbgm.sparrow.resources.feature_startup_keys_generated_after_approval
import com.cbgm.sparrow.resources.feature_startup_opening_sparrow
import com.cbgm.sparrow.resources.feature_startup_partial_identity_no_replacement
import com.cbgm.sparrow.resources.feature_startup_preparing_sparrow
import com.cbgm.sparrow.resources.feature_startup_setup_failed
import com.cbgm.sparrow.resources.feature_startup_verify_phone_number
import com.cbgm.sparrow.startup.presentation.model.StartupUiEvent
import com.cbgm.sparrow.startup.presentation.model.StartupUiState
import org.jetbrains.compose.resources.stringResource

@Composable
fun StartupScreen(
    uiState: StartupUiState,
    identityUiState: IdentityUiState,
    onUiEvent: (StartupUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.primary)
                .padding(horizontal = MaterialTheme.spacing.screenPadding)
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SparrowAnimation(modifier = Modifier.size(Dimens.StartupScreen.animationSize), true)

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.startupScreen.titleGap))

            Text(
                text = stringResource(Res.string.base_app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

            Text(
                text = stringResource(Res.string.base_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = Alpha.OpaqueText)
            )

            Spacer(
                modifier = Modifier.height(MaterialTheme.spacing.medium)
            )

            SparrowCard(modifier = Modifier.widthIn(max = Dimens.StartupScreen.contentMaxWidth)) {
                AnimatedContent(
                    targetState = uiState,
                    transitionSpec = {
                        fadeIn(
                            animationSpec = tween(300)
                        ) togetherWith
                            fadeOut(
                                animationSpec = tween(180)
                            )
                    },
                    label = "startupState"
                ) { state ->
                    Box(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                        StartupStateContent(
                            uiState = state,
                            identityUiState = identityUiState,
                            onUiEvent = onUiEvent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        }
    }
}

@Composable
private fun StartupStateContent(
    uiState: StartupUiState,
    identityUiState: IdentityUiState,
    onUiEvent: (StartupUiEvent) -> Unit
) {
    when (uiState) {
        StartupUiState.Loading -> {
            StartupProgress(message = stringResource(Res.string.feature_startup_preparing_sparrow))
        }

        StartupUiState.Ready -> {
            StartupProgress(message = stringResource(Res.string.feature_startup_opening_sparrow))
        }

        StartupUiState.IdentityRequired -> {
            StartupIdentityContent(
                identityUiState = identityUiState,
                onRequestPhoneNumberHint = { onUiEvent(StartupUiEvent.RequestPhoneNumberHint) },
                onPhoneNumberChanged = { value ->
                    onUiEvent(StartupUiEvent.PhoneNumberChanged(value))
                },
                onCreateIdentity = { onUiEvent(StartupUiEvent.CreateIdentityClicked) },
                onRetry = { onUiEvent(StartupUiEvent.RetryClicked) }
            )
        }

        is StartupUiState.Error -> {
            StartupErrorContent(
                message = uiState.message,
                onRetry = { onUiEvent(StartupUiEvent.RetryClicked) }
            )
        }
    }
}

@Composable
private fun StartupIdentityContent(
    identityUiState: IdentityUiState,
    onRequestPhoneNumberHint: () -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onCreateIdentity: () -> Unit,
    onRetry: () -> Unit
) {
    when (identityUiState) {
        IdentityUiState.Loading -> {
            StartupProgress(message = stringResource(Res.string.base_generating_secure_identity))
        }

        is IdentityUiState.NoIdentity -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(Res.string.feature_startup_verify_phone_number),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier = Modifier.height(MaterialTheme.spacing.base)
                )

                Text(
                    text = stringResource(Res.string.feature_startup_contacts_find_by_phone),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.OpaqueText),
                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier = Modifier.height(MaterialTheme.spacing.medium)
                )

                OutlinedTextField(
                    value = identityUiState.phoneNumber,
                    onValueChange = onPhoneNumberChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(text = stringResource(Res.string.base_phone_number))
                    },
                    placeholder = {
                        Text(text = "+491701234567", style = MaterialTheme.typography.bodyMedium)
                    },
                    supportingText = {
                        Text(
                            text =
                                identityUiState.phoneNumberError
                                    ?: if (identityUiState.phoneNumber.isBlank()) {
                                        stringResource(Res.string.feature_startup_choose_number_or_enter)
                                    } else {
                                        stringResource(Res.string.feature_startup_detected_edit_or_choose)
                                    }
                        )
                    },
                    isError = identityUiState.phoneNumberError != null,
                    singleLine = true,
                    textStyle =
                        MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        ),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.OpaqueText),
                            focusedContainerColor = StartupPhoneFieldBackground,
                            unfocusedContainerColor = StartupPhoneFieldBackground,
                            errorContainerColor = StartupPhoneFieldBackground,
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.TextField.unfocusedBorder),
                            errorBorderColor = MaterialTheme.colorScheme.error,
                            focusedLabelColor = MaterialTheme.colorScheme.secondary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.OpaqueText),
                            errorLabelColor = MaterialTheme.colorScheme.error,
                            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.TextField.placeholder),
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.TextField.placeholder),
                            focusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.OpaqueText),
                            unfocusedSupportingTextColor =
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = Alpha.OpaqueText
                                ),
                            errorSupportingTextColor = MaterialTheme.colorScheme.error,
                            cursorColor = MaterialTheme.colorScheme.secondary,
                            errorCursorColor = MaterialTheme.colorScheme.error
                        ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                OutlinedButton(
                    onClick = onRequestPhoneNumberHint,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text =
                            if (identityUiState.phoneNumber.isBlank()) {
                                stringResource(Res.string.base_choose_phone_number)
                            } else {
                                stringResource(Res.string.base_choose_another_number)
                            }
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                SparrowApprovalButton(
                    onClick = onCreateIdentity,
                    enabled = identityUiState.phoneNumber.isNotBlank(),
                    text = stringResource(Res.string.base_continue_action)
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

                Text(
                    text = stringResource(Res.string.feature_startup_keys_generated_after_approval),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        is IdentityUiState.Ready -> {
            StartupProgress(message = stringResource(Res.string.base_identity_ready_opening))
        }

        IdentityUiState.IncompleteIdentity -> {
            StartupErrorContent(
                message = stringResource(Res.string.feature_startup_partial_identity_no_replacement),
                onRetry = onRetry
            )
        }

        is IdentityUiState.Error -> {
            StartupErrorContent(
                message = identityUiState.message,
                onRetry = onRetry
            )
        }
    }
}

@Composable
private fun StartupErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.feature_startup_setup_failed),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.base)
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        SparrowApprovalButton(
            onClick = onRetry,
            text = stringResource(Res.string.base_retry)
        )
    }
}

@Composable
private fun StartupProgress(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.StartupScreen.progressTrack)
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.OpaqueText),
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
private fun StartupScreenPreview() {
    SparrowTheme {
        StartupScreen(
            uiState = StartupUiState.IdentityRequired,
            identityUiState = IdentityUiState.NoIdentity(),
            onUiEvent = {}
        )
    }
}

private val StartupPhoneFieldBackground =
    Color(0xFF0B2035)
