package com.cbgm.sparrow.feature.attachments.presentation.management

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.component.SparrowSecondaryButton
import com.cbgm.sparrow.core.ui.component.SparrowStaticScaffold
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.FunctionalColors
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentContent
import com.cbgm.sparrow.feature.attachments.presentation.component.MessageAttachmentViewer
import com.cbgm.sparrow.feature.attachments.presentation.component.rememberAttachmentUiState
import com.cbgm.sparrow.feature.attachments.presentation.management.model.AttachmentManagementTab
import com.cbgm.sparrow.feature.attachments.presentation.management.model.AttachmentManagementUiEvent
import com.cbgm.sparrow.feature.attachments.presentation.management.model.AttachmentManagementUiState
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toMediaItem
import com.cbgm.sparrow.feature.attachments.presentation.model.AttachmentUiState
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageAttachmentUi
import com.cbgm.sparrow.feature.media.presentation.component.MediaThumbnail
import com.cbgm.sparrow.feature.media.util.toReadableByteSize
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_attachments_delete_confirm
import com.cbgm.sparrow.resources.feature_attachments_delete_description
import com.cbgm.sparrow.resources.feature_attachments_delete_title
import com.cbgm.sparrow.resources.feature_attachments_files
import com.cbgm.sparrow.resources.feature_attachments_media
import com.cbgm.sparrow.resources.feature_attachments_media_and_files
import com.cbgm.sparrow.resources.feature_attachments_selected_count
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.lazy.grid.items as gridItems

@Composable
fun AttachmentManagementScreen(
    uiState: AttachmentManagementUiState,
    onUiEvent: (AttachmentManagementUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val mediaItems = remember(uiState.attachments) {
        uiState.attachments.filterIsInstance<MessageAttachmentUi.ImageVideoAttachmentUi>()
    }
    val fileItems = remember(uiState.attachments) {
        uiState.attachments.filterIsInstance<MessageAttachmentUi.FileAttachmentUi>()
    }
    val onAttachmentClick = remember(onUiEvent) {
        { id: String -> onUiEvent(AttachmentManagementUiEvent.AttachmentClicked(id)) }
    }

    val currentSelectedIds = rememberUpdatedState(uiState.selectedIds)
    val currentSelectionMode = rememberUpdatedState(uiState.isSelectionMode)

    SparrowStaticScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AttachmentManagementTopBar(
                isSelectionMode = uiState.isSelectionMode,
                selectedCount = uiState.selectedIds.size,
                isDeleting = uiState.isDeleting,
                hasAttachments = uiState.hasAttachments,
                onUiEvent = onUiEvent
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            AttachmentManagementBody(
                mediaItems = mediaItems,
                fileItems = fileItems,
                selectedTab = uiState.selectedTab,
                selectedIds = currentSelectedIds,
                isSelectionMode = currentSelectionMode,
                bottomPadding = innerPadding.calculateBottomPadding(),
                onUiEvent = onUiEvent,
                onAttachmentClick = onAttachmentClick
            )
        }
    }

    AttachmentDeleteConfirmation(
        visible = uiState.showDeleteConfirmation,
        onUiEvent = onUiEvent
    )
    AttachmentManagementViewer(
        viewerId = uiState.viewerAttachmentId,
        mediaItems = mediaItems,
        onUiEvent = onUiEvent
    )
}

@Composable
private fun AttachmentManagementBody(
    mediaItems: List<MessageAttachmentUi.ImageVideoAttachmentUi>,
    fileItems: List<MessageAttachmentUi.FileAttachmentUi>,
    selectedTab: AttachmentManagementTab,
    selectedIds: State<Set<String>>,
    isSelectionMode: State<Boolean>,
    bottomPadding: Dp,
    onUiEvent: (AttachmentManagementUiEvent) -> Unit,
    onAttachmentClick: (String) -> Unit
) {
    AttachmentTabs(
        selectedTab = selectedTab,
        onTabSelected = { selected -> onUiEvent(AttachmentManagementUiEvent.TabSelected(selected)) }
    )
    when (selectedTab) {
        AttachmentManagementTab.MEDIA -> MediaGrid(
            attachments = mediaItems,
            selectedIds = selectedIds,
            isSelectionMode = isSelectionMode,
            bottomPadding = bottomPadding,
            onClick = onAttachmentClick
        )

        AttachmentManagementTab.FILES -> FileList(
            attachments = fileItems,
            selectedIds = selectedIds,
            isSelectionMode = isSelectionMode,
            bottomPadding = bottomPadding,
            onClick = onAttachmentClick
        )
    }
}

