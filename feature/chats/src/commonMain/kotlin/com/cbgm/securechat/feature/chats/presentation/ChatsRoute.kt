package com.cbgm.securechat.feature.chats.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.chats.presentation.screen.ChatsScreen
import com.cbgm.securechat.feature.chats.presentation.screen.ChatsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatsRoute(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    innerPadding: PaddingValues,
    viewModel: ChatsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ChatsScreen(
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent,
        listState = listState,
        innerPadding = innerPadding,
        modifier = modifier
    )
}
