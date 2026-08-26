package com.cbgm.sparrow.feature.attachments.presentation.management

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowSecondaryButton
import com.cbgm.sparrow.core.ui.component.SparrowStaticScaffold
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.FunctionalColors
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.attachments.presentation.component.MessageAttachmentViewer
import com.cbgm.sparrow.feature.attachments.presentation.management.model.AttachmentFileUi
import com.cbgm.sparrow.feature.attachments.presentation.management.model.AttachmentManagementTab
import com.cbgm.sparrow.feature.attachments.presentation.management.model.AttachmentManagementUiEvent
import com.cbgm.sparrow.feature.attachments.presentation.management.model.AttachmentManagementUiState
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toMediaItem
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageMediaAttachmentUi
import com.cbgm.sparrow.feature.media.domain.model.MediaContentType
import com.cbgm.sparrow.feature.media.presentation.component.MediaThumbnail
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_attachments_delete_confirm
import com.cbgm.sparrow.resources.feature_attachments_delete_description
import com.cbgm.sparrow.resources.feature_attachments_delete_selected
import com.cbgm.sparrow.resources.feature_attachments_delete_title
import com.cbgm.sparrow.resources.feature_attachments_files
import com.cbgm.sparrow.resources.feature_attachments_media
import com.cbgm.sparrow.resources.feature_attachments_media_and_files
import com.cbgm.sparrow.resources.feature_attachments_select
import com.cbgm.sparrow.resources.feature_attachments_selected_count
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.lazy.grid.items as gridItems

