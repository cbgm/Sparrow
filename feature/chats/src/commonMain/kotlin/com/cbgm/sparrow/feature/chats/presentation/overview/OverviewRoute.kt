package com.cbgm.sparrow.feature.chats.presentation.overview

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OverviewRoute(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    innerPadding: PaddingValues,
    viewModel: OverviewViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OverviewScreen(
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent,
        listState = listState,
        innerPadding = innerPadding,
        modifier = modifier
    )
}
