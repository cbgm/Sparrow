package com.cbgm.securechat.feature.contacts.presentation.screen.details

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.crypto.safety.SafetyNumber
import com.cbgm.securechat.core.ui.component.PatternBackground
import com.cbgm.securechat.core.ui.component.SecureChatScrollScaffold
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumber
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.model.SecureChatIdentity
import com.cbgm.securechat.feature.contacts.presentation.model.ContactDetailsUiEvent
import com.cbgm.securechat.feature.contacts.presentation.model.ContactDetailsUiState
import com.cbgm.securechat.feature.contacts.presentation.screen.details.component.ContactDetailsBody
import com.cbgm.securechat.feature.contacts.presentation.screen.details.component.ContactDetailsTopBar
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_contact
import com.cbgm.securechat.resources.feature_contacts_contact_details
import org.jetbrains.compose.resources.stringResource

@Composable
fun ContactDetailsScreen(
    uiState: ContactDetailsUiState,
    onUiEvent: (ContactDetailsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val title =
        when (uiState) {
            is ContactDetailsUiState.Content ->
                uiState.contact.displayName ?: stringResource(Res.string.base_contact)

            else ->
                stringResource(Res.string.feature_contacts_contact_details)
        }

    SecureChatScrollScaffold(
        modifier = modifier,
        background = {
            PatternBackground(
                modifier = Modifier.fillMaxSize(),
                backgroundColor = MaterialTheme.colorScheme.background,
                alpha = 0.04f
            )
        },
        topBar = { containerColor ->
            ContactDetailsTopBar(
                title = title,
                containerColor = containerColor,
                onBack = { onUiEvent(ContactDetailsUiEvent.BackClicked) }
            )
        }
    ) { innerPadding, scrollState ->
        ContactDetailsBody(
            uiState = uiState,
            innerPadding = innerPadding,
            scrollState = scrollState,
            onBack = { onUiEvent(ContactDetailsUiEvent.BackClicked) },
            onRetry = { onUiEvent(ContactDetailsUiEvent.RetryClicked) },
            onShareContact = { onUiEvent(ContactDetailsUiEvent.ShareContactClicked) },
            onVerifyIdentity = { onUiEvent(ContactDetailsUiEvent.VerifyIdentityClicked) }
        )
    }
}

@Preview
@Composable
private fun PreviewContactDetailsScreen() {
    SecureChatTheme {
        ContactDetailsScreen(
            uiState =
                ContactDetailsUiState.Content(
                    contact =
                        Contact(
                            id = "1",
                            displayName = "Alex",
                            phoneNumbers =
                                listOf(
                                    ContactPhoneNumber(
                                        id = "1",
                                        value = "1234567890",
                                        type = ContactPhoneNumberType.MOBILE,
                                        label = "Mobile"
                                    )
                                ),
                            preferredPhoneNumberId = "1",
                            deviceContactLinkStatus = DeviceContactLinkStatus.LINKED,
                            secureChatIdentity =
                                SecureChatIdentity(
                                    signingPublicKey = byteArrayOf(1, 2, 3),
                                    encryptionPublicKey = byteArrayOf(4, 5, 6),
                                    verificationStatus = ContactVerificationStatus.UNVERIFIED,
                                    updatedAtEpochMilliseconds = System.currentTimeMillis(),
                                    keyExchangeStatus = KeyExchangeStatus.ONE_WAY
                                ),
                            createdAtEpochMilliseconds = System.currentTimeMillis(),
                            updatedAtEpochMilliseconds = System.currentTimeMillis(),
                            deviceContactId = "1"
                        ),
                    safetyNumber =
                        SafetyNumber(
                            groups =
                                listOf(
                                    "11111",
                                    "11111",
                                    "11111",
                                    "11111",
                                    "11111",
                                    "11111",
                                    "11111",
                                    "11111",
                                    "11111",
                                    "11111",
                                    "11111",
                                    "11111",
                                    "11111",
                                    "11111",
                                    "11111",
                                    "11111"
                                )
                        ),
                    isSavingVerification = false,
                    verificationError = null
                ),
            onUiEvent = {}
        )
    }
}