@Composable
fun AttachmentManagementScreen(
    uiState: AttachmentManagementUiState,
    onUiEvent: (AttachmentManagementUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    SparrowStaticScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AttachmentManagementTopBar(
                uiState = uiState,
                onUiEvent = onUiEvent
            )
        }
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
        ) {
            AttachmentTabs(
                selectedTab = uiState.selectedTab,
                onTabSelected = { tab ->
                    onUiEvent(AttachmentManagementUiEvent.TabSelected(tab))
                }
            )

            when (uiState.selectedTab) {
                AttachmentManagementTab.MEDIA ->
                    MediaGrid(
                        attachments = uiState.media,
                        selectedIds = uiState.selectedIds,
                        isSelectionMode = uiState.isSelectionMode,
                        bottomPadding = innerPadding.calculateBottomPadding(),
                        onVisible = { onUiEvent(AttachmentManagementUiEvent.AttachmentVisible(it)) },
                        onClick = { onUiEvent(AttachmentManagementUiEvent.AttachmentClicked(it)) }
                    )

                AttachmentManagementTab.FILES ->
                    FileList(
                        attachments = uiState.files,
                        selectedIds = uiState.selectedIds,
                        isSelectionMode = uiState.isSelectionMode,
                        bottomPadding = innerPadding.calculateBottomPadding(),
                        onClick = { onUiEvent(AttachmentManagementUiEvent.AttachmentClicked(it)) }
                    )
            }
        }
    }

    if (uiState.showDeleteConfirmation) {
        SparrowAlertDialog(
            onDismissRequest = { onUiEvent(AttachmentManagementUiEvent.DeleteDismissed) },
            title = stringResource(Res.string.feature_attachments_delete_title),
            text = { Text(stringResource(Res.string.feature_attachments_delete_description)) },
            confirmButton = {
                SparrowApprovalButton(
                    fillMaxWidth = false,
                    onClick = { onUiEvent(AttachmentManagementUiEvent.DeleteConfirmed) },
                    text = stringResource(Res.string.feature_attachments_delete_confirm)
                )
            },
            dismissButton = {
                SparrowSecondaryButton(
                    fillMaxWidth = false,
                    onClick = { onUiEvent(AttachmentManagementUiEvent.DeleteDismissed) },
                    text = stringResource(Res.string.base_cancel)
                )
            }
        )
    }

    uiState.viewerAttachmentId?.let { selectedId ->
        MessageAttachmentViewer(
            attachments = uiState.media,
            selectedAttachmentId = selectedId,
            canSaveToCameraRoll = false,
            onDismiss = { onUiEvent(AttachmentManagementUiEvent.ViewerDismissed) },
            onEnsureAttachmentLoaded = { onUiEvent(AttachmentManagementUiEvent.AttachmentVisible(it)) },
            onError = { onUiEvent(AttachmentManagementUiEvent.ViewerError(it)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentManagementTopBar(
    uiState: AttachmentManagementUiState,
    onUiEvent: (AttachmentManagementUiEvent) -> Unit
) {
    TopAppBar(
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground
            ),
        title = {
            Text(
                text =
                    if (uiState.isSelectionMode) {
                        stringResource(
                            Res.string.feature_attachments_selected_count,
                            uiState.selectedIds.size
                        )
                    } else {
                        stringResource(Res.string.feature_attachments_media_and_files)
                    },
                style = MaterialTheme.typography.titleSmall
            )
        },
        navigationIcon = {
            IconButton(onClick = { onUiEvent(AttachmentManagementUiEvent.BackClicked) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }
        },
        actions = {
            if (uiState.isSelectionMode) {
                IconButton(
                    onClick = { onUiEvent(AttachmentManagementUiEvent.DeleteSelectedClicked) },
                    enabled = uiState.selectedIds.isNotEmpty() && !uiState.isDeleting
                ) {
                    if (uiState.isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(MaterialTheme.spacing.medium)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = stringResource(Res.string.feature_attachments_delete_selected)
                        )
                    }
                }

                IconButton(
                    onClick = { onUiEvent(AttachmentManagementUiEvent.SelectionCleared) },
                    enabled = !uiState.isDeleting
                ) {
                    Icon(
                        imageVector = Icons.Default.Deselect,
                        contentDescription = stringResource(Res.string.base_cancel)
                    )
                }
            } else {
                IconButton(
                    onClick = { onUiEvent(AttachmentManagementUiEvent.SelectionStarted) },
                    enabled = uiState.hasAttachments
                ) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = stringResource(Res.string.feature_attachments_select)
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentTabs(
    selectedTab: AttachmentManagementTab,
    onTabSelected: (AttachmentManagementTab) -> Unit
) {
    PrimaryTabRow(
        selectedTabIndex = selectedTab.ordinal,
        indicator = {
            Box(
                modifier =
                    Modifier
                        .tabIndicatorOffset(selectedTabIndex = selectedTab.ordinal)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    ) {
        AttachmentTab(
            title = stringResource(Res.string.feature_attachments_media),
            selected = selectedTab == AttachmentManagementTab.MEDIA,
            onClick = { onTabSelected(AttachmentManagementTab.MEDIA) }
        )
        AttachmentTab(
            title = stringResource(Res.string.feature_attachments_files),
            selected = selectedTab == AttachmentManagementTab.FILES,
            onClick = { onTabSelected(AttachmentManagementTab.FILES) }
        )
    }
}

@Composable
private fun AttachmentTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Tab(
        selected = selected,
        onClick = onClick,
        text = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    )
}

@Composable
private fun MediaGrid(
    attachments: List<MessageMediaAttachmentUi>,
    selectedIds: Set<String>,
    isSelectionMode: Boolean,
    bottomPadding: Dp,
    onVisible: (String) -> Unit,
    onClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding =
            PaddingValues(
                start = MaterialTheme.spacing.small,
                top = MaterialTheme.spacing.small,
                end = MaterialTheme.spacing.small,
                bottom = bottomPadding + MaterialTheme.spacing.small
            ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro),
        modifier = Modifier.fillMaxSize()
    ) {
        gridItems(attachments, key = { attachment -> attachment.id }) { attachment ->

            GridItem(
                onClick = { onClick(attachment.id) },
                attachment = attachment,
                onVisible = onVisible,
                isSelected = isSelectionMode && attachment.id in selectedIds
            )
        }
    }
}

@Composable
private fun GridItem(
    onClick: () -> Unit,
    isSelected: Boolean,
    attachment: MessageMediaAttachmentUi,
    onVisible: (String) -> Unit
) {
    LaunchedEffect(attachment.id, attachment.bytes) {
        if (attachment.bytes == null) onVisible(attachment.id)
    }

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() },
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            MediaThumbnail(
                media = attachment.toMediaItem(),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )

            if (attachment.type == MediaContentType.VIDEO) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(FunctionalColors.MediaBackground.copy(alpha = Alpha.Disabled))
                        .padding(MaterialTheme.spacing.micro)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(MaterialTheme.spacing.micro)
                    )
                }
            }
        }
    }
}

@Composable
private fun FileList(
    attachments: List<AttachmentFileUi>,
    selectedIds: Set<String>,
    isSelectionMode: Boolean,
    bottomPadding: Dp,
    onClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding)
    ) {
        items(attachments, key = { attachment -> attachment.id }) { attachment ->
            ListItem(
                headlineContent = {
                    Text(
                        text = attachment.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                supportingContent = {
                    Text(
                        text = attachment.sizeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null
                    )
                },
                trailingContent = {
                    if (isSelectionMode && attachment.id in selectedIds) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null
                        )
                    }
                },
                colors =
                    ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onClick(attachment.id) }
            )
        }
    }
}

@Preview
@Composable
private fun AttachmentManagementScreenPreview() {
    SparrowTheme {
        AttachmentManagementScreen(
            uiState = previewAttachmentManagementUiState(),
            onUiEvent = {}
        )
    }
}

@Preview
@Composable
private fun AttachmentManagementSelectionPreview() {
    SparrowTheme {
        AttachmentManagementScreen(
            uiState =
                previewAttachmentManagementUiState().copy(
                    isSelectionMode = true,
                    selectedIds = setOf("preview-image")
                ),
            onUiEvent = {}
        )
    }
}

@Preview
@Composable
private fun AttachmentTabsPreview() {
    SparrowTheme {
        AttachmentTabs(
            selectedTab = AttachmentManagementTab.MEDIA,
            onTabSelected = {}
        )
    }
}

private fun previewAttachmentManagementUiState(): AttachmentManagementUiState =
    AttachmentManagementUiState(
        media =
            listOf(
                MessageMediaAttachmentUi(
                    id = "preview-image",
                    type = MediaContentType.IMAGE,
                    mimeType = "image/jpeg"
                ),
                MessageMediaAttachmentUi(
                    id = "preview-video",
                    type = MediaContentType.VIDEO,
                    mimeType = "video/mp4",
                    durationMilliseconds = 42_000
                )
            ),
        files =
            listOf(
                AttachmentFileUi(
                    id = "preview-file",
                    displayName = "document.pdf",
                    sizeText = "240 KB"
                )
            )
    )
