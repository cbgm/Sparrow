package com.cbgm.sparrow.feature.contactimport.presentation.scan

import androidx.compose.runtime.Composable
import com.cbgm.sparrow.feature.contactimport.presentation.verify.ScanIdentityRoute
import org.koin.compose.viewmodel.koinNavViewModel

@Composable
fun ScanIdentityNavigationRoute(
    viewModel: ScanIdentityNavigationViewModel = koinNavViewModel()
) {
    ScanIdentityRoute(
        onUiEvent = viewModel::onUiEvent
    )
}
