package com.cbgm.sparrow.feature.contacts.presentation.details

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
import com.cbgm.sparrow.feature.contacts.presentation.details.model.ContactDetailsContactUi
import com.cbgm.sparrow.feature.contacts.presentation.details.model.ContactDetailsUiEvent
import com.cbgm.sparrow.feature.contacts.presentation.details.model.ContactDetailsUiState
import com.cbgm.sparrow.feature.identity.presentation.verification.IdentityVerificationScreen
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_contact
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private enum class ContactDetailsContent {
    Overview,
    VerifyIdentity
}

private fun ContactDetailsContent.afterVerifyClicked(canVerify: Boolean): ContactDetailsContent =
    if (canVerify) ContactDetailsContent.VerifyIdentity else this

@Composable
fun ContactDetailsRoute(
    contactId: String,
    openVerification: Boolean,
    qrScanner: @Composable (enabled: Boolean) -> Unit,
    onShareContact: (ContactDetailsContactUi) -> Unit,
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactDetailsViewModel = koinViewModel()
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
            ?.sparrowIdentity
            ?.verifiedByMe == true
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
                            ContactDetailsUiEvent.ReconnectConfirmed -> onReconnect()
                            ContactDetailsUiEvent.VerifyIdentityClicked ->
                                content = content.afterVerifyClicked(canVerify = contentState?.canVerify == true)
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
                        safetyNumber = safetyNumber,
                        isLoadingSafetyNumber = false,
                        isVerifying = contentState.isSavingVerification,
                        errorMessage = contentState.verificationError,
                        onConfirm = {
                            viewModel.onUiEvent(ContactDetailsUiEvent.ConfirmVerificationClicked)
                        },
                        scanner = qrScanner,
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
