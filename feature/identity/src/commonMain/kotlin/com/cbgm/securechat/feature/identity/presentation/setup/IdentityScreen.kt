package com.cbgm.securechat.feature.identity.presentation.setup

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.component.SecureChatCard
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.identity.domain.model.PublicIdentity
import com.cbgm.securechat.feature.identity.presentation.setup.components.IconBadge
import com.cbgm.securechat.feature.identity.presentation.setup.components.PublicKeySection
import com.cbgm.securechat.feature.identity.presentation.setup.model.IdentityUiEvent
import com.cbgm.securechat.feature.identity.presentation.setup.model.IdentityUiState
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_retry
import com.cbgm.securechat.resources.base_securechat
import com.cbgm.securechat.resources.feature_identity_approve_number_create_identity
import com.cbgm.securechat.resources.feature_identity_check_again
import com.cbgm.securechat.resources.feature_identity_checking_secure_identity
import com.cbgm.securechat.resources.feature_identity_choose_phone_number_from_device
import com.cbgm.securechat.resources.feature_identity_encryption_public_key
import com.cbgm.securechat.resources.feature_identity_encryption_public_key_description
import com.cbgm.securechat.resources.feature_identity_identity_enter_phone_description
import com.cbgm.securechat.resources.feature_identity_identity_ready
import com.cbgm.securechat.resources.feature_identity_incomplete_identity
import com.cbgm.securechat.resources.feature_identity_incomplete_identity_description
import com.cbgm.securechat.resources.feature_identity_private_keys_protected
import com.cbgm.securechat.resources.feature_identity_share_my_identity
import com.cbgm.securechat.resources.feature_identity_signing_public_key
import com.cbgm.securechat.resources.feature_identity_signing_public_key_description
import com.cbgm.securechat.resources.feature_identity_something_went_wrong
import com.cbgm.securechat.resources.feature_identity_stable_relay_address_description
import com.cbgm.securechat.resources.feature_identity_your_phone_number
import org.jetbrains.compose.resources.stringResource

/** Public identity screen contract. */
private val Field = Color(0xFF102A46)

