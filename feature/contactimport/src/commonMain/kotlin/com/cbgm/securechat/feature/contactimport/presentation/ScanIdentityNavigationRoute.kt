package com.cbgm.securechat.feature.contactimport.presentation

import androidx.compose.runtime.Composable
import com.cbgm.securechat.feature.contactimport.presentation.screen.ScanIdentityNavigationViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ScanIdentityNavigationRoute(
    viewModel: ScanIdentityNavigationViewModel = koinViewModel()
) {
    ScanIdentityRoute(
        onUiEvent = viewModel::onUiEvent
    )
}
