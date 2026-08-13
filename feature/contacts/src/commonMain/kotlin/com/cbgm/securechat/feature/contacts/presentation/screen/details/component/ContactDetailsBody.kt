package com.cbgm.securechat.feature.contacts.presentation.screen.details.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.contacts.presentation.component.contactdetails.ContactDetailsContent
import com.cbgm.securechat.feature.contacts.presentation.component.contactdetails.ContactDetailsErrorContent
import com.cbgm.securechat.feature.contacts.presentation.component.contactdetails.ContactDetailsLoadingContent
import com.cbgm.securechat.feature.contacts.presentation.component.contactdetails.ContactDetailsNotFoundContent
import com.cbgm.securechat.feature.contacts.presentation.component.contactdetails.ContactDetailsPreviewData
import com.cbgm.securechat.feature.contacts.presentation.model.ContactDetailsUiState

@Composable
internal fun ContactDetailsBody(
    uiState: ContactDetailsUiState,
    innerPadding: PaddingValues,
    scrollState: ScrollState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onShareContact: () -> Unit,
    onVerifyIdentity: () -> Unit
) {
    when (uiState) {
        ContactDetailsUiState.Loading ->
            ContactDetailsLoadingContent(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
            )

        ContactDetailsUiState.NotFound ->
            ContactDetailsNotFoundContent(
                onBack = onBack,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(MaterialTheme.spacing.medium)
            )

        is ContactDetailsUiState.Content ->
            ContactDetailsContent(
                contact = uiState.contact,
                safetyNumber = uiState.safetyNumber,
                onShareContact = onShareContact,
                onVerifyIdentity = onVerifyIdentity,
                scrollState = scrollState,
                innerPadding = innerPadding,
                modifier = Modifier.fillMaxSize()
            )

        is ContactDetailsUiState.Error ->
            ContactDetailsErrorContent(
                message = uiState.message,
                onRetry = onRetry,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp)
            )
    }
}

@Preview
@Composable
private fun ContactDetailsBodyPreview() {
    SecureChatTheme {
        ContactDetailsBody(
            uiState = ContactDetailsUiState.Content(ContactDetailsPreviewData.contact, ContactDetailsPreviewData.safetyNumber),
            innerPadding = PaddingValues(),
            scrollState = rememberScrollState(),
            onBack = {},
            onRetry = {},
            onShareContact = {},
            onVerifyIdentity = {}
        )
    }
}
