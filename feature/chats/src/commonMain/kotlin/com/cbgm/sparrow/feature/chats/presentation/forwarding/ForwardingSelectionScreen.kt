package com.cbgm.sparrow.feature.chats.presentation.forwarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.component.SparrowSearchField
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.chats.presentation.forwarding.component.forwardingTargetSection
import com.cbgm.sparrow.feature.chats.presentation.forwarding.model.ForwardingSelectionUiEvent
import com.cbgm.sparrow.feature.chats.presentation.forwarding.model.ForwardingSelectionUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_forward_chats
import com.cbgm.sparrow.resources.feature_chats_forward_contacts
import com.cbgm.sparrow.resources.feature_chats_forward_no_targets
import com.cbgm.sparrow.resources.feature_chats_forward_search_placeholder
import com.cbgm.sparrow.resources.feature_chats_forward_to
import org.jetbrains.compose.resources.stringResource

@Composable
fun ForwardingSelectionScreen(
    uiState: ForwardingSelectionUiState,
    onUiEvent: (ForwardingSelectionUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SparrowLazyScaffold(
        modifier = modifier.fillMaxSize(),
        barColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            TopBar(
                searchQuery = uiState.searchQuery,
                containerColor = containerColor,
                onSearchQueryChanged = { query ->
                    onUiEvent(ForwardingSelectionUiEvent.SearchQueryChanged(query))
                },
                onBack = onBack
            )
        }
    ) { innerPadding, listState ->
        Content(
            uiState = uiState,
            listState = listState,
            innerPadding = innerPadding,
            onUiEvent = onUiEvent
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    searchQuery: String,
    containerColor: Color,
    onSearchQueryChanged: (String) -> Unit,
    onBack: () -> Unit
) {
    Column {
        CenterAlignedTopAppBar(
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    scrolledContainerColor = containerColor
                ),
            title = {
                Text(
                    text = stringResource(Res.string.feature_chats_forward_to),
                    style = MaterialTheme.typography.titleSmall
                )
            },
            actions = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null
                    )
                }
            }
        )

        SparrowSearchField(
            searchQuery = searchQuery,
            onSearchQueryChanged = onSearchQueryChanged,
            placeholder = stringResource(Res.string.feature_chats_forward_search_placeholder),
            onClear = { onSearchQueryChanged("") },
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.screenPadding)
        )
    }
}

@Composable
private fun Content(
    uiState: ForwardingSelectionUiState,
    listState: LazyListState,
    innerPadding: PaddingValues,
    onUiEvent: (ForwardingSelectionUiEvent) -> Unit
) {
    when (uiState) {
        is ForwardingSelectionUiState.Loading ->
            LoadingContent(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
            )

        is ForwardingSelectionUiState.Empty ->
            EmptyContent(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
            )

        is ForwardingSelectionUiState.Content -> {
            val forwardStrChats = stringResource(Res.string.feature_chats_forward_chats)
            val forwardStrContacts = stringResource(Res.string.feature_chats_forward_contacts)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = innerPadding
            ) {
                forwardingTargetSection(
                    title = forwardStrChats,
                    targets = uiState.chats,
                    onUiEvent = onUiEvent
                )
                forwardingTargetSection(
                    title = forwardStrContacts,
                    targets = uiState.contacts,
                    onUiEvent = onUiEvent
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(Res.string.feature_chats_forward_no_targets),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
