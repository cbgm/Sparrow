package com.cbgm.sparrow.feature.contacts.presentation.details

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowScrollScaffold
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.feature.contacts.presentation.details.components.ContactDetailPage
import com.cbgm.sparrow.feature.contacts.presentation.details.components.ContactDetailsBody
import com.cbgm.sparrow.feature.contacts.presentation.details.components.ContactDetailsPreviewData
import com.cbgm.sparrow.feature.contacts.presentation.details.components.ContactDetailsTopBar
import com.cbgm.sparrow.feature.contacts.presentation.details.model.ContactDetailsUiEvent
import com.cbgm.sparrow.feature.contacts.presentation.details.model.ContactDetailsUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_contact
import com.cbgm.sparrow.resources.base_phone_numbers
import com.cbgm.sparrow.resources.feature_contacts_contact_details
import com.cbgm.sparrow.resources.feature_contacts_sparrow_identity
import org.jetbrains.compose.resources.stringResource

@Composable
fun ContactDetailsScreen(
    uiState: ContactDetailsUiState,
    onUiEvent: (ContactDetailsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val contactId = (uiState as? ContactDetailsUiState.Content)?.contact?.id
    var page by rememberSaveable(contactId) { mutableStateOf(ContactDetailPage.Overview) }
    val title =
        when (uiState) {
            is ContactDetailsUiState.Content -> when (page) {
                ContactDetailPage.Overview -> uiState.contact.displayName ?: stringResource(Res.string.base_contact)
                ContactDetailPage.Phones -> stringResource(Res.string.base_phone_numbers)
                ContactDetailPage.Identity -> stringResource(Res.string.feature_contacts_sparrow_identity)
            }

            else ->
                stringResource(Res.string.feature_contacts_contact_details)
        }

    SparrowScrollScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            ContactDetailsTopBar(
                title = title,
                containerColor = containerColor,
                onBack = {
                    if (page == ContactDetailPage.Overview) {
                        onUiEvent(ContactDetailsUiEvent.BackClicked)
                    } else {
                        page = ContactDetailPage.Overview
                    }
                }
            )
        }
    ) { innerPadding, scrollState ->
        LaunchedEffect(page) { scrollState.scrollTo(0) }
        ContactDetailsBody(
            uiState = uiState,
            innerPadding = innerPadding,
            scrollState = scrollState,
            onBack = { onUiEvent(ContactDetailsUiEvent.BackClicked) },
            onRetry = { onUiEvent(ContactDetailsUiEvent.RetryClicked) },
            onShareContact = { onUiEvent(ContactDetailsUiEvent.ShareContactClicked) },
            onVerifyIdentity = { onUiEvent(ContactDetailsUiEvent.VerifyIdentityClicked) },
            onMediaAndFiles = { onUiEvent(ContactDetailsUiEvent.MediaAndFilesClicked) },
            page = page,
            onPageSelected = { page = it }
        )
    }
}

@Preview
@Composable
private fun PreviewContactDetailsScreen() {
    SparrowTheme {
        ContactDetailsScreen(
            uiState = ContactDetailsUiState.Content(
                contact = ContactDetailsPreviewData.contact,
                safetyNumber = ContactDetailsPreviewData.safetyNumber
            ),
            onUiEvent = {}
        )
    }
}
