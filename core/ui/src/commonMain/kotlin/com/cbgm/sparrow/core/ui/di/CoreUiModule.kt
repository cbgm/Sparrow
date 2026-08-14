package com.cbgm.sparrow.core.ui.di

import com.cbgm.sparrow.core.ui.navigation.AppNavigator
import org.koin.dsl.module

val coreUiModule =
    module {
        single { AppNavigator() }
    }
