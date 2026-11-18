package com.cbgm.sparrow.feature.onboarding.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.feature.identity.presentation.create.model.IdentityCreateUiEvent
import com.cbgm.sparrow.feature.identity.presentation.create.model.IdentityCreateUiState
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingPage
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingUiEvent
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingUiState
import com.cbgm.sparrow.feature.onboarding.presentation.pages.OnionPage
import com.cbgm.sparrow.feature.onboarding.presentation.pages.PermissionsPage
import com.cbgm.sparrow.feature.onboarding.presentation.pages.PhonePage
import com.cbgm.sparrow.feature.onboarding.presentation.pages.ReceivingPage
import com.cbgm.sparrow.feature.onboarding.presentation.pages.WelcomePage
import com.cbgm.sparrow.feature.onboarding.presentation.pages.component.PageNumber

/**
 * The onboarding pages side by side; they can always be swiped. The pager reports where it settled, and follows when
 * the ViewModel moves on by itself (after the permission dialogs).
 */
@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    identityState: IdentityCreateUiState,
    onRestoreIdentity: () -> Unit = {},
    onIdentityUiEvent: (IdentityCreateUiEvent) -> Unit,
    onUiEvent: (OnboardingUiEvent) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = state.page.ordinal) { OnboardingPage.entries.size }
    val currentOnUiEvent = rememberUpdatedState(onUiEvent)
    LaunchedEffect(state.page) {
        if (pagerState.currentPage != state.page.ordinal) pagerState.animateScrollToPage(state.page.ordinal)
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            currentOnUiEvent.value(OnboardingUiEvent.PageShown(OnboardingPage.entries[page]))
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
            val active = pagerState.currentPage == index
            when (OnboardingPage.entries[index]) {
                OnboardingPage.WELCOME -> WelcomePage(active = active)
                OnboardingPage.ONION -> OnionPage(active = active)
                OnboardingPage.PERMISSIONS ->
                    PermissionsPage(active = active, onAllow = { onUiEvent(OnboardingUiEvent.RequestPermissionsClicked) })

                OnboardingPage.RECEIVING ->
                    ReceivingPage(
                        state = state,
                        active = active,
                        onChoose = { mode -> onUiEvent(OnboardingUiEvent.ConnectionModeChosen(mode)) }
                    )

                OnboardingPage.PHONE ->
                    PhonePage(
                        identityState = identityState,
                        active = active,
                        isCreating = identityState == IdentityCreateUiState.Creating,
                        canRetryAutomatic = state.phonePermissionGranted,
                        onChooseAnotherNumber = { onUiEvent(OnboardingUiEvent.ChooseAnotherNumberClicked) },
                        onRetryAutomaticNumber = { onUiEvent(OnboardingUiEvent.RetryAutomaticNumberClicked) },
                        onPhoneNumberChanged = { value -> onIdentityUiEvent(IdentityCreateUiEvent.PhoneNumberChanged(value)) },
                        onApproveAndCreate = { onIdentityUiEvent(IdentityCreateUiEvent.CreateClicked) },
                        onRestoreIdentity = onRestoreIdentity,
                        onNameChanged = { value -> onIdentityUiEvent(IdentityCreateUiEvent.NameChanged(value)) }
                    )
            }
        }
        PageNumber(
            page = pagerState.currentPage,
            pageCount = OnboardingPage.entries.size,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

@Preview
@Composable
private fun OnboardingScreenPreview() {
    SparrowTheme {
        OnboardingScreen(
            state = OnboardingUiState(page = OnboardingPage.ONION),
            identityState = IdentityCreateUiState.Form(),
            onIdentityUiEvent = {},
            onUiEvent = {}
        )
    }
}
