package com.cbgm.sparrow.feature.search.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import com.cbgm.sparrow.feature.search.presentation.model.MessageSearchResultUiModel
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
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    SparrowLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.feature_search_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onUiEvent(MessageSearchUiEvent.BackClicked) }) {
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
        }
    ) { innerPadding, listState ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = innerPadding
        ) {
            item(key = "search-field") {
                SparrowSearchField(
                    searchQuery = uiState.query,
                    focusRequester = focusRequester,
                    onSearchQueryChanged = { query ->
                        onUiEvent(MessageSearchUiEvent.QueryChanged(query))
                    },
                    placeholder = stringResource(Res.string.feature_search_placeholder),
                    onClear = { onUiEvent(MessageSearchUiEvent.ClearQueryClicked) }
                )

                SearchModeHint(mode = uiState.mode)

                if (uiState.isSearching) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

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
                        key = MessageSearchResultUiModel::messageId
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
    result: MessageSearchResultUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = result.conversationName ?: stringResource(Res.string.base_unknown),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        supportingContent = {
            Text(
                text = result.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Text(
                text = result.timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
    )
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
                            MessageSearchResultUiModel(
                                messageId = "message-1",
                                conversationId = "conversation-1",
                                conversationType = MessageSearchConversationType.GROUP,
                                contactId = null,
                                conversationName = "Development",
                                text = "The new server is running on 192.168.178.60.",
                                timestamp = "20.08.26 01:30"
                            ),
                            MessageSearchResultUiModel(
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
