package com.cbgm.sparrow.feature.settings.presentation.developer.nodes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DeveloperNodesRoute(
    modifier: Modifier = Modifier,
    viewModel: DeveloperNodesViewModel = koinViewModel()
) {
    val diagnostics by viewModel.diagnostics.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refreshTransportDiagnosticsWhileVisible() }

    DeveloperNodesScreen(
        diagnostics = diagnostics,
        onBack = viewModel::onBackClicked,
        modifier = modifier
    )
}
