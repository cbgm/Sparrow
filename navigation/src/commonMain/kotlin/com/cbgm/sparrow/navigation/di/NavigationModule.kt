package com.cbgm.sparrow.navigation.di

import com.cbgm.sparrow.presentation.screen.MainViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val navigationModule =
    module {
        viewModel {
            MainViewModel(
                observePendingContactInvitationCount = get(),
                observeSemanticSearchState = get()
            )
        }
    }