@Composable
private fun AttachmentDeleteConfirmation(
    visible: Boolean,
    onUiEvent: (AttachmentManagementUiEvent) -> Unit
) {
    SparrowAlertDialog(
        isVisible = visible,
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

@Composable
private fun AttachmentManagementViewer(
    viewerId: String?,
    mediaItems: List<MessageAttachmentUi.ImageVideoAttachmentUi>,
    onUiEvent: (AttachmentManagementUiEvent) -> Unit
) {
    viewerId?.let { selectedId ->
        MessageAttachmentViewer(
            attachments = mediaItems,
            selectedAttachmentId = selectedId,
            canSaveToCameraRoll = false,
            onDismiss = { onUiEvent(AttachmentManagementUiEvent.ViewerDismissed) },
            onError = { onUiEvent(AttachmentManagementUiEvent.ViewerError(it)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentManagementTopBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    isDeleting: Boolean,
    hasAttachments: Boolean,
    onUiEvent: (AttachmentManagementUiEvent) -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground
        ),
        title = {
            Text(
                text = if (isSelectionMode) {
                    stringResource(Res.string.feature_attachments_selected_count, selectedCount)
                } else {
                    stringResource(Res.string.feature_attachments_media_and_files)
                },
                style = MaterialTheme.typography.titleSmall
            )
        },
        navigationIcon = {
            IconButton(onClick = { onUiEvent(AttachmentManagementUiEvent.BackClicked) }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        actions = {
            if (isSelectionMode) {
                IconButton(
                    onClick = { onUiEvent(AttachmentManagementUiEvent.DeleteSelectedClicked) },
                    enabled = selectedCount > 0 && !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(MaterialTheme.spacing.medium))
                    } else {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null)
                    }
                }
                IconButton(
                    onClick = { onUiEvent(AttachmentManagementUiEvent.SelectionCleared) },
                    enabled = !isDeleting
                ) {
                    Icon(imageVector = Icons.Default.Deselect, contentDescription = null)
                }
            } else {
                IconButton(
                    onClick = { onUiEvent(AttachmentManagementUiEvent.SelectionStarted) },
                    enabled = hasAttachments
                ) {
                    Icon(imageVector = Icons.Default.SelectAll, contentDescription = null)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.spacing.screenPadding,
                vertical = MaterialTheme.spacing.base
            )
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background)
            .border(
                BorderStroke(
                    Dimens.Base.borderStrokeWidth,
                    MaterialTheme.colorScheme.outlineVariant
                ),
                MaterialTheme.shapes.medium
            ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
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
private fun RowScope.AttachmentTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Tab(
        modifier = Modifier
            .weight(1f)
            .background(if (selected) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.background),
        selected = selected,
        onClick = onClick,
        text = {
            Text(
                text = title,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    )
}

@Composable
private fun MediaGrid(
    attachments: List<MessageAttachmentUi.ImageVideoAttachmentUi>,
    selectedIds: State<Set<String>>,
    isSelectionMode: State<Boolean>,
    bottomPadding: Dp,
    onClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(
            start = MaterialTheme.spacing.screenPadding,
            top = MaterialTheme.spacing.base,
            end = MaterialTheme.spacing.screenPadding,
            bottom = bottomPadding + MaterialTheme.spacing.small
        ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro),
        modifier = Modifier.fillMaxSize()
    ) {
        gridItems(attachments, key = { attachment -> attachment.id }) { attachment ->
            SelectableGridItem(
                onClick = remember(onClick, attachment.id) { { onClick(attachment.id) } },
                attachment = attachment,
                selectionIds = selectedIds,
                selectionMode = isSelectionMode
            )
        }
    }
}

@Composable
private fun SelectableGridItem(
    onClick: () -> Unit,
    attachment: MessageAttachmentUi.ImageVideoAttachmentUi,
    selectionIds: State<Set<String>>,
    selectionMode: State<Boolean>
) {
    val selected by remember(attachment.id, selectionIds, selectionMode) {
        derivedStateOf { selectionMode.value && attachment.id in selectionIds.value }
    }
    GridItem(onClick = onClick, attachment = attachment, selected = selected)
}

@Composable
private fun GridItem(
    onClick: () -> Unit,
    attachment: MessageAttachmentUi.ImageVideoAttachmentUi,
    selected: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AttachmentGridThumbnail(attachment)
            AttachmentSelectionIndicator(
                selected = selected,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
private fun AttachmentGridThumbnail(
    attachment: MessageAttachmentUi.ImageVideoAttachmentUi
) {
    val attachmentState = rememberAttachmentUiState(attachment.target)
    val localFilePath =
        (attachmentState as? AttachmentUiState.Ready)
            ?.content
            ?.let { content -> content as? AttachmentContent.LocalFile }
            ?.localFilePath

    Box(modifier = Modifier.fillMaxWidth()) {
        if (localFilePath != null) {
            MediaThumbnail(
                media = attachment.toMediaItem(localFilePath),
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        if (attachment.type == MessageAttachmentType.VIDEO) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.padding(MaterialTheme.spacing.base),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AttachmentSelectionIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Box(
            modifier = modifier
                .background(FunctionalColors.MediaBackground.copy(alpha = Alpha.Disabled))
                .padding(MaterialTheme.spacing.micro)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopEnd).padding(MaterialTheme.spacing.micro)
            )
        }
    }
}

@Composable
private fun FileList(
    attachments: List<MessageAttachmentUi.FileAttachmentUi>,
    selectedIds: State<Set<String>>,
    isSelectionMode: State<Boolean>,
    bottomPadding: Dp,
    onClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MaterialTheme.spacing.screenPadding,
            top = MaterialTheme.spacing.base,
            end = MaterialTheme.spacing.screenPadding,
            bottom = bottomPadding + MaterialTheme.spacing.small
        )
    ) {
        if (attachments.isNotEmpty()) {
            item(key = "attachment-files-group") {
                SparrowCardNoAnimation {
                    Column {
                        attachments.forEachIndexed { index, attachment ->
                            androidx.compose.runtime.key(attachment.id) {
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(
                                            start = MaterialTheme.spacing.times(
                                                5
                                            )
                                        ),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Alpha.divider)
                                    )
                                }
                                SelectableAttachmentFileRow(
                                    attachment = attachment,
                                    selectionIds = selectedIds,
                                    selectionMode = isSelectionMode,
                                    onClick = remember(onClick, attachment.id) {
                                        {
                                            onClick(
                                                attachment.id
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableAttachmentFileRow(
    attachment: MessageAttachmentUi.FileAttachmentUi,
    selectionIds: State<Set<String>>,
    selectionMode: State<Boolean>,
    onClick: () -> Unit
) {
    val selected by remember(attachment.id, selectionIds, selectionMode) {
        derivedStateOf { selectionMode.value && attachment.id in selectionIds.value }
    }
    AttachmentFileRow(attachment = attachment, selected = selected, onClick = onClick)
}

@Composable
private fun AttachmentFileRow(
    attachment: MessageAttachmentUi.FileAttachmentUi,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(
            horizontal = MaterialTheme.spacing.small,
            vertical = MaterialTheme.spacing.small
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(Dimens.SettingsScreen.primaryIconSize),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.size(MaterialTheme.spacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attachment.fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = attachment.byteSize.toReadableByteSize(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AttachmentFileSelectionIndicator(selected)
    }
}

@Composable
private fun AttachmentFileSelectionIndicator(selected: Boolean) {
    if (selected) Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
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
            uiState = previewAttachmentManagementUiState().copy(
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
        attachments =
            listOf(
                MessageAttachmentUi.ImageVideoAttachmentUi(
                    id = "preview-image",
                    type = MessageAttachmentType.IMAGE,
                    mimeType = "image/jpeg",
                    byteSize = 0
                ),
                MessageAttachmentUi.ImageVideoAttachmentUi(
                    id = "preview-video",
                    type = MessageAttachmentType.VIDEO,
                    mimeType = "video/mp4",
                    byteSize = 0,
                    durationMilliseconds = 42_000
                ),
                MessageAttachmentUi.FileAttachmentUi(
                    id = "preview-file",
                    mimeType = "application/pdf",
                    byteSize = 240_000,
                    fileName = "document.pdf"
                )
            )
    )
