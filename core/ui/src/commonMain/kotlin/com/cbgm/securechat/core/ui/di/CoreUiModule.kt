package com.cbgm.securechat.core.ui.di

import com.cbgm.securechat.core.ui.navigation.AppNavigator
import org.koin.dsl.module

val coreUiModule =
    module {
        single { AppNavigator() }
    }
