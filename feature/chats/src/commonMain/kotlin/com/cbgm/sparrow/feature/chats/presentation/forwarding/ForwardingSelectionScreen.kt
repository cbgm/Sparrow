package com.cbgm.sparrow.feature.chats.presentation.forwarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.component.SparrowSearchField
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.chats.domain.model.ForwardingTarget
import com.cbgm.sparrow.feature.chats.presentation.forwarding.component.forwardingTargetSection
import com.cbgm.sparrow.feature.chats.presentation.forwarding.model.ForwardingSelectionUiEvent
import com.cbgm.sparrow.feature.chats.presentation.forwarding.model.ForwardingSelectionUiState
import com.cbgm.sparrow.feature.chats.presentation.forwarding.model.ForwardingTargetUi
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
            windowInsets = WindowInsets(MaterialTheme.spacing.zero),
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    scrolledContainerColor = containerColor
                ),
            title = {
                Text(
                    text = stringResource(Res.string.feature_chats_forward_to),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
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
            modifier = Modifier.padding(
                start = MaterialTheme.spacing.screenPadding,
                end = MaterialTheme.spacing.screenPadding,
                bottom = MaterialTheme.spacing.small
            )
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
                contentPadding =
                    PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding()
                    ),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
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
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
        Surface(
            color = MaterialTheme.colorScheme.background,
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(Res.string.feature_chats_forward_no_targets),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Preview
@Composable
private fun ForwardSelectionScreenPreview() {
    SparrowTheme {
        ForwardingSelectionScreen(
            uiState = ForwardingSelectionUiState.Content(
                searchQuery = "",
                chats = listOf(
                    ForwardingTargetUi(
                        id = "2",
                        displayName = "Test",
                        avatarTarget = AvatarTarget.User(
                            id = "113"
                        ),
                        target = ForwardingTarget.Contact(
                            contactId = "1"
                        )
                    )
                ),
                contacts = emptyList()
            ),
            onUiEvent = {},
            onBack = {}
        )
    }
}
