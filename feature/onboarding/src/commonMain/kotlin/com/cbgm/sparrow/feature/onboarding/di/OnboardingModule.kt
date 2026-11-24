package com.cbgm.sparrow.feature.onboarding.di

import com.cbgm.sparrow.feature.onboarding.presentation.OnboardingViewModel
import com.cbgm.sparrow.feature.onboarding.presentation.localnetwork.LocalNetworkAccessViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val onboardingModule = module {
    viewModel {
        OnboardingViewModel(
            setConnectionMode = get(),
            canConnectWhileClosed = get(),
            getAppThemeMode = get(),
            setAppThemeMode = get()
        )
    }
    viewModel { LocalNetworkAccessViewModel(observeLocalNetworkNeeded = get()) }
}
