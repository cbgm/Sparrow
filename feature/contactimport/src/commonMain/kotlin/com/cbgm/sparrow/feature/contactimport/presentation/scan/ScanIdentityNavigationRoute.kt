package com.cbgm.sparrow.feature.contactimport.presentation.scan

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ScanIdentityNavigationRoute(
    viewModel: ScanIdentityNavigationViewModel = koinViewModel()
) {
    ScanIdentityRoute(
        onUiEvent = viewModel::onUiEvent
    )
}
