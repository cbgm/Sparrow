package com.cbgm.securechat.navigation.di

import com.cbgm.securechat.presentation.screen.MainViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val navigationModule =
    module {
        viewModel {
            MainViewModel(
                observePendingContactInvitationCount = get()
            )
        }
    }
