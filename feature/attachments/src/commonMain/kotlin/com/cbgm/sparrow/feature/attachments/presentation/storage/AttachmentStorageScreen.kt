package com.cbgm.sparrow.feature.attachments.presentation.storage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.attachments.presentation.storage.model.AttachmentStorageUiEvent
import com.cbgm.sparrow.feature.attachments.presentation.storage.model.AttachmentStorageUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_attachments_conversation_storage_description
import com.cbgm.sparrow.resources.feature_attachments_storage
import com.cbgm.sparrow.resources.feature_attachments_storage_empty
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentStorageScreen(
    uiState: AttachmentStorageUiState,
    onUiEvent: (AttachmentStorageUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SparrowLazyScaffold(
        modifier = modifier,
        topBar = { color ->
            TopBar(
                containerColor = color,
                onBack = { onUiEvent(AttachmentStorageUiEvent.BackClicked) }
            )
        }
    ) { innerPadding, listState ->
        when (uiState) {
            AttachmentStorageUiState.Loading ->
                Box(
                    Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

            is AttachmentStorageUiState.Error ->
                Box(
                    Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(uiState.message, color = MaterialTheme.colorScheme.error)
                }

            is AttachmentStorageUiState.Content -> {
                if (uiState.conversations.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize().padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(Res.string.feature_attachments_storage_empty))
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = innerPadding
                    ) {
                        items(uiState.conversations, key = { it.conversationId }) { summary ->
                            ListItem(
                                headlineContent = { Text(summary.displayName) },
                                supportingContent = {
                                    Text(
                                        stringResource(
                                            Res.string.feature_attachments_conversation_storage_description,
                                            summary.mediaCount,
                                            summary.fileCount
                                        )
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null
                                    )
                                },
                                trailingContent = { Text(formatBytes(summary.byteSize)) },
                                modifier =
                                    Modifier.padding(horizontal = MaterialTheme.spacing.small)
                                        .clickable {
                                            onUiEvent(
                                                AttachmentStorageUiEvent.ConversationClicked(summary.conversationId)
                                            )
                                        }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(containerColor: Color, onBack: () -> Unit) {
    TopAppBar(
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                scrolledContainerColor = containerColor,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground
            ),
        title = {
            Text(
                text = stringResource(Res.string.feature_attachments_storage),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        }
    )
}

private fun formatBytes(bytes: Long): String =
    when {
        bytes >= 1024L * 1024L * 1024L -> "${bytes / (1024L * 1024L * 1024L)} GB"
        bytes >= 1024L * 1024L -> "${bytes / (1024L * 1024L)} MB"
        bytes >= 1024L -> "${bytes / 1024L} KB"
        else -> "$bytes B"
    }
