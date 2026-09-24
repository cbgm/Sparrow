package com.cbgm.sparrow.feature.attachments.presentation.storage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
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
                    Modifier.fillMaxSize().padding(innerPadding)
                        .padding(MaterialTheme.spacing.screenPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(
                            Dimens.Base.borderStrokeWidth,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = uiState.message,
                            modifier = Modifier.padding(MaterialTheme.spacing.medium),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

            is AttachmentStorageUiState.Content -> {
                if (uiState.conversations.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize().padding(innerPadding)
                            .padding(MaterialTheme.spacing.screenPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Column(
                                modifier = Modifier.padding(MaterialTheme.spacing.medium),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(Res.string.feature_attachments_storage_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            start = MaterialTheme.spacing.screenPadding,
                            end = MaterialTheme.spacing.screenPadding,
                            top = innerPadding.calculateTopPadding() + MaterialTheme.spacing.medium,
                            bottom = innerPadding.calculateBottomPadding() + MaterialTheme.spacing.medium
                        )
                    ) {
                        item(key = "conversation-storage-group") {
                            SparrowCardNoAnimation {
                                Column {
                                    uiState.conversations.forEachIndexed { index, summary ->
                                        androidx.compose.runtime.key(summary.conversationId) {
                                            if (index > 0) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(
                                                        start = MaterialTheme.spacing.times(
                                                            5
                                                        )
                                                    ),
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                        alpha = Alpha.divider
                                                    )
                                                )
                                            }
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                color = Color.Transparent,
                                                onClick = {
                                                    onUiEvent(
                                                        AttachmentStorageUiEvent.ConversationClicked(
                                                            summary.conversationId
                                                        )
                                                    )
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(
                                                        horizontal = MaterialTheme.spacing.small,
                                                        vertical = MaterialTheme.spacing.small
                                                    ),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.Folder,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(Dimens.SettingsScreen.primaryIconSize),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(Modifier.size(MaterialTheme.spacing.small))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            summary.displayName,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            stringResource(
                                                                Res.string.feature_attachments_conversation_storage_description,
                                                                summary.mediaCount,
                                                                summary.fileCount
                                                            ),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Text(
                                                        formatBytes(summary.byteSize),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
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