@Composable
fun IdentityScreen(
    uiState: IdentityUiState,
    onUiEvent: (IdentityUiEvent) -> Unit,
    scrollState: ScrollState,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding(),
                start = MaterialTheme.spacing.screenPadding,
                end = MaterialTheme.spacing.screenPadding
            )
    ) {
        when (uiState) {
            IdentityUiState.Loading -> {
                LoadingContent()
            }

            is IdentityUiState.NoIdentity -> {
                NoIdentityContent(
                    phoneNumber = uiState.phoneNumber,
                    phoneNumberError = uiState.phoneNumberError,
                    onRequestPhoneNumberHint = { onUiEvent(IdentityUiEvent.RequestPhoneNumberHint) },
                    onPhoneNumberChanged = { onUiEvent(IdentityUiEvent.PhoneNumberChanged(it)) },
                    onCreateIdentity = { onUiEvent(IdentityUiEvent.CreateIdentityClicked) }
                )
            }

            is IdentityUiState.Ready -> {
                ReadyIdentityContent(
                    publicIdentity = uiState.publicIdentity,
                    localPhoneNumber = uiState.localPhoneNumber,
                    onShareIdentity = { onUiEvent(IdentityUiEvent.ShareIdentityClicked) }
                )
            }

            IdentityUiState.IncompleteIdentity -> {
                IncompleteIdentityContent(onRetry = {
                    onUiEvent(IdentityUiEvent.RetryClicked)
                })
            }

            is IdentityUiState.Error -> {
                ErrorContent(message = uiState.message, onRetry = {
                    onUiEvent(IdentityUiEvent.RetryClicked)
                })
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Text(
            text = stringResource(Res.string.feature_identity_checking_secure_identity),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun NoIdentityContent(
    phoneNumber: String,
    phoneNumberError: String?,
    onRequestPhoneNumberHint: () -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onCreateIdentity: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        IconBadge(icon = Icons.Default.Shield)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Text(
            text = stringResource(Res.string.base_securechat),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(Res.string.feature_identity_identity_enter_phone_description),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        SecureChatCard {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                OutlinedButton(
                    onClick = onRequestPhoneNumberHint,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(Res.string.feature_identity_choose_phone_number_from_device))
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = onPhoneNumberChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(Res.string.feature_identity_your_phone_number)) },
                    placeholder = { Text(text = "+491701234567") },
                    supportingText = {
                        Text(
                            text =
                                phoneNumberError
                                    ?: stringResource(Res.string.feature_identity_stable_relay_address_description),
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    isError = phoneNumberError != null,
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
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f),
                            focusedLabelColor = MaterialTheme.colorScheme.secondary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                            cursorColor = MaterialTheme.colorScheme.secondary,
                            errorBorderColor = MaterialTheme.colorScheme.error,
                            errorLabelColor = MaterialTheme.colorScheme.error,
                            errorCursorColor = MaterialTheme.colorScheme.error
                        )
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                SecureChatApprovalButton(
                    onClick = onCreateIdentity,
                    enabled = phoneNumber.isNotBlank(),
                    text = stringResource(Res.string.feature_identity_approve_number_create_identity)
                )
            }
        }
    }
}

@Composable
private fun ReadyIdentityContent(
    publicIdentity: PublicIdentity,
    localPhoneNumber: String,
    onShareIdentity: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        IconBadge(icon = Icons.Default.VerifiedUser)
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Text(
            text = stringResource(Res.string.feature_identity_identity_ready),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(Res.string.feature_identity_private_keys_protected),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = Field
        ) {
            Text(
                text = localPhoneNumber,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        SecureChatCard {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                PublicKeySection(
                    icon = Icons.Default.Lock,
                    title = stringResource(Res.string.feature_identity_encryption_public_key),
                    description = stringResource(Res.string.feature_identity_encryption_public_key_description),
                    key = publicIdentity.encryptionPublicKey
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                PublicKeySection(
                    icon = Icons.Default.Key,
                    title = stringResource(Res.string.feature_identity_signing_public_key),
                    description = stringResource(Res.string.feature_identity_signing_public_key_description),
                    key = publicIdentity.signingPublicKey
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                Button(
                    onClick = onShareIdentity,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color(0xFF071A2E)
                        )
                ) {
                    Text(
                        text = stringResource(Res.string.feature_identity_share_my_identity),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun IncompleteIdentityContent(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        IconBadge(icon = Icons.Default.ErrorOutline, tint = MaterialTheme.colorScheme.error)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Text(
            text = stringResource(Res.string.feature_identity_incomplete_identity),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Text(
            text = stringResource(Res.string.feature_identity_incomplete_identity_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        SecureChatApprovalButton(onClick = onRetry, text = stringResource(Res.string.feature_identity_check_again))
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        IconBadge(icon = Icons.Default.ErrorOutline, tint = MaterialTheme.colorScheme.error)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Text(
            text = stringResource(Res.string.feature_identity_something_went_wrong),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        SecureChatApprovalButton(onClick = onRetry, text = stringResource(Res.string.base_retry))
    }
}

@Preview(showBackground = true)
@Composable
private fun NoIdentityPreview() {
    SecureChatTheme {
        IdentityScreen(
            uiState = IdentityUiState.NoIdentity(phoneNumber = "+491701111111"),
            onUiEvent = {},
            scrollState = ScrollState(0),
            innerPadding = PaddingValues(0.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReadyIdentityPreview() {
    SecureChatTheme {
        IdentityScreen(
            uiState =
                IdentityUiState.Ready(
                    publicIdentity =
                        PublicIdentity(
                            encryptionPublicKey = byteArrayOf(1, 2, 3),
                            signingPublicKey = byteArrayOf(4, 5, 6)
                        ),
                    localPhoneNumber = "+491701111111"
                ),
            onUiEvent = {},
            scrollState = ScrollState(0),
            innerPadding = PaddingValues(0.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IncompleteIdentityPreview() {
    SecureChatTheme {
        IdentityScreen(
            uiState = IdentityUiState.IncompleteIdentity,
            onUiEvent = {},
            scrollState = ScrollState(0),
            innerPadding = PaddingValues(0.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingIdentityPreview() {
    SecureChatTheme {
        IdentityScreen(
            uiState = IdentityUiState.Error("gdfgdgdg"),
            onUiEvent = {},
            scrollState = ScrollState(0),
            innerPadding = PaddingValues(0.dp)
        )
    }
}
