package com.cbgm.sparrow.feature.onboarding.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowAnimation
import com.cbgm.sparrow.core.ui.component.SparrowCard
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityUiState
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingPage
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingUiEvent
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingUiState
import com.cbgm.sparrow.feature.onboarding.presentation.pages.PermissionsPage
import com.cbgm.sparrow.feature.onboarding.presentation.pages.PhonePage
import com.cbgm.sparrow.feature.onboarding.presentation.pages.PrivacyPage
import com.cbgm.sparrow.feature.onboarding.presentation.pages.WelcomePage
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_app_name
import com.cbgm.sparrow.resources.base_tagline
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    identityState: IdentityUiState,
    onUiEvent: (OnboardingUiEvent) -> Unit
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(MaterialTheme.spacing.screenPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SparrowAnimation(modifier = Modifier.size(Dimens.OnboardingScreen.animationSize), true)
            Spacer(Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = stringResource(Res.string.base_app_name),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(Res.string.base_tagline),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(MaterialTheme.spacing.medium))

            SparrowCard {
                AnimatedContent(
                    targetState = state.page,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "onboardingPage"
                ) { page ->

                    when (page) {
                        OnboardingPage.WELCOME -> WelcomePage { onUiEvent(OnboardingUiEvent.NextClicked) }
                        OnboardingPage.PRIVACY -> PrivacyPage { onUiEvent(OnboardingUiEvent.NextClicked) }
                        OnboardingPage.PERMISSIONS ->
                            PermissionsPage {
                                onUiEvent(OnboardingUiEvent.RequestPermissionsClicked)
                            }

                        OnboardingPage.PHONE ->
                            PhonePage(
                                identityState = identityState,
                                isCreating = state.isCreatingIdentity,
                                canRetryAutomatic = state.phonePermissionGranted,
                                onChooseAnotherNumber = { onUiEvent(OnboardingUiEvent.ChooseAnotherNumberClicked) },
                                onRetryAutomaticNumber = { onUiEvent(OnboardingUiEvent.RetryAutomaticNumberClicked) },
                                onPhoneNumberChanged = { value ->
                                    onUiEvent(OnboardingUiEvent.PhoneNumberChanged(value))
                                },
                                onApproveAndCreate = { onUiEvent(OnboardingUiEvent.ApproveAndCreateClicked) },
                                onNameChanged = { value ->
                                    onUiEvent(OnboardingUiEvent.NameChanged(value))
                                }
                            )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun OnboardingScreenPreview() {
    SparrowTheme {
        OnboardingScreen(
            state = OnboardingUiState(page = OnboardingPage.PERMISSIONS),
            identityState =
                IdentityUiState.Ready(
                    localPhoneNumber = "445446",
                    publicIdentity =
                        PublicIdentity(
                            ByteArray(size = 0),
                            ByteArray(size = 0)
                        )
                ),
            onUiEvent = {}
        )
    }
}

@Preview
@Composable
private fun OnboardingScreen2Preview() {
    SparrowTheme {
        OnboardingScreen(
            state = OnboardingUiState(page = OnboardingPage.WELCOME),
            identityState = IdentityUiState.NoIdentity(),
            onUiEvent = {}
        )
    }
}
