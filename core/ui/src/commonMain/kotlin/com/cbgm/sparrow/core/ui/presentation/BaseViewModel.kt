package com.cbgm.sparrow.core.ui.presentation

import androidx.lifecycle.ViewModel
import com.cbgm.sparrow.core.ui.navigation.AppNavigator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class BaseViewModel :
    ViewModel(),
    KoinComponent {
    protected val navigator: AppNavigator by inject()
}
