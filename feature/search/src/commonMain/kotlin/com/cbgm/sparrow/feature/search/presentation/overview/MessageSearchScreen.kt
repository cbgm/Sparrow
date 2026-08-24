package com.cbgm.sparrow.feature.search.presentation.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.component.SparrowSearchField
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.search.domain.model.MessageSearchConversationType
import com.cbgm.sparrow.feature.search.presentation.model.MessageSearchMode
import com.cbgm.sparrow.feature.search.presentation.model.MessageSearchResultUi
import com.cbgm.sparrow.feature.search.presentation.model.MessageSearchUiEvent
import com.cbgm.sparrow.feature.search.presentation.model.MessageSearchUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_back
import com.cbgm.sparrow.resources.base_unknown
import com.cbgm.sparrow.resources.feature_search_exact_only_hint
import com.cbgm.sparrow.resources.feature_search_failed
import com.cbgm.sparrow.resources.feature_search_no_results
import com.cbgm.sparrow.resources.feature_search_placeholder
import com.cbgm.sparrow.resources.feature_search_preparing_hint
import com.cbgm.sparrow.resources.feature_search_semantic_unavailable_hint
import com.cbgm.sparrow.resources.feature_search_start_hint
import com.cbgm.sparrow.resources.feature_search_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageSearchScreen(
    uiState: MessageSearchUiState,
    onUiEvent: (MessageSearchUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SparrowLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            TopBar(
                searchQuery = uiState.query,
                containerColor = containerColor,
                onSearchQueryChanged = { query ->
                    onUiEvent(MessageSearchUiEvent.QueryChanged(query))
                },
                mode = uiState.mode,
                isSearching = uiState.isSearching,
                onBack = { onUiEvent(MessageSearchUiEvent.BackClicked) },
                onClearClicked = { onUiEvent(MessageSearchUiEvent.ClearQueryClicked) }
            )
        }
    ) { innerPadding, listState ->
        SearchList(
            uiState = uiState,
            onUiEvent = onUiEvent,
            innerPadding = innerPadding,
            listState = listState
        )
    }
}

@Composable
private fun SearchList(
    uiState: MessageSearchUiState,
    onUiEvent: (MessageSearchUiEvent) -> Unit,
    innerPadding: PaddingValues,
    listState: LazyListState
) {
    LazyColumn(
        modifier = Modifier
            .padding(vertical = MaterialTheme.spacing.screenPadding)
            .fillMaxSize(),
        state = listState,
        contentPadding = innerPadding
    ) {
        when {
            uiState.query.isBlank() ->
                item(key = "start-hint") {
                    SearchMessage(
                        text = stringResource(Res.string.feature_search_start_hint)
                    )
                }

            uiState.searchFailed ->
                item(key = "search-failed") {
                    SearchMessage(
                        text = stringResource(Res.string.feature_search_failed)
                    )
                }

            !uiState.isSearching && uiState.results.isEmpty() ->
                item(key = "no-results") {
                    SearchMessage(
                        text = stringResource(Res.string.feature_search_no_results)
                    )
                }

            else ->
                items(
                    items = uiState.results,
                    key = MessageSearchResultUi::messageId
                ) { result ->
                    SearchResultItem(
                        result = result,
                        onClick = { onUiEvent(MessageSearchUiEvent.ResultClicked(result.messageId)) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Alpha.itemDivider)
                    )
                }
        }
    }
}

@Composable
private fun TopBar(
    searchQuery: String,
    containerColor: Color,
    mode: MessageSearchMode,
    isSearching: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onClearClicked: () -> Unit,
    onBack: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(Res.string.feature_search_title),
                    style = MaterialTheme.typography.titleSmall
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.base_back)
                    )
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    scrolledContainerColor = containerColor,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
        )

        Column(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.screenPadding)
        ) {
            SparrowSearchField(
                searchQuery = searchQuery,
                focusRequester = focusRequester,
                onSearchQueryChanged = onSearchQueryChanged,
                placeholder = stringResource(Res.string.feature_search_placeholder),
                onClear = onClearClicked
            )

            SearchModeHint(mode = mode)

            if (isSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SearchModeHint(mode: MessageSearchMode) {
    val text =
        when (mode) {
            MessageSearchMode.HYBRID -> null
            MessageSearchMode.EXACT_ONLY -> stringResource(Res.string.feature_search_exact_only_hint)
            MessageSearchMode.PREPARING_SEMANTIC -> stringResource(Res.string.feature_search_preparing_hint)
            MessageSearchMode.SEMANTIC_UNAVAILABLE -> stringResource(Res.string.feature_search_semantic_unavailable_hint)
        }

    text?.let {
        Text(
            text = it,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.screenPadding)
                    .padding(bottom = MaterialTheme.spacing.small),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SearchResultItem(
    result: MessageSearchResultUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = MaterialTheme.spacing.screenPadding)
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = MaterialTheme.spacing.base),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = MaterialTheme.spacing.base)
        ) {
            Text(
                text = result.conversationName ?: stringResource(Res.string.base_unknown),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.micro))
            Text(
                text = result.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = result.timestamp,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Top)
        )
    }
}

@Composable
private fun SearchMessage(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview
@Composable
private fun MessageSearchScreenPreview() {
    SparrowTheme {
        MessageSearchScreen(
            uiState =
                MessageSearchUiState(
                    query = "server address",
                    results =
                        listOf(
                            MessageSearchResultUi(
                                messageId = "message-1",
                                conversationId = "conversation-1",
                                conversationType = MessageSearchConversationType.GROUP,
                                contactId = null,
                                conversationName = "Development",
                                text = "The new server is running on 192.168.178.60.",
                                timestamp = "20.08.26 01:30"
                            ),
                            MessageSearchResultUi(
                                messageId = "message-2",
                                conversationId = "conversation-2",
                                conversationType = MessageSearchConversationType.DIRECT,
                                contactId = "contact-2",
                                conversationName = "Peter",
                                text = "Use the laptop address instead of the old machine.",
                                timestamp = "19.08.26 22:14"
                            )
                        ),
                    mode = MessageSearchMode.HYBRID
                ),
            onUiEvent = {}
        )
    }
}
