package com.cbgm.securechat.feature.onboarding.di

import com.cbgm.securechat.feature.onboarding.presentation.OnboardingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val onboardingModule =
    module {
        viewModel { OnboardingViewModel() }
    }
