package com.cbgm.sparrow.navigation.di

import com.cbgm.sparrow.navigation.presentation.main.MainViewModel
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
