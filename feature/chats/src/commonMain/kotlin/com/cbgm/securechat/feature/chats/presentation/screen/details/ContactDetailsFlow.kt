package com.cbgm.securechat.feature.contacts.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.core.ui.component.IdentityVerificationScreen
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.presentation.model.ContactDetailsUiEvent
import com.cbgm.securechat.feature.contacts.presentation.model.ContactDetailsUiState
import com.cbgm.securechat.feature.contacts.presentation.screen.details.ContactDetailsScreen
import com.cbgm.securechat.feature.contacts.presentation.screen.details.ContactDetailsViewModel
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_contact
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private enum class ContactDetailsContent {
    Overview,
    VerifyIdentity
}

@Composable
fun ContactDetailsFlow(
    contactId: String,
    openVerification: Boolean,
    verificationRevision: Int,
    onShareContact: (Contact) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactDetailsViewModel =
        koinViewModel(
            parameters = {
                parametersOf(contactId)
            }
        )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val contentState = uiState as? ContactDetailsUiState.Content
    var content by
        rememberSaveable(contactId) {
            mutableStateOf(
                if (openVerification) {
                    ContactDetailsContent.VerifyIdentity
                } else {
                    ContactDetailsContent.Overview
                }
            )
        }

    val isAlreadyVerified =
        contentState
            ?.contact
            ?.secureChatIdentity
            ?.verificationStatus == ContactVerificationStatus.VERIFIED
    val visibleContent =
        if (
            content == ContactDetailsContent.VerifyIdentity &&
            (contentState?.canVerify != true || isAlreadyVerified)
        ) {
            ContactDetailsContent.Overview
        } else {
            content
        }

    LaunchedEffect(isAlreadyVerified) {
        if (isAlreadyVerified) {
            content = ContactDetailsContent.Overview
        }
    }

    LaunchedEffect(verificationRevision) {
        if (verificationRevision > 0) {
            viewModel.onUiEvent(ContactDetailsUiEvent.RefreshRequested)
        }
    }

    AnimatedContent(
        targetState = visibleContent,
        modifier = modifier,
        transitionSpec = {
            if (targetState == ContactDetailsContent.VerifyIdentity) {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) togetherWith
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
            } else {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) togetherWith
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
            }
        }
    ) { target ->
        when (target) {
            ContactDetailsContent.Overview -> {
                ContactDetailsScreen(
                    uiState = uiState,
                    onUiEvent = { event ->
                        when (event) {
                            ContactDetailsUiEvent.ShareContactClicked -> {
                                contentState?.contact?.let(onShareContact)
                            }
                            ContactDetailsUiEvent.VerifyIdentityClicked -> {
                                if (contentState?.canVerify == true) {
                                    content = ContactDetailsContent.VerifyIdentity
                                }
                            }
                            else -> viewModel.onUiEvent(event)
                        }
                    }
                )
            }

            ContactDetailsContent.VerifyIdentity -> {
                val contact = contentState?.contact
                val safetyNumber = contentState?.safetyNumber

                if (contact != null && safetyNumber != null) {
                    IdentityVerificationScreen(
                        contactName =
                            contact.displayName
                                ?: stringResource(Res.string.base_contact),
                        safetyNumber = safetyNumber.singleLine,
                        isLoadingSafetyNumber = false,
                        isVerifying = contentState.isSavingVerification,
                        errorMessage = contentState.verificationError,
                        onConfirm = {
                            viewModel.onUiEvent(ContactDetailsUiEvent.ConfirmVerificationClicked)
                        },
                        onScanQrCode = {
                            viewModel.onUiEvent(ContactDetailsUiEvent.ScanQrCodeClicked)
                        },
                        onBack = {
                            if (!contentState.isSavingVerification) {
                                viewModel.onUiEvent(ContactDetailsUiEvent.VerificationBackClicked)
                                content = ContactDetailsContent.Overview
                            }
                        }
                    )
                }
            }
        }
    }
}
