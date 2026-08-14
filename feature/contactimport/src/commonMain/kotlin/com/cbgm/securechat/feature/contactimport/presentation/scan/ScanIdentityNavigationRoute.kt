package com.cbgm.securechat.feature.contactimport.presentation.scan

import androidx.compose.runtime.Composable
import com.cbgm.securechat.core.ui.navigation.AppRoute
import com.cbgm.securechat.feature.contactimport.presentation.verify.ScanIdentityRoute
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ScanIdentityNavigationRoute(
    route: AppRoute.ScanIdentity,
    viewModel: ScanIdentityNavigationViewModel =
        koinViewModel(
            parameters = {
                parametersOf(route)
            }
        )
) {
    ScanIdentityRoute(
        onUiEvent = viewModel::onUiEvent
    )
}
