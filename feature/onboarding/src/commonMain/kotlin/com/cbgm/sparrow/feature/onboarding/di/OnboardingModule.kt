package com.cbgm.sparrow.feature.onboarding.di

import com.cbgm.sparrow.feature.onboarding.presentation.OnboardingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val onboardingModule =
    module {
        viewModel { OnboardingViewModel() }
    }
